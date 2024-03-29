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
import de.stem.stemSystem.modules.notificationModule.archive.ArchivedNotification;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class Notifications extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();
        Format dateFormat = new SimpleDateFormat("EEEE d MMMMM yyyy", Locale.GERMANY);

        List<ArchivedNotification> list = STEMSystemApp.getInstance().getNotificationModule().getNotificationArchive().getLastNotifications();

        JSONArray jsonArray = new JSONArray();
        int i = 1;
        for (ArchivedNotification notificationArchiveObject : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", i);
            jsonObject.put("source", notificationArchiveObject.source);
            jsonObject.put("notification", notificationArchiveObject.notification);
            jsonObject.put("date", dateFormat.format(notificationArchiveObject.date));
            jsonArray.put(jsonObject);
            i++;
        }

        apiResponse.getJSONObject().put("entries", jsonArray);
        return apiResponse.buildResponse();
    }
}
