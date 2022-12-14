/*
 *  Copyright (C) 2021. Niklas Linz - All Rights Reserved
 *  You may use, distribute and modify this code under the
 *  terms of the LGPLv3 license, which unfortunately won't be
 *  written for another century.
 *
 *  You should have received a copy of the LGPLv3 license with
 *  this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.webapi.core;


import com.sun.net.httpserver.HttpServer;
import de.linzn.webapi.modules.WebModule;
import de.stem.stemSystem.STEMSystemApp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {

    private final String hostname;
    private final int port;
    private HttpServer apiServer;

    public WebServer(String host, int port) {
        this.hostname = host;
        this.port = port;
        try {
            apiServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            apiServer.setExecutor(Executors.newCachedThreadPool());
        } catch (IOException e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
    }

    public void enableCallModule(WebModule webModule) {
        this.apiServer.createContext("/" + webModule.getModuleName().toLowerCase(), webModule);
    }

    public void disableCallModule(WebModule webModule) {
        this.apiServer.removeContext("/" + webModule.getModuleName().toLowerCase());
    }

    public void start() {
        this.apiServer.start();
    }

    public void stop() {
        this.apiServer.stop(0);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
