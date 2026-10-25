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

import com.sun.net.httpserver.HttpServer;
import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.modules.WebModule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server. Each request is handled on its own virtual thread; the executor is shut down
 * when the server stops, so no threads are left behind.
 */
public class WebServer {
    private final String hostname;
    private final int port;
    private HttpServer apiServer;
    private ExecutorService executor;

    public WebServer(String host, int port) {
        this.hostname = host;
        this.port = port;
        try {
            this.apiServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("web-api-", 0).factory());
            this.apiServer.setExecutor(this.executor);
        } catch (IOException e) {
            WebApiPlugin.webApiPlugin.getPluginLogger().error("Could not create web server on " + host + ":" + port, e);
            this.apiServer = null;
        }
    }

    public void enableCallModule(WebModule webModule) {
        if (this.apiServer == null) {
            return;
        }
        this.apiServer.createContext("/" + webModule.getModuleName().toLowerCase(), webModule);
    }

    public void disableCallModule(WebModule webModule) {
        if (this.apiServer == null) {
            return;
        }
        try {
            this.apiServer.removeContext("/" + webModule.getModuleName().toLowerCase());
        } catch (IllegalArgumentException ignored) {
            /* context was not registered */
        }
    }

    public void start() {
        if (this.apiServer == null) {
            WebApiPlugin.webApiPlugin.getPluginLogger().error("Web server not available, not starting");
            return;
        }
        this.apiServer.start();
        WebApiPlugin.webApiPlugin.getPluginLogger().info("Web server listening on " + hostname + ":" + port);
    }

    public void stop() {
        if (this.apiServer != null) {
            /* give running requests up to one second to finish */
            this.apiServer.stop(1);
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

    public boolean isRunning() {
        return this.apiServer != null && this.executor != null && !this.executor.isShutdown();
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
