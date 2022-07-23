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
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class InformationBlocks extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {

        JSONObject jsonObject = new JSONObject();

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

        jsonObject.put("active", activeInformationBlocksJSON);
        jsonObject.put("archived", archivedInformationBlocksJSON);

        return jsonObject;
    }

    private JSONObject convertToJSONObject(InformationBlock informationBlock) {
        JSONObject informationBlockJSON = new JSONObject();
        informationBlockJSON.put("id", informationBlock.getId());
        informationBlockJSON.put("name", informationBlock.getName());
        informationBlockJSON.put("description", informationBlock.getDescription());
        informationBlockJSON.put("sourcePlugin", informationBlock.getSourcePlugin().getPluginName());
        informationBlockJSON.put("active", informationBlock.isActive());
        return informationBlockJSON;
    }
}
