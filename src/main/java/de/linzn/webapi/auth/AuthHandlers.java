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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code /auth/login}, {@code /auth/logout}, {@code /auth/me}, {@code /auth/password} (change the own password)
 * and {@code /auth/redeem} (set the password with a setup code). These paths need no login, the handlers check
 * credentials themselves; every password check is throttled.
 */
public final class AuthHandlers implements HttpHandler {

    private static final int MAX_BODY = 16 * 1024;

    private final AuthService auth;

    public AuthHandlers(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String action = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if (action.equals("me") && method.equals("GET")) {
                Optional<AuthService.Principal> principal = this.auth.authenticate(exchange);
                if (principal.isEmpty() && this.auth.isEnabled()) {
                    send(exchange, 401, new JSONObject().put("ok", false).put("error", "Not logged in"));
                } else {
                    send(exchange, 200, new JSONObject().put("ok", true).put("loginEnabled", this.auth.isEnabled())
                            .put("user", principal.map(AuthService.Principal::user).orElse(null)));
                }
                return;
            }
            if (!method.equals("POST")) {
                send(exchange, 405, new JSONObject().put("ok", false).put("error", "POST only"));
                return;
            }
            if (!this.auth.sameOrigin(exchange)) {
                send(exchange, 403, new JSONObject().put("ok", false).put("error", "Request from a foreign page"));
                return;
            }
            JSONObject body = readJson(exchange);
            switch (action) {
                case "login" -> this.login(exchange, body);
                case "logout" -> this.logout(exchange);
                case "password" -> this.password(exchange, body);
                case "redeem" -> this.redeem(exchange, body);
                default -> send(exchange, 404, new JSONObject().put("ok", false).put("error", "Unknown action"));
            }
        }
    }

    private void login(HttpExchange exchange, JSONObject body) throws IOException {
        String address = this.auth.clientAddress(exchange);
        String user = body.optString("user", "").trim().toLowerCase(Locale.ROOT);
        if (this.throttled(exchange, address, user)) {
            return;
        }
        Optional<UserStore.User> verified = this.auth.users().verifyPassword(user, body.optString("password", ""));
        if (verified.isEmpty()) {
            this.auth.throttle().failed(address, user);
            this.auth.logger().warning("Web login failed for '" + clean(user) + "' from " + address);
            send(exchange, 401, new JSONObject().put("ok", false).put("error", "Benutzername oder Passwort falsch"));
            return;
        }
        this.auth.throttle().succeeded(address, user);
        boolean remember = body.optBoolean("remember", false);
        String id = this.auth.sessions().create(verified.get().name(), remember, address,
                exchange.getRequestHeaders().getFirst("User-Agent"));
        this.auth.users().touchLogin(verified.get().name());
        this.auth.logger().info("Web login of " + verified.get().name() + " from " + address + (remember ? " (stay logged in)" : ""));
        exchange.getResponseHeaders().add("Set-Cookie", this.auth.sessionCookie(exchange, id, remember));
        send(exchange, 200, new JSONObject().put("ok", true).put("user", verified.get().name()));
    }

    private void logout(HttpExchange exchange) throws IOException {
        String id = this.auth.cookie(exchange, this.auth.settings().cookieName());
        this.auth.sessions().remove(id);
        exchange.getResponseHeaders().add("Set-Cookie", this.auth.clearCookie(exchange));
        send(exchange, 200, new JSONObject().put("ok", true));
    }

    private void password(HttpExchange exchange, JSONObject body) throws IOException {
        String address = this.auth.clientAddress(exchange);
        String user = body.optString("user", "").trim().toLowerCase(Locale.ROOT);
        if (this.throttled(exchange, address, user)) {
            return;
        }
        Optional<UserStore.User> verified = this.auth.users().verifyPassword(user, body.optString("password", ""));
        if (verified.isEmpty()) {
            this.auth.throttle().failed(address, user);
            this.auth.logger().warning("Web password change failed for '" + clean(user) + "' from " + address);
            send(exchange, 401, new JSONObject().put("ok", false).put("error", "Benutzername oder Passwort falsch"));
            return;
        }
        try {
            this.auth.users().changePassword(verified.get(), body.optString("newPassword", ""));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, new JSONObject().put("ok", false).put("error", german(e.getMessage())));
            return;
        }
        this.auth.throttle().succeeded(address, user);
        /* other logins of the user end, the current one stays */
        int ended = this.auth.sessions().removeUser(verified.get().name(), this.auth.cookie(exchange, this.auth.settings().cookieName()));
        this.auth.logger().info("Web password of " + verified.get().name() + " changed from " + address + ", " + ended + " other session(s) ended");
        send(exchange, 200, new JSONObject().put("ok", true));
    }

    private void redeem(HttpExchange exchange, JSONObject body) throws IOException {
        String address = this.auth.clientAddress(exchange);
        String user = body.optString("user", "").trim().toLowerCase(Locale.ROOT);
        if (this.throttled(exchange, address, user)) {
            return;
        }
        UserStore.User updated;
        try {
            UserStore.checkPassword(body.optString("newPassword", ""), user);
            updated = this.auth.users().redeemCode(user, body.optString("code", ""), body.optString("newPassword", ""));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("Invalid or expired")) {
                this.auth.throttle().failed(address, user);
                this.auth.logger().warning("Web setup code rejected for '" + clean(user) + "' from " + address);
            }
            send(exchange, 400, new JSONObject().put("ok", false).put("error", german(e.getMessage())));
            return;
        }
        this.auth.throttle().succeeded(address, user);
        this.auth.sessions().removeUser(updated.name(), null);
        this.auth.logger().info("Web password of " + updated.name() + " set with a setup code from " + address);
        send(exchange, 200, new JSONObject().put("ok", true));
    }

    private boolean throttled(HttpExchange exchange, String address, String user) throws IOException {
        long wait = this.auth.throttle().waitSeconds(address, user);
        if (wait <= 0) {
            return false;
        }
        exchange.getResponseHeaders().set("Retry-After", String.valueOf(wait));
        send(exchange, 429, new JSONObject().put("ok", false).put("retryAfter", wait)
                .put("error", "Zu viele Versuche, bitte " + wait + " Sekunden warten"));
        return true;
    }

    private static String german(String message) {
        if (message.startsWith("The password needs at least")) {
            return "Das Passwort braucht mindestens " + UserStore.MIN_PASSWORD_LENGTH + " Zeichen";
        }
        if (message.startsWith("The password must not be the user name")) {
            return "Das Passwort darf nicht der Benutzername sein";
        }
        if (message.startsWith("The password is too long")) {
            return "Das Passwort ist zu lang";
        }
        if (message.startsWith("Invalid or expired code")) {
            return "Code ungültig oder abgelaufen";
        }
        return message;
    }

    /* user names come from the request; keep the log readable */
    private static String clean(String value) {
        String cut = value.length() > 40 ? value.substring(0, 40) : value;
        return cut.replaceAll("[^a-z0-9._@-]", "?");
    }

    private static JSONObject readJson(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (out.size() + read > MAX_BODY) {
                    return new JSONObject();
                }
                out.write(buffer, 0, read);
            }
            try {
                return new JSONObject(out.toString(StandardCharsets.UTF_8));
            } catch (JSONException e) {
                return new JSONObject();
            }
        }
    }

    static void send(HttpExchange exchange, int status, JSONObject json) throws IOException {
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
