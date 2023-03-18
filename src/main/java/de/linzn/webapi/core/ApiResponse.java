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
