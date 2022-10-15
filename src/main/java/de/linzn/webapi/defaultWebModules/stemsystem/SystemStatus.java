/*
 *  Copyright (C) 2021. Niklas Linz - All Rights Reserved
 *  You may use, distribute and modify this code under the
 *  terms of the LGPLv3 license, which unfortunately won't be
 *  written for another century.
 *
 *  You should have received a copy of the LGPLv3 license with
 *  this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.openJL.system.HardwareResources;
import de.linzn.systemChain.callbacks.NetworkScheduler;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.utils.JavaUtils;
import org.json.JSONObject;

import java.io.IOException;

public class SystemStatus extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        double load = HardwareResources.getSystemLoad();
        int cores = HardwareResources.getCoreAmount();

        double usedMemory = HardwareResources.getUsedMemory();
        double maxMemory = HardwareResources.getMaxMemory();

        int cpuLoad = (int) ((load * 100) / cores);

        int memoryLoad = (int) ((100 / maxMemory) * usedMemory);

        long spaceUsable = HardwareResources.getUsableSpace();
        long spaceTotal = HardwareResources.getTotalSpace();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ping", NetworkScheduler.getLastPing());
        jsonObject.put("status", "OK");
        jsonObject.put("cpuLoad", cpuLoad);
        jsonObject.put("memoryLoad", memoryLoad);
        jsonObject.put("memoryTotal", maxMemory);
        jsonObject.put("memoryUsed", usedMemory);
        jsonObject.put("spaceUsable", spaceUsable);
        jsonObject.put("spaceTotal", spaceTotal);
        return jsonObject;
    }
}
