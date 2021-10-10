/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.restfulapi.api.jsonapi.get.internal;

import de.linzn.restfulapi.api.jsonapi.RequestData;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.notificationModule.archive.ArchivedNotification;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GET_NotificationArchive implements IRequest {
    @Override
    public Object proceedRequestData(RequestData requestData) {
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

        return jsonArray;
    }

    @Override
    public Object genericData() {
        return proceedRequestData(null);
    }

    @Override
    public String name() {
        return "notification-archive";
    }
}
