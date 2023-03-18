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
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InformationBlocks extends RequestInterface {
    Format dateFormat = new SimpleDateFormat("EEEE d MMMMM yyyy - HH:mm:ss", Locale.GERMANY);

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {

        ApiResponse apiResponse = new ApiResponse();


        JSONArray activeInformationBlocksJSON = new JSONArray();
        ArrayList<InformationBlock> activeInformationBlocks = STEMSystemApp.getInstance().getInformationModule().getActiveInformationBlocks();

        for (InformationBlock informationBlock : activeInformationBlocks) {
            activeInformationBlocksJSON.put(convertToJSONObject(informationBlock));
        }

        JSONArray archivedInformationBlocksJSON = new JSONArray();
        ArrayList<InformationBlock> archivedInformationBlocks = STEMSystemApp.getInstance().getInformationModule().getArchivedInformationBlocks();

        for (InformationBlock informationBlock : archivedInformationBlocks) {
            archivedInformationBlocksJSON.put(convertToJSONObject(informationBlock));
        }

        apiResponse.getJSONObject().put("active", activeInformationBlocksJSON);
        apiResponse.getJSONObject().put("archived", archivedInformationBlocksJSON);

        return apiResponse.buildResponse();
    }

    private JSONObject convertToJSONObject(InformationBlock informationBlock) {
        JSONObject informationBlockJSON = new JSONObject();
        informationBlockJSON.put("id", informationBlock.getId());
        informationBlockJSON.put("name", informationBlock.getName());
        informationBlockJSON.put("description", informationBlock.getDescription());
        informationBlockJSON.put("sourcePlugin", informationBlock.getSourcePlugin().getPluginName());
        informationBlockJSON.put("icon", informationBlock.getIcon());
        informationBlockJSON.put("active", informationBlock.isActive());
        informationBlockJSON.put("creationDate", dateFormat.format(new Date(informationBlock.getCreationTime())));
        return informationBlockJSON;
    }
}
