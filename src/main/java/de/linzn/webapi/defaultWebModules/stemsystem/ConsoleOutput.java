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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

public class ConsoleOutput extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONArray jsonArray = new JSONArray();
        final List<LogRecord> list = STEMSystemApp.LOGGER.getLastEntries(30);
        for (LogRecord entry : new ArrayList<>(list)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", entry.getMessage());
            jsonObject.put("htmlText", STEMSystemApp.logSystem.htmlFormatter.format(entry));
            jsonObject.put("level", entry.getLevel().getName());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
}
