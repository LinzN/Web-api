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

package de.linzn.webapi.defaultWebModules.stemsystem;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.openJL.system.HardwareResources;
import de.linzn.systemChain.callbacks.NetworkScheduler;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;

import java.io.IOException;

public class SystemStatus extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        double load = HardwareResources.getSystemLoad();
        int cores = HardwareResources.getCoreAmount();

        double usedMemory = HardwareResources.getUsedMemory();
        double maxMemory = HardwareResources.getMaxMemory();

        int cpuLoad = (int) ((load * 100) / cores);

        int memoryLoad = (int) ((100 / maxMemory) * usedMemory);

        long spaceUsable = HardwareResources.getUsableSpace();
        long spaceTotal = HardwareResources.getTotalSpace();

        apiResponse.getJSONObject().put("ping", NetworkScheduler.getLastPing());
        apiResponse.getJSONObject().put("status", "OK");
        apiResponse.getJSONObject().put("cpuLoad", cpuLoad);
        apiResponse.getJSONObject().put("memoryLoad", memoryLoad);
        apiResponse.getJSONObject().put("memoryTotal", maxMemory);
        apiResponse.getJSONObject().put("memoryUsed", usedMemory);
        apiResponse.getJSONObject().put("spaceUsable", spaceUsable);
        apiResponse.getJSONObject().put("spaceTotal", spaceTotal);
        return apiResponse.buildResponse();
    }
}
