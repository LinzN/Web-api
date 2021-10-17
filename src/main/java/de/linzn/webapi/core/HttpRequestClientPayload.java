package de.linzn.webapi.core;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class HttpRequestClientPayload {
    private final HttpExchange httpExchange;
    private Map<String, String> queryData;
    private Object postData;

    public HttpRequestClientPayload(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        this.readQueryData();
        this.readPostData();
    }


    private void readQueryData() {
        String query = httpExchange.getRequestURI().getQuery();
        this.queryData = new TreeMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    queryData.put(entry[0], entry[1]);
                } else {
                    queryData.put(entry[0], "");
                }
            }
        }
    }

    private void readPostData() {
        StringBuilder stringBuilder = new StringBuilder();
        new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()))
                .lines()
                .forEach((String s) -> stringBuilder.append(s).append("\n"));
        this.postData = parseJson(stringBuilder.toString());
    }

    private Object parseJson(String data) {
        try {
            return new JSONObject(data);
        } catch (JSONException ex) {
            try {
                return new JSONArray(data);
            } catch (JSONException ex1) {
                return data;
            }
        }
    }

    public Object getPostData() {
        return postData;
    }

    public Map<String, String> getQueryData() {
        return queryData;
    }
}