package de.linzn.webapi.defaultWebModules;

import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.defaultWebModules.stemsystem.ApiTest;
import de.linzn.webapi.defaultWebModules.stemsystem.ConsoleOutput;
import de.linzn.webapi.defaultWebModules.stemsystem.Notifications;
import de.linzn.webapi.defaultWebModules.stemsystem.SystemStatus;
import de.linzn.webapi.handler.WebModule;

public class HandlerSetup {
    WebModule stemWebModule;

    public HandlerSetup() {
        this.setupStemHandler();
    }

    private void setupStemHandler() {
        this.stemWebModule = new WebModule("stemsystem");
        this.stemWebModule.registerSubCallHandler(new SystemStatus(), "status");
        this.stemWebModule.registerSubCallHandler(new ConsoleOutput(), "console");
        this.stemWebModule.registerSubCallHandler(new Notifications(), "notifications");
        this.stemWebModule.registerSubCallHandler(new ApiTest(), "apitest");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(stemWebModule);
    }
}
