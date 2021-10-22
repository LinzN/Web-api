/*
 * Copyright (C) 2021. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.webapi;


import de.linzn.webapi.core.WebServer;
import de.linzn.webapi.defaultWebModules.DefaultHandlerSetup;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;


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
