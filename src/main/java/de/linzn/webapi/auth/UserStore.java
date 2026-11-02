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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Users and API tokens of the web login, stored in {@code users.json} (file mode 600). Passwords are stored as
 * PBKDF2 hash, setup codes and API tokens as SHA-256. Secrets that are handed out (setup codes, new API tokens) are
 * written to files in {@code codes/} and never to the log, because the STEM log can be read through the web API
 * and by Mirra.
 */
public final class UserStore {

    public static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9._-]{0,31}");
    public static final int MIN_PASSWORD_LENGTH = 10;
    private static final Duration CODE_VALIDITY = Duration.ofDays(2);

    public record User(String name, String salt, int iterations, String hash, Instant created, Instant lastLogin,
                       String codeHash, Instant codeExpires) {
        public boolean hasPassword() {
            return hash != null;
        }

        JSONObject toJson() {
            return new JSONObject().put("name", name).put("salt", salt == null ? JSONObject.NULL : salt)
                    .put("iterations", iterations).put("hash", hash == null ? JSONObject.NULL : hash)
                    .put("created", created.toString()).put("lastLogin", lastLogin == null ? JSONObject.NULL : lastLogin.toString())
                    .put("codeHash", codeHash == null ? JSONObject.NULL : codeHash)
                    .put("codeExpires", codeExpires == null ? JSONObject.NULL : codeExpires.toString());
        }

        static User fromJson(JSONObject json) {
            return new User(json.getString("name"), opt(json, "salt"), json.optInt("iterations", Secrets.ITERATIONS),
                    opt(json, "hash"), Instant.parse(json.getString("created")), instant(json, "lastLogin"),
                    opt(json, "codeHash"), instant(json, "codeExpires"));
        }
    }

    public record ApiToken(String name, String hash, Instant created, Instant lastUsed) {
        JSONObject toJson() {
            return new JSONObject().put("name", name).put("hash", hash).put("created", created.toString())
                    .put("lastUsed", lastUsed == null ? JSONObject.NULL : lastUsed.toString());
        }

        static ApiToken fromJson(JSONObject json) {
            return new ApiToken(json.getString("name"), json.getString("hash"), Instant.parse(json.getString("created")),
                    instant(json, "lastUsed"));
        }
    }

    private final Path file;
    private final Path codes;
    private final Map<String, User> users = new TreeMap<>();
    private final Map<String, ApiToken> tokens = new TreeMap<>();

    public UserStore(Path folder) throws IOException {
        this.file = folder.resolve("users.json");
        this.codes = folder.resolve("codes");
        Files.createDirectories(folder);
        if (Files.exists(this.file)) {
            JSONObject json = new JSONObject(Files.readString(this.file));
            JSONArray userArray = json.optJSONArray("users");
            for (int i = 0; userArray != null && i < userArray.length(); i++) {
                User user = User.fromJson(userArray.getJSONObject(i));
                this.users.put(user.name(), user);
            }
            JSONArray tokenArray = json.optJSONArray("tokens");
            for (int i = 0; tokenArray != null && i < tokenArray.length(); i++) {
                ApiToken token = ApiToken.fromJson(tokenArray.getJSONObject(i));
                this.tokens.put(token.name(), token);
            }
        }
    }

    public synchronized boolean isEmpty() {
        return this.users.isEmpty();
    }

    public synchronized List<User> users() {
        return new ArrayList<>(this.users.values());
    }

    public synchronized List<ApiToken> tokens() {
        return new ArrayList<>(this.tokens.values());
    }

    public synchronized Optional<User> find(String name) {
        return Optional.ofNullable(name == null ? null : this.users.get(normalize(name)));
    }

    /**
     * Check a password. Takes the same time for unknown users.
     */
    public Optional<User> verifyPassword(String name, String password) {
        User user = this.find(name).orElse(null);
        boolean ok = Secrets.verify(password == null ? "" : password, user == null ? null : user.salt(),
                user == null ? Secrets.ITERATIONS : user.iterations(), user == null ? null : user.hash());
        return ok ? Optional.of(user) : Optional.empty();
    }

    /**
     * Create a user without password and write a one-time setup code to {@code codes/<name>.txt}.
     *
     * @return the file with the code
     */
    public synchronized Path create(String name) throws IOException {
        String normalized = normalize(name);
        if (!NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid user name (a-z, 0-9, . _ -, up to 32 characters)");
        }
        if (this.users.containsKey(normalized)) {
            throw new IllegalArgumentException("User " + normalized + " exists already");
        }
        this.users.put(normalized, new User(normalized, null, Secrets.ITERATIONS, null, Instant.now(), null, null, null));
        return this.issueCode(normalized);
    }

    /**
     * New setup code for an existing user (forgotten password). The old password stays valid until the code is used.
     */
    public synchronized Path issueCode(String name) throws IOException {
        User user = this.require(name);
        String code = Secrets.code();
        this.users.put(user.name(), new User(user.name(), user.salt(), user.iterations(), user.hash(), user.created(),
                user.lastLogin(), Secrets.sha256(code), Instant.now().plus(CODE_VALIDITY)));
        this.save();
        Path target = this.codes.resolve(user.name() + ".txt");
        writePrivate(target, "Web login of STEM\n\nUser:  " + user.name() + "\nCode:  " + code + "\nValid: until "
                + Instant.now().plus(CODE_VALIDITY) + " (UTC)\n\nOpen the login page, choose \"Code einlösen\" and set a password.\n"
                + "Delete this file afterwards.\n");
        return target;
    }

    /**
     * Set the password with a setup code.
     */
    public synchronized User redeemCode(String name, String code, String password) throws IOException {
        User user = this.find(name).orElse(null);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        boolean valid = user != null && user.codeHash() != null && user.codeExpires() != null
                && user.codeExpires().isAfter(Instant.now()) && Secrets.equal(Secrets.sha256(normalizedCode), user.codeHash());
        if (!valid) {
            throw new IllegalArgumentException("Invalid or expired code");
        }
        User updated = this.withPassword(user, password);
        Files.deleteIfExists(this.codes.resolve(user.name() + ".txt"));
        return updated;
    }

    public synchronized User changePassword(User user, String password) throws IOException {
        return this.withPassword(this.require(user.name()), password);
    }

    private User withPassword(User user, String password) throws IOException {
        checkPassword(password, user.name());
        String salt = Secrets.token(16);
        User updated = new User(user.name(), salt, Secrets.ITERATIONS, Secrets.hash(password, salt, Secrets.ITERATIONS),
                user.created(), user.lastLogin(), null, null);
        this.users.put(user.name(), updated);
        this.save();
        return updated;
    }

    public synchronized void touchLogin(String name) {
        this.find(name).ifPresent(user -> {
            this.users.put(user.name(), new User(user.name(), user.salt(), user.iterations(), user.hash(), user.created(),
                    Instant.now(), user.codeHash(), user.codeExpires()));
            try {
                this.save();
            } catch (IOException ignored) {
                /* only the last login time */
            }
        });
    }

    public synchronized void remove(String name) throws IOException {
        User user = this.require(name);
        this.users.remove(user.name());
        Files.deleteIfExists(this.codes.resolve(user.name() + ".txt"));
        this.save();
    }

    /* ------------------------------------------------------------------ API tokens */

    /**
     * Create an API token (for scripts and devices, header {@code Authorization: Bearer <token>}) and write it to
     * {@code codes/token-<name>.txt}.
     */
    public synchronized Path createToken(String name) throws IOException {
        String normalized = normalize(name);
        if (!NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid token name (a-z, 0-9, . _ -, up to 32 characters)");
        }
        if (this.tokens.containsKey(normalized)) {
            throw new IllegalArgumentException("Token " + normalized + " exists already");
        }
        String token = "stem_" + Secrets.token(32);
        this.tokens.put(normalized, new ApiToken(normalized, Secrets.sha256(token), Instant.now(), null));
        this.save();
        Path target = this.codes.resolve("token-" + normalized + ".txt");
        writePrivate(target, "API token of STEM web-api\n\nName:   " + normalized + "\nToken:  " + token
                + "\n\nSend it as HTTP header:  Authorization: Bearer " + token + "\nDelete this file after copying the token.\n");
        return target;
    }

    public synchronized Optional<ApiToken> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = Secrets.sha256(token.trim());
        for (ApiToken candidate : this.tokens.values()) {
            if (Secrets.equal(candidate.hash(), hash)) {
                ApiToken used = new ApiToken(candidate.name(), candidate.hash(), candidate.created(), Instant.now());
                /* the last use is saved at most once per hour */
                if (candidate.lastUsed() == null || candidate.lastUsed().isBefore(Instant.now().minus(Duration.ofHours(1)))) {
                    this.tokens.put(candidate.name(), used);
                    try {
                        this.save();
                    } catch (IOException ignored) {
                        /* only the last use */
                    }
                }
                return Optional.of(used);
            }
        }
        return Optional.empty();
    }

    public synchronized void removeToken(String name) throws IOException {
        String normalized = normalize(name);
        if (this.tokens.remove(normalized) == null) {
            throw new IllegalArgumentException("Unknown token " + normalized);
        }
        Files.deleteIfExists(this.codes.resolve("token-" + normalized + ".txt"));
        this.save();
    }

    /* ------------------------------------------------------------------ helpers */

    public static void checkPassword(String password, String name) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("The password needs at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > 200) {
            throw new IllegalArgumentException("The password is too long");
        }
        if (password.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("The password must not be the user name");
        }
    }

    private User require(String name) {
        return this.find(name).orElseThrow(() -> new IllegalArgumentException("Unknown user " + name));
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void save() throws IOException {
        JSONArray userArray = new JSONArray();
        this.users.values().forEach(user -> userArray.put(user.toJson()));
        JSONArray tokenArray = new JSONArray();
        this.tokens.values().forEach(token -> tokenArray.put(token.toJson()));
        writePrivate(this.file, new JSONObject().put("users", userArray).put("tokens", tokenArray).toString(2));
    }

    static void writePrivate(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        restrict(target.getParent(), "rwx------");
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        restrict(temp, "rw-------");
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void restrict(Path path, String permissions) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (UnsupportedOperationException | IOException ignored) {
            /* not a POSIX file system */
        }
    }

    private static String opt(JSONObject json, String key) {
        Object value = json.opt(key);
        return value == null || value == JSONObject.NULL ? null : String.valueOf(value);
    }

    private static Instant instant(JSONObject json, String key) {
        String value = opt(json, key);
        return value == null ? null : Instant.parse(value);
    }
}
