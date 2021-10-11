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

package de.linzn.webapi.core;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONTemplate {

    private String generatedPage;

    public JSONTemplate() {
        this.generatedPage = new JSONObject().put("error", 404).toString();
    }

    public void setCode(Object object) {
        this.generatedPage = object.toString();
    }

    public Map<String, String> headerList() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Access-Control-Allow-Origin", "*");
        map.put("Content-Type", "application/json; charset=UTF-8");
        map.put("Accept-Ranges", "bytes");
        return map;
    }

    public long length() {
        return this.generatedPage.getBytes().length;
    }

    public byte[] getBytes() {
        return this.generatedPage.getBytes();
    }

}
