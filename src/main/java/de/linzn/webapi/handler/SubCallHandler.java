package de.linzn.webapi.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.stem.stemSystem.STEMSystemApp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SubCallHandler implements HttpHandler {

    private final CallModule callModule;
    private final String subPath;

    public SubCallHandler(CallModule callModule, String subPath) {
        this.callModule = callModule;
        this.subPath = subPath;
    }

    private static Map<String, String> headerList() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Access-Control-Allow-Origin", "*");
        map.put("Content-Type", "application/json; charset=UTF-8");
        map.put("Accept-Ranges", "bytes");
        return map;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            Object response = callHttpEvent(exchange);
            Headers h = exchange.getResponseHeaders();

            for (String key : headerList().keySet()) {
                h.set(key, headerList().get(key));
            }

            exchange.sendResponseHeaders(200, response.toString().length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.toString().getBytes());
            os.close();
        } catch (IOException e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }


    }

    public String getWebPath() {
        return "/" + callModule.getModuleName() + "/" + subPath;
    }


    public abstract Object callHttpEvent(HttpExchange exchange) throws IOException;
}