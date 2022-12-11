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

package de.linzn.webapi.defaultWebModules.emergency;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.taskManagment.operations.defaultOperations.StemRestartOperation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

public class Reboot extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();
        STEMSystemApp.LOGGER.CORE("--EMERGENCY WEBAPI--");
        STEMSystemApp.LOGGER.CORE("Request emergency reboot from WEBAPI:");
        STEMSystemApp.LOGGER.CORE("Backend IP: " + exchange.getRemoteAddress().getAddress().toString());
        STEMSystemApp.LOGGER.CORE("PostData: " + postData.toString());

        String requestAction = postData.get("requestAction").toString();

        if (requestAction.equalsIgnoreCase("WRITE")) {
            StemRestartOperation stemRestartOperation = new StemRestartOperation();
            STEMSystemApp.getInstance().getScheduler().runTask(WebApiPlugin.webApiPlugin, stemRestartOperation);
            jsonObject.put("error", 0);
        } else {
            jsonObject.put("error", 404);
        }

        return jsonObject;
    }
}
