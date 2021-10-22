package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONArray;

import java.io.IOException;
import java.util.logging.LogRecord;

public class ConsoleOutput extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONArray jsonArray = new JSONArray();
        for (LogRecord entry : STEMSystemApp.LOGGER.getLastEntries(30)) {
            jsonArray.put(STEMSystemApp.logSystem.htmlFormatter.format(entry));
        }
        return jsonArray;
    }
}
