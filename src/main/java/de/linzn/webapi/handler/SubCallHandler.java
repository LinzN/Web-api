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

package de.linzn.webapi.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public abstract class SubCallHandler {

    public abstract Object callHttpEvent(HttpExchange exchange) throws IOException;
}
