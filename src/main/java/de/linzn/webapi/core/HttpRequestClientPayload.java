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


package de.linzn.webapi.core;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Parsed request data: URL-decoded query parameters and the request body (JSON object, JSON array or text).
 * The body is limited to {@link #MAX_BODY_BYTES} to protect the server's memory.
 */
public class HttpRequestClientPayload {
    public static final int MAX_BODY_BYTES = 1024 * 1024;

    private final HttpExchange httpExchange;
    private final Map<String, String> queryData;
    private final Object postData;

    public HttpRequestClientPayload(HttpExchange httpExchange) throws IOException {
        this.httpExchange = httpExchange;
        this.queryData = this.readQueryData();
        this.postData = parseJson(this.readBody());
    }

    private static Object parseJson(String data) {
        String trimmed = data.trim();
        if (trimmed.isEmpty()) {
            return data;
        }
        try {
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed);
            }
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
        } catch (JSONException ignored) {
            /* not valid JSON, keep the raw text */
        }
        return data;
    }

    private Map<String, String> readQueryData() {
        Map<String, String> result = new TreeMap<>();
        String query = httpExchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            if (param.isEmpty()) {
                continue;
            }
            String[] entry = param.split("=", 2);
            String key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8);
            String value = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    private String readBody() throws IOException {
        try (InputStream in = httpExchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (out.size() + read > MAX_BODY_BYTES) {
                    throw new IOException("Request body exceeds " + MAX_BODY_BYTES + " bytes");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    public Object getPostData() {
        return postData;
    }

    /**
     * @return the body as JSON object, or an empty object if the body is not a JSON object
     */
    public JSONObject getPostJson() {
        return postData instanceof JSONObject jsonObject ? jsonObject : new JSONObject();
    }

    public Map<String, String> getQueryData() {
        return queryData;
    }
}
