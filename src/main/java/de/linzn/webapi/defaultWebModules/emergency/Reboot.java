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

        String ipAddress = exchange.getRequestHeaders().getFirst("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        }

        STEMSystemApp.LOGGER.CORE("--EMERGENCY WEBAPI--");
        STEMSystemApp.LOGGER.CORE("Request emergency reboot from WEBAPI:");
        STEMSystemApp.LOGGER.CORE("Requester IP: " + ipAddress);
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
