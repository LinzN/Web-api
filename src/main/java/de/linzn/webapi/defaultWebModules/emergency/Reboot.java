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
import de.linzn.stem.STEMApp;
import de.linzn.stem.taskManagment.operations.defaultOperations.StemRestartOperation;
import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONObject;

import java.io.IOException;

public class Reboot extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();

        String ipAddress = exchange.getRequestHeaders().getFirst("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        }

        STEMApp.LOGGER.CORE("--EMERGENCY WEBAPI--");
        STEMApp.LOGGER.CORE("Request emergency reboot from WEBAPI:");
        STEMApp.LOGGER.CORE("Requester IP: " + ipAddress);
        STEMApp.LOGGER.CORE("PostData: " + postData.toString());

        String requestAction = postData.get("requestAction").toString();

        if (requestAction.equalsIgnoreCase("WRITE")) {
            StemRestartOperation stemRestartOperation = new StemRestartOperation();
            STEMApp.getInstance().getScheduler().runTask(WebApiPlugin.webApiPlugin, stemRestartOperation);
            jsonObject.put("error", 0);
        } else {
            jsonObject.put("error", 404);
        }

        return jsonObject;
    }
}
