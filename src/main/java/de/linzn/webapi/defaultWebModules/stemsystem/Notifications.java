package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.handler.RequestInterface;
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
}
