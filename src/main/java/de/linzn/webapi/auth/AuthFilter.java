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

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Lets only logged in requests through. API requests without login get {@code 401} as JSON; page requests of a
 * browser are redirected to the login page and come back after the login.
 */
public final class AuthFilter extends Filter {

    public enum Mode {
        /** JSON endpoints: 401 */
        API,
        /** pages: redirect to the login page */
        PAGE
    }

    private final AuthService auth;
    private final Mode mode;

    public AuthFilter(AuthService auth, Mode mode) {
        this.auth = auth;
        this.mode = mode;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        AuthService.setCurrent(null);
        if (!this.auth.isEnabled() || "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())
                || this.auth.isPublic(exchange.getRequestURI().getPath())) {
            chain.doFilter(exchange);
            return;
        }
        Optional<AuthService.Principal> principal = this.auth.authenticate(exchange);
        if (principal.isEmpty()) {
            if (this.mode == Mode.PAGE && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                redirectToLogin(exchange);
            } else {
                reject(exchange, 401, "Login required");
            }
            return;
        }
        if (!principal.get().token() && unsafe(exchange.getRequestMethod()) && !this.auth.sameOrigin(exchange)) {
            this.auth.logger().warning("Web request from foreign origin " + exchange.getRequestHeaders().getFirst("Origin")
                    + " rejected for " + principal.get().user() + ": " + exchange.getRequestURI().getPath());
            reject(exchange, 403, "Request from a foreign page");
            return;
        }
        AuthService.setCurrent(principal.get());
        try {
            chain.doFilter(exchange);
        } finally {
            AuthService.setCurrent(null);
        }
    }

    @Override
    public String description() {
        return "STEM web login (" + this.mode + ")";
    }

    private static boolean unsafe(String method) {
        String upper = method.toUpperCase(Locale.ROOT);
        return !upper.equals("GET") && !upper.equals("HEAD");
    }

    private void redirectToLogin(HttpExchange exchange) throws IOException {
        try (exchange) {
            String target = exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
            exchange.getResponseHeaders().set("Location", this.auth.settings().loginPath() + "?next="
                    + URLEncoder.encode(target, StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    static void reject(HttpExchange exchange, int status, String message) throws IOException {
        try (exchange) {
            byte[] body = new JSONObject().put("request", exchange.getRequestURI().getRawPath())
                    .put("payload", new JSONObject().put("apiError", true).put("errorDescription", message))
                    .put("error", status).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }
}
