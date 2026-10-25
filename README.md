![MirraNET Logo](https://raw.githubusercontent.com/LinzN/Stem/master/stem-core/src/main/resources/header-logo.png)

# web-api

> Embedded JSON web server for [STEM](https://github.com/LinzN/Stem) and the base for all web endpoints of other plugins.

web-api starts a small HTTP server inside STEM. It offers endpoints with the state of STEM itself
(status, plugins, health checks, log output, ...) and lets other plugins register their own endpoints.
Dashboards read their data from here.

---

## Features

- HTTP server on virtual threads, one thread per request, no thread pool to tune
- Module system: `/<module>/<sub-path>` is routed to a handler of the owning plugin
- CORS headers on every response, `OPTIONS` preflight requests are answered automatically
- Request bodies up to 1 MB, parsed as JSON object, JSON array or plain text
- Default endpoints for the STEM system state and an emergency restart

---

## Requirements

| Requirement | Version                                                  |
|-------------|----------------------------------------------------------|
| STEM        | 1.2.0 or newer (full `stem` dependency, uses core-only features) |
| Plugins     | [system-chain](https://github.com/LinzN/system-chain)     |

---

## Installation

1. Build the plugin with `mvn clean package` (or take the build from Jenkins).
2. Copy `target/web-api.jar` into the `plugins` folder of STEM.
3. Start STEM once to create `plugins/web-api/config.yml`.
4. Adjust the configuration and restart STEM (plugins are only loaded at startup).

---

## Configuration (`config.yml`)

| Key               | Default   | Description                                                                 |
|-------------------|-----------|-----------------------------------------------------------------------------|
| `server.hostname` | `0.0.0.0` | Address the server binds to                                                 |
| `server.port`     | `8082`    | Port of the server                                                          |
| `emergency.token` | *(empty)* | Optional shared secret for `/emergency/reboot`; empty = local network only |

---

## Response format

Every response is HTTP `200` with this envelope:

```json
{
  "request": "/stemsystem/status",
  "payload": { "...": "data of the handler" },
  "error": 0
}
```

`error` is `404` if no handler exists for the path and `500` if the handler failed. Handlers that use
`ApiResponse` add `apiError` (boolean) and `errorDescription` to the payload.

---

## Default endpoints

| Path                               | Description                                                               |
|------------------------------------|---------------------------------------------------------------------------|
| `/stemsystem/steminfo`             | STEM version and system name                                              |
| `/stemsystem/status`               | CPU and memory load, disk space and network ping (from system-chain)      |
| `/stemsystem/console`              | Latest log entries as text and HTML                                       |
| `/stemsystem/notifications`        | Recent notifications                                                      |
| `/stemsystem/informationblocks`    | Active and archived information blocks                                    |
| `/stemsystem/pluginsinfo`          | Loaded plugins with version and build number                              |
| `/stemsystem/taskinfo`             | Number of running background tasks                                        |
| `/stemsystem/healthcheckinfo`      | Results of all health checks, grouped by level                            |
| `/stemsystem/compliancecheck`      | Status of the compliance checks                                           |
| `/emergency/reboot`                | Restarts STEM (see below)                                                 |
| `/testapi/mirror`                  | Returns the received body and query parameters (for testing)              |

### Emergency restart

`POST /emergency/reboot` with `{"requestAction": "WRITE"}` restarts STEM.

- Only accepted from loopback, private (`10.x`, `172.16-31.x`, `192.168.x`) and link-local addresses
- If `emergency.token` is set, the body must also contain `"token": "<secret>"`
- Every request is logged with the remote address (and `X-Forwarded-For`, if present)

Note: behind a reverse proxy on the same network the proxy address is checked, not the client address.

---

## Adding endpoints from another plugin

```java
WebModule module = new WebModule("myplugin");
module.registerSubCallHandler(new MyRequest(), "data");   // -> /myplugin/data
WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(module);

// in onDisable(), so no request reaches the plugin after it was disabled
WebApiPlugin.webApiPlugin.getWebServer().disableCallModule(module);
```

```java
public class MyRequest extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange exchange, HttpRequestClientPayload payload) throws IOException {
        JSONObject body = payload.getPostJson();          // empty object if the body is no JSON object
        Map<String, String> query = payload.getQueryData();
        ApiResponse response = new ApiResponse();
        response.getJSONObject().put("value", 42);
        return response.buildResponse();
    }
}
```

The handler registered with the path `$_root` answers requests to `/<module>` without sub-path.
Add `web-api` as `provided` dependency and to `depend` in `plugin.yml`.

---

## License

Copyright (c) MirraNET, Niklas Linz. Licensed under the
[GNU Lesser General Public License v3.0](https://www.gnu.org/licenses/lgpl-3.0.html).
