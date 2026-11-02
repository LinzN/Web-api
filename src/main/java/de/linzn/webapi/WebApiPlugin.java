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


package de.linzn.webapi;

import de.linzn.simplyConfiguration.FileConfiguration;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import de.linzn.webapi.auth.AuthService;
import de.linzn.webapi.core.WebServer;
import de.linzn.webapi.defaultWebModules.DefaultHandlerSetup;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WebApiPlugin extends STEMPlugin {

    public static WebApiPlugin webApiPlugin;
    private WebServer webServer;
    private AuthService auth;

    public WebApiPlugin() {
        webApiPlugin = this;
    }

    @Override
    public void onEnable() {
        this.setupConfig();
        FileConfiguration config = this.getDefaultConfig();
        AuthService.Settings settings = new AuthService.Settings(
                bool(config, "auth.enabled", true),
                config.getString("auth.cookieName", "stem_session"),
                String.valueOf(config.get("auth.secureCookie", "auto")),
                Duration.ofHours(Math.max(1, config.getInt("auth.sessionHours", 12))),
                Duration.ofDays(Math.max(1, config.getInt("auth.rememberDays", 30))),
                config.getString("auth.loginPath", "/login"),
                list(config, "auth.publicPaths"),
                list(config, "server.trustedProxies"));
        Path folder = this.getDataFolder().toPath();
        try {
            this.auth = new AuthService(settings, folder, this.getPluginLogger());
        } catch (IOException e) {
            this.getPluginLogger().error("Could not load the web login (users.json / sessions.json), web server not started", e);
            return;
        }
        this.bootstrapAdmin();
        this.webServer = new WebServer(config.getString("server.hostname", "0.0.0.0"), config.getInt("server.port", 8082),
                config.getString("server.basePath", "/apiv2"), this.auth, this.getPluginLogger());
        this.webServer.start();
        new DefaultHandlerSetup();
        this.getStemApi().commands().register("webapi", new WebApiCommand(this));
    }

    /**
     * First start with login: create the user "admin" with a setup code in a file (never in the log).
     */
    private void bootstrapAdmin() {
        if (!this.auth.isEnabled() || !this.auth.users().isEmpty()) {
            return;
        }
        try {
            Path file = this.auth.users().create("admin");
            this.getPluginLogger().warning("Web login: no users yet, created user 'admin'. Its setup code is in " + file.toAbsolutePath()
                    + " - open the login page, choose \"Code einlösen\" and set a password.");
        } catch (IOException | IllegalArgumentException e) {
            this.getPluginLogger().error("Could not create the first web user: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }

    public WebServer getWebServer() {
        return this.webServer;
    }

    /**
     * The login shared by the API and the pages of other plugins.
     */
    public AuthService getAuth() {
        return this.auth;
    }

    private void setupConfig() {
        FileConfiguration config = this.getDefaultConfig();
        config.getString("server.hostname", "0.0.0.0");
        config.getInt("server.port", 8082);
        /* API modules are also reachable below this path, e.g. /apiv2/stemsystem/status */
        config.getString("server.basePath", "/apiv2");
        /* reverse proxies whose X-Forwarded-For/-Proto/-Host headers are believed */
        config.get("server.trustedProxies", new ArrayList<>(List.of("127.0.0.1", "::1")));
        config.get("auth.enabled", true);
        config.getString("auth.cookieName", "stem_session");
        /* auto: Secure cookie when the request came in over HTTPS (also through a trusted proxy) */
        config.get("auth.secureCookie", "auto");
        config.getInt("auth.sessionHours", 12);
        config.getInt("auth.rememberDays", 30);
        config.getString("auth.loginPath", "/login");
        /* paths (prefixes) that need no login */
        config.get("auth.publicPaths", new ArrayList<>());
        /* Optional shared secret for /emergency/reboot (field "token" in the request); empty = LAN only */
        config.getString("emergency.token", "");
        config.save();
    }

    private static boolean bool(FileConfiguration config, String path, boolean def) {
        Object value = config.get(path, def);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private static List<String> list(FileConfiguration config, String path) {
        Object value = config.get(path, new ArrayList<>());
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            items.forEach(item -> result.add(String.valueOf(item).trim()));
        } else if (value != null && !String.valueOf(value).isBlank()) {
            for (String item : String.valueOf(value).split(",")) {
                result.add(item.trim());
            }
        }
        return result;
    }
}
