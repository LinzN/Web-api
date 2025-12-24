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
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ComplianceCheck extends RequestInterface {


    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        ArrayList<de.stem.stemSystem.modules.complianceModule.ComplianceCheck> complianceChecks = STEMSystemApp.getInstance().getComplianceModule().getComplianceChecks();

        JSONObject complianceObjects = new JSONObject();
        for (de.stem.stemSystem.modules.complianceModule.ComplianceCheck complianceCheck : complianceChecks){
            complianceObjects.put(complianceCheck.getComplianceID(), complianceCheck.getStatus());
        }
        apiResponse.getJSONObject().put("complianceChecks", complianceObjects);

        return apiResponse.buildResponse();
    }
}