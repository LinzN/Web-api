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

package de.linzn.restfulapi.api.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.linzn.openJL.network.IPAddressMatcher;
import de.linzn.restfulapi.RestFulApiPlugin;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import de.linzn.restfulapi.api.jsonapi.get.internal.*;
import de.linzn.restfulapi.api.jsonapi.post.internal.POST_ExecuteStemCommand;
import de.linzn.restfulapi.core.JSONTemplate;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ApiHandler implements HttpHandler {

    private List<IRequest> getList;
    private List<IRequest> postList;

    public ApiHandler() {
        this.getList = new ArrayList<>();
        this.postList = new ArrayList<>();
        this.registerInternalHandlers();
    }


    @Override
    public void handle(HttpExchange he) {
        try {
            handleRequests(he);
        } catch (Exception e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
    }

    private void handleRequests(final HttpExchange he) throws IOException {
        List<String> whitelist = RestFulApiPlugin.restFulApiPlugin.getDefaultConfig().getStringList("apiServer.whitelist");
        String requestingAddress = he.getRemoteAddress().getAddress().getHostName();

        boolean matched = false;
        for (String ip : whitelist) {
            if (new IPAddressMatcher(ip).matches(requestingAddress)) {
                matched = true;
                break;
            }
        }

        if (!matched) {
            STEMSystemApp.LOGGER.ERROR("[REST_API] Access deny for " + requestingAddress);
            he.close();
            return;
        }

        String url = he.getRequestURI().getRawPath();

        List<String> argsList = Arrays.stream(url.split("/")).filter(arg -> !arg.isEmpty()).collect(Collectors.toList());
        JSONTemplate jsonTemplate = new JSONTemplate();

        Map<String, String> postQueryData = queryToMap(he);

        if (!argsList.isEmpty()) {
            String command = argsList.get(0);
            argsList.remove(0);

            if (command.toLowerCase().startsWith("get_")) {
                String split_command = command.substring(4);
                if (split_command.equalsIgnoreCase("generic")) {
                    jsonTemplate.setCode(buildGeneric());
                } else {
                    for (IRequest iRequest : this.getList) {
                        if (iRequest.name().equalsIgnoreCase(split_command)) {
                            jsonTemplate.setCode(iRequest.proceedRequestData(new RequestData(argsList, postQueryData, he.getRemoteAddress())));
                        }
                    }
                }
            } else if (command.toLowerCase().startsWith("post_")) {
                String split_command = command.substring(5);
                for (IRequest iPostJSON : this.postList) {
                    if (iPostJSON.name().equalsIgnoreCase(split_command)) {
                        jsonTemplate.setCode(iPostJSON.proceedRequestData(new RequestData(argsList, postQueryData, he.getRemoteAddress())));
                    }
                }
            }
        }

        Headers h = he.getResponseHeaders();

        for (String key : jsonTemplate.headerList().keySet()) {
            h.set(key, jsonTemplate.headerList().get(key));
        }

        he.sendResponseHeaders(200, jsonTemplate.length());
        OutputStream os = he.getResponseBody();
        os.write(jsonTemplate.getBytes());
        os.close();
    }

    public void addGetHandler(IRequest iRequest) {
        this.getList.add(iRequest);
    }

    public void addPostHandler(IRequest iRequest) {
        this.postList.add(iRequest);
    }

    private void registerInternalHandlers() {
        this.addGetHandler(new GET_NotificationArchive());
        this.addGetHandler(new GET_Resources());
        this.addGetHandler(new GET_Terminal());
        this.addGetHandler(new GET_Stem());
        this.addGetHandler(new GET_Network());
        this.addGetHandler(new GET_TestApi());

        this.addPostHandler(new POST_ExecuteStemCommand());
    }

    private JSONObject buildGeneric() {
        JSONObject jsonObject = new JSONObject();

        List<IRequest> list = this.getList;
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            IRequest iRequest = list.get(i);
            try {
                Object data = iRequest.genericData();
                if (data != null) {
                    jsonObject.put(iRequest.name(), data);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return jsonObject;
    }

    private Map<String, String> queryToMap(HttpExchange httpExchange) {
        String query = httpExchange.getRequestURI().getQuery();
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
        }

        InputStream input = httpExchange.getRequestBody();
        StringBuilder stringBuilder = new StringBuilder();

        new BufferedReader(new InputStreamReader(input))
                .lines()
                .forEach((String s) -> stringBuilder.append(s + "\n"));

        if (!stringBuilder.toString().isEmpty()) {
            result.put("requestBody", stringBuilder.toString());
        }

        return result;
    }
}

