/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
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
    }

    @Override
    public void onDisable() {
        this.webServer.stop();
    }

    private void setupConfig() {
        this.getDefaultConfig().getString("server.hostname","localhost");
        this.getDefaultConfig().getInt("server.port",8081);
        this.getDefaultConfig().save();
    }


}