package de.linzn.webapi.defaultSubHandler.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.handler.SubCallHandler;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONArray;

import java.io.IOException;
import java.util.logging.LogRecord;

public class ConsoleOutput extends SubCallHandler {

    @Override
    public Object callHttpEvent(HttpExchange exchange) throws IOException {
        JSONArray jsonArray = new JSONArray();
        for (LogRecord entry : STEMSystemApp.LOGGER.getLastEntries(30)) {
            jsonArray.put(STEMSystemApp.logSystem.htmlFormatter.format(entry));
        }
        return jsonArray;
    }
}
