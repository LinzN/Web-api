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

package de.linzn.webapi;

import de.linzn.stem.modules.commandModule.ICommand;
import de.linzn.webapi.auth.AuthService;
import de.linzn.webapi.auth.SessionStore;
import de.linzn.webapi.auth.UserStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * STEM command for the web login. Secrets (setup codes, API tokens) are written to files, never to the log:
 * the STEM log can be read through the web API and by Mirra.
 * <pre>
 * webapi users                      users with last login
 * webapi user add &lt;name&gt;           new user, setup code in plugins/web-api/codes/&lt;name&gt;.txt
 * webapi user reset &lt;name&gt;         new setup code (forgotten password)
 * webapi user remove &lt;name&gt;        delete the user and end its sessions
 * webapi sessions [name]            active logins
 * webapi logout &lt;name|all&gt;         end the logins of a user or of everybody
 * webapi tokens                     API tokens
 * webapi token add &lt;name&gt;          new API token in plugins/web-api/codes/token-&lt;name&gt;.txt
 * webapi token remove &lt;name&gt;       delete an API token
 * </pre>
 */
public class WebApiCommand implements ICommand {

    private final WebApiPlugin plugin;

    public WebApiCommand(WebApiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean executeTerminal(String[] args) {
        AuthService auth = this.plugin.getAuth();
        if (auth == null) {
            this.plugin.getPluginLogger().warning("Web login not available");
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        try {
            String what = args[0].toLowerCase(Locale.ROOT);
            String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
            switch (what) {
                case "users" -> {
                    List<UserStore.User> users = auth.users().users();
                    this.info(users.size() + " user(s)");
                    for (UserStore.User user : users) {
                        this.info("  " + user.name() + (user.hasPassword() ? "" : " (no password yet)")
                                + (user.codeHash() != null ? ", setup code open until " + user.codeExpires() : "")
                                + ", last login " + (user.lastLogin() == null ? "never" : user.lastLogin()));
                    }
                }
                case "user" -> {
                    if (args.length < 3) {
                        return false;
                    }
                    String name = args[2];
                    switch (action) {
                        case "add" -> this.codeFile(auth.users().create(name), "Created user " + name.toLowerCase(Locale.ROOT));
                        case "reset" -> this.codeFile(auth.users().issueCode(name), "New setup code for " + name.toLowerCase(Locale.ROOT));
                        case "remove" -> {
                            auth.users().remove(name);
                            int ended = auth.sessions().removeUser(name.toLowerCase(Locale.ROOT), null);
                            this.info("Removed user " + name + ", " + ended + " session(s) ended");
                        }
                        default -> {
                            return false;
                        }
                    }
                }
                case "sessions" -> {
                    List<SessionStore.Session> sessions = auth.sessions().list();
                    for (SessionStore.Session session : sessions) {
                        if (args.length > 1 && !session.user().equals(args[1].toLowerCase(Locale.ROOT))) {
                            continue;
                        }
                        this.info("  " + session.user() + " from " + session.address() + ", since " + session.created()
                                + ", last seen " + session.lastSeen() + (session.remember() ? ", stays logged in" : "")
                                + " - " + session.agent());
                    }
                    this.info(sessions.size() + " session(s)");
                }
                case "logout" -> {
                    if (args.length < 2) {
                        return false;
                    }
                    int ended = args[1].equalsIgnoreCase("all") ? auth.sessions().removeAll()
                            : auth.sessions().removeUser(args[1].toLowerCase(Locale.ROOT), null);
                    this.info(ended + " session(s) ended");
                }
                case "tokens" -> {
                    for (UserStore.ApiToken token : auth.users().tokens()) {
                        this.info("  " + token.name() + ", created " + token.created() + ", last used "
                                + (token.lastUsed() == null ? "never" : token.lastUsed()));
                    }
                }
                case "token" -> {
                    if (args.length < 3) {
                        return false;
                    }
                    switch (action) {
                        case "add" -> this.codeFile(auth.users().createToken(args[2]), "Created API token " + args[2].toLowerCase(Locale.ROOT));
                        case "remove" -> {
                            auth.users().removeToken(args[2]);
                            this.info("Removed API token " + args[2]);
                        }
                        default -> {
                            return false;
                        }
                    }
                }
                default -> {
                    return false;
                }
            }
        } catch (IllegalArgumentException e) {
            this.plugin.getPluginLogger().warning("webapi: " + e.getMessage());
        } catch (Exception e) {
            this.plugin.getPluginLogger().error("webapi " + String.join(" ", args) + " failed", e);
        }
        return true;
    }

    private void codeFile(Path file, String what) {
        this.info(what + ". The secret is in " + file.toAbsolutePath() + " (not shown here, the log can be read through the web API).");
    }

    private void info(String text) {
        this.plugin.getPluginLogger().info(text);
    }
}
