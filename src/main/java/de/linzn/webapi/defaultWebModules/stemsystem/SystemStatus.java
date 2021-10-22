package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.systemChain.callbacks.NetworkScheduler;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.utils.JavaUtils;
import org.json.JSONObject;

import java.io.IOException;

public class SystemStatus extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        double load = JavaUtils.getSystemLoad();
        int cores = JavaUtils.getCoreAmount();

        double usedMemory = JavaUtils.getUsedMemory();
        double maxMemory = JavaUtils.getMaxMemory();

        int cpuLoad = (int) ((load * 100) / cores);

        int memoryLoad = (int) ((100 / maxMemory) * usedMemory);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ping", NetworkScheduler.getLastPing());
        jsonObject.put("status", "OK");
        jsonObject.put("cpuLoad", cpuLoad);
        jsonObject.put("memoryLoad", memoryLoad);
        jsonObject.put("memoryTotal", maxMemory);
        jsonObject.put("memoryUsed", usedMemory);
        return jsonObject;
    }
}
