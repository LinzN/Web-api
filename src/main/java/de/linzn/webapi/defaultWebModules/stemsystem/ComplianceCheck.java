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