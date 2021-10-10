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

package de.linzn.restfulapi.api;


import com.sun.net.httpserver.HttpServer;
import de.linzn.restfulapi.api.handler.ApiHandler;
import de.stem.stemSystem.STEMSystemApp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class APIWebserver {

    private HttpServer apiServer;
    private String hostname;
    private int port;
    private ApiHandler apiHandler;

    public APIWebserver(String host, int port) {
        this.hostname = host;
        this.port = port;
        this.apiHandler = new ApiHandler();
        try {
            apiServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            apiServer.createContext("/", apiHandler);
            apiServer.setExecutor(Executors.newSingleThreadExecutor());
        } catch (IOException e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
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

    public ApiHandler getApiHandler() {
        return this.apiHandler;
    }
}
