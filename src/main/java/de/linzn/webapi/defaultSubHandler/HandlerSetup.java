package de.linzn.webapi.defaultSubHandler;

import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.defaultSubHandler.stemsystem.ConsoleOutput;
import de.linzn.webapi.defaultSubHandler.stemsystem.Notifications;
import de.linzn.webapi.defaultSubHandler.stemsystem.SystemStatus;
import de.linzn.webapi.handler.CallModule;

public class HandlerSetup {
    CallModule stemCallModule;

    public HandlerSetup() {
        this.setupStemHandler();
    }

    private void setupStemHandler() {
        this.stemCallModule = new CallModule("stemsystem");
        this.stemCallModule.registerSubCallHandler(new SystemStatus(), "status");
        this.stemCallModule.registerSubCallHandler(new ConsoleOutput(), "console");
        this.stemCallModule.registerSubCallHandler(new Notifications(), "notifications");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(stemCallModule);
    }
}
