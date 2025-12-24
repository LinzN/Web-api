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
import de.stem.stemSystem.modules.healthModule.HealthCheck;
import de.stem.stemSystem.modules.healthModule.HealthCheckFeedback;
import de.stem.stemSystem.modules.healthModule.HealthCheckLevel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class HealthCheckInfo extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        List<HealthCheck> healthChecks = STEMSystemApp.getInstance().getHealthModule().getHealthChecks();

        JSONArray jsonArray = new JSONArray();

        for (HealthCheck healthCheck : healthChecks) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("healthCheckName", healthCheck.getName());
            jsonObject.put("pluginSource", healthCheck.getStemPlugin().getPluginName());

            JSONObject feedbacks = new JSONObject();
            for (HealthCheckLevel healthCheckLevel : HealthCheckLevel.values()) {
                JSONArray levelFeedbacks = new JSONArray();
                for (HealthCheckFeedback healthCheckFeedback : healthCheck.getHealthCheckFeedbacks(healthCheckLevel)) {
                    JSONObject feedback = new JSONObject();
                    feedback.put("description", healthCheckFeedback.getDescription());
                    levelFeedbacks.put(feedback);
                }
                feedbacks.put(healthCheckLevel.name(), levelFeedbacks);
            }
            jsonObject.put("feedbacks", feedbacks);
            jsonArray.put(jsonObject);
        }

        apiResponse.getJSONObject().put("entries", jsonArray);
        return apiResponse.buildResponse();
    }
}
