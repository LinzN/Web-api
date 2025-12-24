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

import org.json.JSONObject;

public class ApiResponse {

    private boolean hasApiError;
    private String errorDescription;
    private JSONObject content;

    public ApiResponse() {
        this.hasApiError = false;
        this.errorDescription = "";
        this.content = new JSONObject();
    }

    public void setError(String errorDescription) {
        this.hasApiError = true;
        this.errorDescription = errorDescription;
    }

    public JSONObject getJSONObject() {
        return this.content;
    }

    public void setFullContent(JSONObject jsonObject) {
        this.content = jsonObject;
    }

    public JSONObject buildResponse() {

        if (hasApiError) {
            this.content.put("apiError", true);
            this.content.put("errorDescription", errorDescription);
        } else {
            this.content.put("apiError", false);
        }

        return this.content;
    }
}
