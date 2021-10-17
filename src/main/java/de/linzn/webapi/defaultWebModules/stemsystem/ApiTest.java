package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.handler.RequestInterface;
import org.json.JSONObject;

import java.io.IOException;

public class ApiTest extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("postData", httpRequestClientPayload.getPostData());
        jsonObject.put("queryData", httpRequestClientPayload.getQueryData());
        return jsonObject;
    }

}
