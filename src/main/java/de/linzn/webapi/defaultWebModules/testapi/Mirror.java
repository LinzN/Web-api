package de.linzn.webapi.defaultWebModules.testapi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONObject;

import java.io.IOException;

public class Mirror extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("postData", httpRequestClientPayload.getPostData());
        jsonObject.put("queryData", httpRequestClientPayload.getQueryData());
        return jsonObject;
    }

}
