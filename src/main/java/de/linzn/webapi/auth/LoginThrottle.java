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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing. Failed attempts are counted per client address and per user name; after
 * {@link #FREE_ATTEMPTS} failures within {@link #WINDOW} every further attempt is locked for 30 s, doubling up to
 * 15 minutes. A successful login resets the counters.
 */
public final class LoginThrottle {

    static final int FREE_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration FIRST_LOCK = Duration.ofSeconds(30);
    private static final Duration MAX_LOCK = Duration.ofMinutes(15);
    private static final int MAX_KEYS = 10_000;

    private record Failures(int count, Instant first, Instant lockedUntil) {
    }

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    /**
     * @return seconds until the next attempt is allowed, 0 if allowed now
     */
    public long waitSeconds(String address, String user) {
        Instant now = Instant.now();
        long wait = 0;
        for (String key : new String[]{"ip:" + address, "user:" + user}) {
            Failures entry = this.failures.get(key);
            if (entry != null && entry.lockedUntil() != null && entry.lockedUntil().isAfter(now)) {
                wait = Math.max(wait, Duration.between(now, entry.lockedUntil()).toSeconds() + 1);
            }
        }
        return wait;
    }

    public void failed(String address, String user) {
        if (this.failures.size() > MAX_KEYS) {
            this.cleanup();
        }
        Instant now = Instant.now();
        for (String key : new String[]{"ip:" + address, "user:" + user}) {
            this.failures.compute(key, (k, entry) -> {
                if (entry == null || entry.first().plus(WINDOW).isBefore(now) && (entry.lockedUntil() == null || entry.lockedUntil().isBefore(now))) {
                    entry = new Failures(0, now, null);
                }
                int count = entry.count() + 1;
                Instant locked = null;
                if (count >= FREE_ATTEMPTS) {
                    long seconds = Math.min(MAX_LOCK.toSeconds(), FIRST_LOCK.toSeconds() << Math.min(10, count - FREE_ATTEMPTS));
                    locked = now.plusSeconds(seconds);
                }
                return new Failures(count, entry.first(), locked);
            });
        }
    }

    public void succeeded(String address, String user) {
        this.failures.remove("ip:" + address);
        this.failures.remove("user:" + user);
    }

    private void cleanup() {
        Instant limit = Instant.now().minus(WINDOW).minus(MAX_LOCK);
        this.failures.values().removeIf(entry -> entry.first().isBefore(limit));
    }
}
