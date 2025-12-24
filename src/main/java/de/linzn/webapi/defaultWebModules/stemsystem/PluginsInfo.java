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

package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class PluginsInfo extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        ArrayList<STEMPlugin> plugins = new ArrayList<>(STEMSystemApp.getInstance().getPluginModule().getLoadedPlugins());

        JSONArray pluginsList = new JSONArray();

        for (STEMPlugin plugin : plugins) {
            JSONObject pluginJSON = new JSONObject();
            pluginJSON.put("name", plugin.getPluginName());
            pluginJSON.put("description", plugin.getDescription());
            pluginJSON.put("version", plugin.getVersion());
            pluginJSON.put("buildNumber", plugin.getBuildNumber());
            pluginsList.put(pluginJSON);
        }

        apiResponse.getJSONObject().put("list", pluginsList);
        apiResponse.getJSONObject().put("length", pluginsList.length());
        return apiResponse.buildResponse();
    }
}
