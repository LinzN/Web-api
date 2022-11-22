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

package de.linzn.webapi.modules;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebModule implements HttpHandler {

    private final String moduleName;
    private final Map<String, RequestInterface> pathMap;

    public WebModule(String moduleName) {
        this.moduleName = moduleName;
        this.pathMap = new HashMap<>();
    }

    private static Map<String, String> header() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Access-Control-Allow-Origin", "*");
        map.put("Content-Type", "application/json; charset=UTF-8");
        map.put("Accept-Ranges", "bytes");
        return map;
    }

    private static String[] splitURL(String rawURL) {
        rawURL = rawURL.substring(1);
        String[] rawArray = rawURL.split("/");
        return Arrays.copyOfRange(rawArray, 1, rawArray.length);
    }

    public void registerSubCallHandler(RequestInterface requestInterface, String path) {
        this.pathMap.put(path.toLowerCase(), requestInterface);
    }

    public void unregisterSubCallHandler(String path) {
        this.pathMap.remove(path.toLowerCase());
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void handle(HttpExchange exchange) {
        String[] args = splitURL(exchange.getRequestURI().getRawPath());
        RequestInterface requestInterface = null;

        int code = 0;
        if (args.length == 0) {
            if (pathMap.containsKey("$_root")) {
                requestInterface = pathMap.get("$_root");
            }
        } else if (pathMap.containsKey(args[0].toLowerCase())) {
            requestInterface = pathMap.get(args[0].toLowerCase());
        }

        if (requestInterface == null) {
            code = 404;
        }

        try {
            Object payload = new JSONObject();

            if (requestInterface != null) {
                payload = requestInterface.callHttpEvent(exchange, new HttpRequestClientPayload(exchange));
            }

            JSONObject data = new JSONObject();
            data.put("request", exchange.getRequestURI().getRawPath());
            data.put("payload", payload);
            data.put("error", code);

            Headers h = exchange.getResponseHeaders();

            for (String key : header().keySet()) {
                h.set(key, header().get(key));
            }

            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(data.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (Exception e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
    }

}
