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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login sessions. The browser keeps a random session id in a cookie; the server only stores its SHA-256, so a
 * leaked {@code sessions.json} does not contain usable sessions. Sessions survive a STEM restart. A session is
 * extended while it is used (sliding expiry) and ends after its idle time.
 */
public final class SessionStore {

    /* last use is written at most this often, the dashboard polls every second */
    private static final Duration SAVE_INTERVAL = Duration.ofMinutes(10);
    private static final int MAX_SESSIONS_PER_USER = 20;

    public record Session(String hash, String user, boolean remember, Instant created, Instant lastSeen, Instant expires,
                          String address, String agent) {
        JSONObject toJson() {
            return new JSONObject().put("hash", hash).put("user", user).put("remember", remember).put("created", created.toString())
                    .put("lastSeen", lastSeen.toString()).put("expires", expires.toString()).put("address", address).put("agent", agent);
        }

        static Session fromJson(JSONObject json) {
            return new Session(json.getString("hash"), json.getString("user"), json.optBoolean("remember"),
                    Instant.parse(json.getString("created")), Instant.parse(json.getString("lastSeen")),
                    Instant.parse(json.getString("expires")), json.optString("address", ""), json.optString("agent", ""));
        }
    }

    private final Path file;
    private final Duration sessionIdle;
    private final Duration rememberIdle;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private volatile Instant lastSave = Instant.EPOCH;

    /**
     * @param sessionIdle  idle time of a normal login
     * @param rememberIdle idle time of a login with "stay logged in"
     */
    public SessionStore(Path folder, Duration sessionIdle, Duration rememberIdle) throws IOException {
        this.file = folder.resolve("sessions.json");
        this.sessionIdle = sessionIdle;
        this.rememberIdle = rememberIdle;
        if (Files.exists(this.file)) {
            JSONArray array = new JSONArray(Files.readString(this.file));
            for (int i = 0; i < array.length(); i++) {
                Session session = Session.fromJson(array.getJSONObject(i));
                if (session.expires().isAfter(Instant.now())) {
                    this.sessions.put(session.hash(), session);
                }
            }
        }
    }

    /**
     * @return the session id for the cookie (only the hash is kept)
     */
    public String create(String user, boolean remember, String address, String agent) {
        String id = Secrets.token(32);
        Instant now = Instant.now();
        Session session = new Session(Secrets.sha256(id), user, remember, now, now, now.plus(remember ? this.rememberIdle : this.sessionIdle),
                address, agent == null ? "" : agent.substring(0, Math.min(agent.length(), 200)));
        this.sessions.put(session.hash(), session);
        /* keep the newest sessions of a user */
        List<Session> own = this.sessions.values().stream().filter(s -> s.user().equals(user))
                .sorted(Comparator.comparing(Session::lastSeen).reversed()).toList();
        own.stream().skip(MAX_SESSIONS_PER_USER).forEach(old -> this.sessions.remove(old.hash()));
        this.save();
        return id;
    }

    /**
     * @return the session of the id, extended by its idle time, or empty if unknown or expired
     */
    public Optional<Session> touch(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String hash = Secrets.sha256(id);
        Session session = this.sessions.get(hash);
        Instant now = Instant.now();
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expires().isAfter(now)) {
            this.sessions.remove(hash);
            return Optional.empty();
        }
        Session updated = new Session(session.hash(), session.user(), session.remember(), session.created(), now,
                now.plus(session.remember() ? this.rememberIdle : this.sessionIdle), session.address(), session.agent());
        this.sessions.put(hash, updated);
        if (this.lastSave.plus(SAVE_INTERVAL).isBefore(now)) {
            this.save();
        }
        return Optional.of(updated);
    }

    public void remove(String id) {
        if (id != null && this.sessions.remove(Secrets.sha256(id)) != null) {
            this.save();
        }
    }

    /**
     * Log out all sessions of a user (password changed, user removed, console).
     *
     * @param except session id that stays, may be null
     * @return number of ended sessions
     */
    public int removeUser(String user, String except) {
        String keep = except == null ? null : Secrets.sha256(except);
        int before = this.sessions.size();
        this.sessions.values().removeIf(session -> session.user().equals(user) && !session.hash().equals(keep));
        int removed = before - this.sessions.size();
        if (removed > 0) {
            this.save();
        }
        return removed;
    }

    public int removeAll() {
        int count = this.sessions.size();
        this.sessions.clear();
        this.save();
        return count;
    }

    public List<Session> list() {
        Instant now = Instant.now();
        this.sessions.values().removeIf(session -> !session.expires().isAfter(now));
        List<Session> list = new ArrayList<>(this.sessions.values());
        list.sort(Comparator.comparing(Session::user).thenComparing(Session::lastSeen));
        return list;
    }

    public synchronized void save() {
        this.lastSave = Instant.now();
        JSONArray array = new JSONArray();
        this.sessions.values().forEach(session -> array.put(session.toJson()));
        try {
            UserStore.writePrivate(this.file, array.toString());
        } catch (IOException e) {
            /* sessions only live in memory until the next save */
        }
    }
}
