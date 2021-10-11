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

package de.linzn.webapi.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.stem.stemSystem.STEMSystemApp;

public class WebCallHandler implements HttpHandler {


    public WebCallHandler() {

    }


    @Override
    public void handle(HttpExchange he) {
        try {

        } catch (Exception e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
    }
}

