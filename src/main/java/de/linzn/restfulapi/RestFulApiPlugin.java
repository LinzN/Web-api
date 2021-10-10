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

package de.linzn.restfulapi;


import de.linzn.restfulapi.api.APIWebserver;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.ArrayList;
import java.util.List;


public class RestFulApiPlugin extends STEMPlugin {

    public static RestFulApiPlugin restFulApiPlugin;
    private APIWebserver apiWebserver;


    public RestFulApiPlugin() {
        restFulApiPlugin = this;
    }

    @Override
    public void onEnable() {
        this.setupConfig();
        this.apiWebserver = new APIWebserver(this.getDefaultConfig().getString("apiServer.hostname"), this.getDefaultConfig().getInt("apiServer.port"));
        this.apiWebserver.start();
    }

    @Override
    public void onDisable() {
        this.apiWebserver.stop();
    }

    private void setupConfig() {
        List<String> list1 = new ArrayList<>();
        list1.add("127.0.0.1");

        if (!this.getDefaultConfig().contains("apiServer.whitelist")) {
            this.getDefaultConfig().set("apiServer.whitelist", list1);
        }

        this.getDefaultConfig().save();
    }

    public void registerIGetJSONClass(IRequest iRequest) {
        this.apiWebserver.getApiHandler().addGetHandler(iRequest);
    }

    public void registerIPostJSONClass(IRequest iRequest) {
        this.apiWebserver.getApiHandler().addPostHandler(iRequest);
    }

}
