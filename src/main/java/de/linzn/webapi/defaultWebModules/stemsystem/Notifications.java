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
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.notificationModule.archive.ArchivedNotification;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
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

        List<ArchivedNotification> list = STEMApp.getInstance().getNotificationModule().getNotificationArchive().getLastNotifications();

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
