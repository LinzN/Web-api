/*
 *  Copyright (C) 2021. Niklas Linz - All Rights Reserved
 *  You may use, distribute and modify this code under the
 *  terms of the LGPLv3 license, which unfortunately won't be
 *  written for another century.
 *
 *  You should have received a copy of the LGPLv3 license with
 *  this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.webapi.defaultWebModules;

import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.defaultWebModules.testapi.Mirror;
import de.linzn.webapi.defaultWebModules.stemsystem.ConsoleOutput;
import de.linzn.webapi.defaultWebModules.stemsystem.Notifications;
import de.linzn.webapi.defaultWebModules.stemsystem.SystemStatus;
import de.linzn.webapi.modules.WebModule;

public class DefaultHandlerSetup {

    public DefaultHandlerSetup() {
        this.setupTestApiHandler();
        this.setupStemHandler();
    }


    private void setupTestApiHandler(){
        WebModule testApiModule = new WebModule("testapi");
        testApiModule.registerSubCallHandler(new Mirror(), "mirror");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(testApiModule);
    }
    private void setupStemHandler() {
        WebModule stemWebModule = new WebModule("stemsystem");
        stemWebModule.registerSubCallHandler(new SystemStatus(), "status");
        stemWebModule.registerSubCallHandler(new ConsoleOutput(), "console");
        stemWebModule.registerSubCallHandler(new Notifications(), "notifications");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(stemWebModule);
    }
}
