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


import de.linzn.stem.modules.pluginModule.STEMPlugin;
import de.linzn.webapi.core.WebServer;
import de.linzn.webapi.defaultWebModules.DefaultHandlerSetup;


public class WebApiPlugin extends STEMPlugin {

    public static WebApiPlugin webApiPlugin;
    private WebServer webServer;


    public WebApiPlugin() {
        webApiPlugin = this;
    }

    @Override
    public void onEnable() {
        this.setupConfig();
        this.webServer = new WebServer(this.getDefaultConfig().getString("server.hostname"), this.getDefaultConfig().getInt("server.port"));
        this.webServer.start();
        new DefaultHandlerSetup();
    }

    @Override
    public void onDisable() {
        this.webServer.stop();
    }

    public WebServer getWebServer() {
        return this.webServer;
    }

    private void setupConfig() {
        this.getDefaultConfig().getString("server.hostname", "0.0.0.0");
        this.getDefaultConfig().getInt("server.port", 8082);
        this.getDefaultConfig().save();
    }


}
