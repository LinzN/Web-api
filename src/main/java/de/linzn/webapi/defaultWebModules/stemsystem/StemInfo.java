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

package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.utils.JavaUtils;

import java.io.IOException;
import java.net.InetAddress;

public class StemInfo extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        String version = JavaUtils.getVersion().toUpperCase();
        String systemName = InetAddress.getLocalHost().getHostName().toUpperCase();

        apiResponse.getJSONObject().put("version", version);
        apiResponse.getJSONObject().put("systemName", systemName);
        apiResponse.getJSONObject().put("label", version);
        return apiResponse.buildResponse();
    }
}
