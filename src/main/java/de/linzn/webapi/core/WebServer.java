/*
 * Copyright (c) 2025 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */


package de.linzn.webapi.core;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.linzn.stem.api.StemLogger;
import de.linzn.webapi.auth.AuthFilter;
import de.linzn.webapi.auth.AuthHandlers;
import de.linzn.webapi.auth.AuthService;
import de.linzn.webapi.auth.LoginPage;
import de.linzn.webapi.modules.WebModule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server. Each request is handled on its own virtual thread; the executor is shut down
 * when the server stops, so no threads are left behind.
 * <p>
 * Every context goes through the login ({@link AuthService}): API modules answer 401 without login, pages of
 * other plugins ({@link #registerPage}) redirect to the login page. API modules are reachable at
 * {@code /<module>} and, if a base path is set, also at {@code <basePath>/<module>} (e.g. {@code /apiv2/<module>}),
 * so a page served by this server can use the same paths as before behind a reverse proxy.
 */
public class WebServer {
    private final String hostname;
    private final int port;
    private final String basePath;
    private final AuthService auth;
    private final StemLogger logger;
    private HttpServer apiServer;
    private ExecutorService executor;

    /**
     * @param basePath additional prefix for API modules, e.g. "/apiv2", empty for none
     */
    public WebServer(String host, int port, String basePath, AuthService auth, StemLogger logger) {
        this.hostname = host;
        this.port = port;
        this.basePath = normalizeBase(basePath);
        this.auth = auth;
        this.logger = logger;
        try {
            this.apiServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("web-api-", 0).factory());
            this.apiServer.setExecutor(this.executor);
            this.registerLogin();
        } catch (IOException e) {
            logger.error("Could not create web server on " + host + ":" + port, e);
            this.apiServer = null;
        }
    }

    private void registerLogin() throws IOException {
        AuthHandlers handlers = new AuthHandlers(this.auth);
        for (String path : this.paths("auth")) {
            /* the handlers check credentials themselves */
            this.apiServer.createContext(path, handlers);
        }
        this.apiServer.createContext(this.auth.settings().loginPath(), new LoginPage("MirraNET"));
    }

    private static String normalizeBase(String basePath) {
        if (basePath == null || basePath.isBlank() || basePath.equals("/")) {
            return "";
        }
        String path = basePath.trim();
        path = path.startsWith("/") ? path : "/" + path;
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private List<String> paths(String module) {
        List<String> paths = new ArrayList<>();
        paths.add("/" + module);
        if (!this.basePath.isEmpty()) {
            paths.add(this.basePath + "/" + module);
        }
        return paths;
    }

    public void enableCallModule(WebModule webModule) {
        if (this.apiServer == null) {
            return;
        }
        for (String path : this.paths(webModule.getModuleName().toLowerCase())) {
            HttpContext context = this.apiServer.createContext(path, webModule);
            context.getFilters().add(new AuthFilter(this.auth, AuthFilter.Mode.API));
        }
    }

    public void disableCallModule(WebModule webModule) {
        if (this.apiServer == null) {
            return;
        }
        for (String path : this.paths(webModule.getModuleName().toLowerCase())) {
            try {
                this.apiServer.removeContext(path);
            } catch (IllegalArgumentException ignored) {
                /* context was not registered */
            }
        }
    }

    /**
     * Serve pages (HTML, files) of another plugin behind the login: browsers without login are sent to the login
     * page. A page at "/" gets every request no other context takes.
     *
     * @param path context path, e.g. "/" or "/dashboard"
     */
    public void registerPage(String path, HttpHandler handler) {
        if (this.apiServer == null) {
            return;
        }
        HttpContext context = this.apiServer.createContext(path, handler);
        context.getFilters().add(new AuthFilter(this.auth, AuthFilter.Mode.PAGE));
    }

    public void unregisterPage(String path) {
        if (this.apiServer == null) {
            return;
        }
        try {
            this.apiServer.removeContext(path);
        } catch (IllegalArgumentException ignored) {
            /* context was not registered */
        }
    }

    public void start() {
        if (this.apiServer == null) {
            this.logger.error("Web server not available, not starting");
            return;
        }
        this.apiServer.start();
        this.logger.info("Web server listening on " + hostname + ":" + port + (this.basePath.isEmpty() ? "" : ", API also below " + this.basePath)
                + (this.auth.isEnabled() ? ", login required" : ", WITHOUT login (auth.enabled: false)"));
    }

    public void stop() {
        if (this.apiServer != null) {
            /* give running requests up to one second to finish */
            this.apiServer.stop(1);
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
        this.auth.sessions().save();
    }

    public boolean isRunning() {
        return this.apiServer != null && this.executor != null && !this.executor.isShutdown();
    }

    public AuthService getAuth() {
        return this.auth;
    }

    /**
     * @return the additional prefix of API modules ("/apiv2"), empty if none
     */
    public String getBasePath() {
        return this.basePath;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
