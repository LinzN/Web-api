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


package de.linzn.webapi.defaultWebModules.emergency;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.stem.taskManagment.operations.defaultOperations.StemRestartOperation;
import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Emergency restart of STEM. Only accepted from the local network (loopback, private and link-local
 * addresses). If "emergency.token" is configured, the request must also contain that token.
 */
public class Reboot extends RequestInterface {

    private static boolean isLocalNetwork(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        JSONObject postData = httpRequestClientPayload.getPostJson();
        InetAddress remoteAddress = exchange.getRemoteAddress().getAddress();
        String forwardedFor = exchange.getRequestHeaders().getFirst("X-FORWARDED-FOR");

        WebApiPlugin plugin = WebApiPlugin.webApiPlugin;
        plugin.getPluginLogger().warning("--EMERGENCY WEBAPI-- reboot requested by " + remoteAddress.getHostAddress()
                + (forwardedFor != null ? " (forwarded for " + forwardedFor + ")" : ""));

        if (!isLocalNetwork(remoteAddress)) {
            plugin.getPluginLogger().warning("Emergency reboot rejected: request not from the local network");
            return jsonObject.put("error", 403);
        }
        String expectedToken = plugin.getDefaultConfig().getString("emergency.token", "");
        if (!expectedToken.isEmpty()) {
            String token = postData.optString("token", "");
            boolean valid = MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                plugin.getPluginLogger().warning("Emergency reboot rejected: invalid token");
                return jsonObject.put("error", 403);
            }
        }

        if ("WRITE".equalsIgnoreCase(postData.optString("requestAction", ""))) {
            plugin.getStemApi().scheduler().runAsync(new StemRestartOperation());
            jsonObject.put("error", 0);
        } else {
            jsonObject.put("error", 404);
        }
        return jsonObject;
    }
}
