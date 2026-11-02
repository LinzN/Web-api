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


package de.linzn.webapi.modules;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.core.HttpRequestClientPayload;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes "/module/sub-path" requests to registered handlers. Handlers can be registered and removed
 * by other plugins at any time. Every exchange is closed, also when a handler fails.
 */
public class WebModule implements HttpHandler {
    private static final Map<String, String> HEADERS = Map.of(
            "Access-Control-Allow-Origin", "*",
            "Content-Type", "application/json; charset=UTF-8",
            "Cache-Control", "no-store",
            "X-Content-Type-Options", "nosniff");

    private final String moduleName;
    private final Map<String, RequestInterface> pathMap;

    public WebModule(String moduleName) {
        this.moduleName = moduleName;
        this.pathMap = new ConcurrentHashMap<>();
    }

    /**
     * @return the path segments after the context path ("/apiv2/homedevice/device" in context "/apiv2/homedevice"
     * gives ["device"])
     */
    private static String[] splitURL(String rawURL, String contextPath) {
        String rest = rawURL.length() >= contextPath.length() ? rawURL.substring(contextPath.length()) : "";
        rest = rest.startsWith("/") ? rest.substring(1) : rest;
        if (rest.isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(rest.split("/")).filter(part -> !part.isEmpty()).toArray(String[]::new);
    }

    public void registerSubCallHandler(RequestInterface requestInterface, String path) {
        this.pathMap.put(path.toLowerCase(Locale.ROOT), requestInterface);
    }

    public void unregisterSubCallHandler(String path) {
        this.pathMap.remove(path.toLowerCase(Locale.ROOT));
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try (exchange) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                Headers headers = exchange.getResponseHeaders();
                HEADERS.forEach(headers::set);
                headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String[] args = splitURL(exchange.getRequestURI().getRawPath(), exchange.getHttpContext().getPath());
            RequestInterface requestInterface = args.length == 0
                    ? pathMap.get("$_root")
                    : pathMap.get(args[0].toLowerCase(Locale.ROOT));

            int code = 0;
            Object payload = new JSONObject();
            if (requestInterface == null) {
                code = 404;
            } else {
                try {
                    payload = requestInterface.callHttpEvent(exchange, new HttpRequestClientPayload(exchange));
                } catch (Exception e) {
                    WebApiPlugin.webApiPlugin.getPluginLogger().error("Error in web request " + exchange.getRequestURI().getRawPath(), e);
                    code = 500;
                    payload = new JSONObject().put("message", "Internal error");
                }
            }
            this.sendResponse(exchange, code, payload);
        } catch (IOException e) {
            WebApiPlugin.webApiPlugin.getPluginLogger().debug("Web request aborted: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int code, Object payload) throws IOException {
        JSONObject data = new JSONObject();
        data.put("request", exchange.getRequestURI().getRawPath());
        data.put("payload", payload == null ? JSONObject.NULL : payload);
        data.put("error", code);
        byte[] body = data.toString().getBytes(StandardCharsets.UTF_8);

        Headers headers = exchange.getResponseHeaders();
        HEADERS.forEach(headers::set);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
