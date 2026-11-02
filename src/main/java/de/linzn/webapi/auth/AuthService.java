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
import com.sun.net.httpserver.HttpsExchange;
import de.linzn.stem.api.StemLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * The login of the web server, shared by the API and every page another plugin registers on it: one login, one
 * session cookie. Browsers log in with user name and password and get a session cookie; scripts and devices use an
 * API token ({@code Authorization: Bearer ...}).
 */
public final class AuthService {

    /**
     * Who made a request.
     *
     * @param user      user name, or the token name for API tokens
     * @param token     true for an API token
     * @param sessionId the session id of a browser login, null for tokens
     */
    public record Principal(String user, boolean token, String sessionId) {
    }

    /*
     * The principal of the request handled on this thread. Not an exchange attribute: the JDK server keeps
     * exchange attributes per context, so they are shared between requests.
     */
    private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

    /**
     * @return who sent the request that is handled on this thread (inside an API module or page handler), empty
     * without login
     */
    public static Optional<Principal> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    static void setCurrent(Principal principal) {
        if (principal == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(principal);
        }
    }

    /**
     * Settings of the login.
     *
     * @param enabled        false: no login at all (as before)
     * @param cookieName     name of the session cookie
     * @param secureCookie   "auto" (Secure when the request came in over HTTPS, also through a trusted proxy), "true", "false"
     * @param sessionIdle    idle time of a login
     * @param rememberIdle   idle time of a login with "stay logged in"
     * @param loginPath      path of the login page
     * @param publicPaths    paths (prefixes) that need no login
     * @param trustedProxies addresses of reverse proxies whose X-Forwarded-* headers are believed
     */
    public record Settings(boolean enabled, String cookieName, String secureCookie, Duration sessionIdle,
                           Duration rememberIdle, String loginPath, List<String> publicPaths, List<String> trustedProxies) {
    }

    private final Settings settings;
    private final UserStore users;
    private final SessionStore sessions;
    private final LoginThrottle throttle = new LoginThrottle();
    private final StemLogger logger;
    private final Set<InetAddress> trustedProxies = new HashSet<>();

    public AuthService(Settings settings, Path folder, StemLogger logger) throws IOException {
        this.settings = settings;
        this.logger = logger;
        this.users = new UserStore(folder);
        this.sessions = new SessionStore(folder, settings.sessionIdle(), settings.rememberIdle());
        for (String proxy : settings.trustedProxies()) {
            try {
                this.trustedProxies.add(InetAddress.getByName(proxy.trim()));
            } catch (UnknownHostException e) {
                logger.warning("Unknown trusted proxy " + proxy);
            }
        }
    }

    public Settings settings() {
        return this.settings;
    }

    public boolean isEnabled() {
        return this.settings.enabled();
    }

    public UserStore users() {
        return this.users;
    }

    public SessionStore sessions() {
        return this.sessions;
    }

    LoginThrottle throttle() {
        return this.throttle;
    }

    StemLogger logger() {
        return this.logger;
    }

    /**
     * @return who sent the request: a valid API token or session cookie
     */
    public Optional<Principal> authenticate(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return this.users.verifyToken(authorization.substring(7)).map(token -> new Principal(token.name(), true, null));
        }
        String id = this.cookie(exchange, this.settings.cookieName());
        return this.sessions.touch(id)
                .filter(session -> this.users.find(session.user()).isPresent())
                .map(session -> new Principal(session.user(), false, id));
    }

    public boolean isPublic(String path) {
        if (path.equals(this.settings.loginPath())) {
            return true;
        }
        for (String prefix : this.settings.publicPaths()) {
            if (!prefix.isBlank() && (path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Address of the client; behind a trusted reverse proxy the last address in X-Forwarded-For that is no proxy.
     */
    public String clientAddress(HttpExchange exchange) {
        InetAddress remote = exchange.getRemoteAddress().getAddress();
        if (!this.isTrustedProxy(remote)) {
            return remote.getHostAddress();
        }
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remote.getHostAddress();
        }
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            try {
                if (!hop.isEmpty() && !this.isTrustedProxy(InetAddress.getByName(hop))) {
                    return hop;
                }
            } catch (UnknownHostException e) {
                return hop;
            }
        }
        return hops[0].trim();
    }

    private boolean isTrustedProxy(InetAddress address) {
        return this.trustedProxies.contains(address);
    }

    /**
     * Requests that change something with a session cookie must come from the page itself: a browser sends Origin
     * (or Sec-Fetch-Site) with every cross-site request, so a foreign page cannot use the cookie (CSRF).
     */
    public boolean sameOrigin(HttpExchange exchange) {
        String fetchSite = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if (fetchSite != null && !fetchSite.equalsIgnoreCase("same-origin") && !fetchSite.equalsIgnoreCase("none")) {
            return false;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }
        String host = this.requestHost(exchange);
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            URI from = URI.create(origin.trim());
            URI to = URI.create("http://" + host.trim());
            if (from.getHost() == null || to.getHost() == null) {
                return false;
            }
            int defaultPort = "https".equalsIgnoreCase(from.getScheme()) ? 443 : 80;
            int fromPort = from.getPort() == -1 ? defaultPort : from.getPort();
            /* without a port in Host the browser used the default port of the page's scheme */
            int toPort = to.getPort() == -1 ? defaultPort : to.getPort();
            return from.getHost().equalsIgnoreCase(to.getHost()) && fromPort == toPort;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String requestHost(HttpExchange exchange) {
        if (this.isTrustedProxy(exchange.getRemoteAddress().getAddress())) {
            String forwardedHost = exchange.getRequestHeaders().getFirst("X-Forwarded-Host");
            if (forwardedHost != null && !forwardedHost.isBlank()) {
                return forwardedHost.split(",")[0].trim();
            }
        }
        return exchange.getRequestHeaders().getFirst("Host");
    }

    public boolean isHttps(HttpExchange exchange) {
        if (exchange instanceof HttpsExchange) {
            return true;
        }
        if (this.isTrustedProxy(exchange.getRemoteAddress().getAddress())) {
            String proto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
            return proto != null && proto.trim().toLowerCase(Locale.ROOT).startsWith("https");
        }
        return false;
    }

    /* ------------------------------------------------------------------ cookies */

    public String cookie(HttpExchange exchange, String name) {
        List<String> headers = exchange.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                int index = part.indexOf('=');
                if (index > 0 && part.substring(0, index).trim().equals(name)) {
                    return part.substring(index + 1).trim();
                }
            }
        }
        return null;
    }

    String sessionCookie(HttpExchange exchange, String id, boolean remember) {
        StringBuilder cookie = new StringBuilder(this.settings.cookieName()).append('=').append(id)
                .append("; Path=/; HttpOnly; SameSite=Lax");
        if (remember) {
            cookie.append("; Max-Age=").append(this.settings.rememberIdle().toSeconds());
        }
        if (this.secure(exchange)) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    String clearCookie(HttpExchange exchange) {
        return this.settings.cookieName() + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0" + (this.secure(exchange) ? "; Secure" : "");
    }

    private boolean secure(HttpExchange exchange) {
        return switch (this.settings.secureCookie().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> this.isHttps(exchange);
        };
    }
}
