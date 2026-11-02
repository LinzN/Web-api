/*
 * Copyright (c) 2026 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.linzn.webapi.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The login page (self-contained HTML, no external files). Its script runs with a per-response nonce under a
 * strict Content-Security-Policy.
 */
public final class LoginPage implements HttpHandler {

    private final String template;
    private final String title;

    public LoginPage(String title) throws IOException {
        try (InputStream in = LoginPage.class.getResourceAsStream("/web/login.html")) {
            if (in == null) {
                throw new IOException("login.html missing in the plugin");
            }
            this.template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        this.title = title;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String nonce = Secrets.token(16);
            byte[] body = this.template.replace("__NONCE__", nonce).replace("__TITLE__", escape(this.title))
                    .getBytes(StandardCharsets.UTF_8);
            var headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=UTF-8");
            headers.set("Cache-Control", "no-store");
            headers.set("Content-Security-Policy", "default-src 'none'; script-src 'nonce-" + nonce + "'; style-src 'nonce-" + nonce
                    + "'; connect-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "same-origin");
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
