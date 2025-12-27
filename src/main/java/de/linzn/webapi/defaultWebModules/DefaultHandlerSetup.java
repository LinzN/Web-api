/*
 * Copyright (c) 2025 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.linzn.webapi.defaultWebModules;

import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.defaultWebModules.emergency.Reboot;
import de.linzn.webapi.defaultWebModules.stemsystem.*;
import de.linzn.webapi.defaultWebModules.testapi.Mirror;
import de.linzn.webapi.modules.WebModule;

public class DefaultHandlerSetup {

    public DefaultHandlerSetup() {
        this.setupEmergencyHandler();
        this.setupTestApiHandler();
        this.setupStemHandler();
    }

    private void setupEmergencyHandler() {
        WebModule emergencyModule = new WebModule("emergency");
        emergencyModule.registerSubCallHandler(new Reboot(), "reboot");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(emergencyModule);
    }

    private void setupTestApiHandler() {
        WebModule testApiModule = new WebModule("testapi");
        testApiModule.registerSubCallHandler(new Mirror(), "mirror");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(testApiModule);
    }

    private void setupStemHandler() {
        WebModule stemWebModule = new WebModule("stemsystem");
        stemWebModule.registerSubCallHandler(new StemInfo(), "steminfo");
        stemWebModule.registerSubCallHandler(new SystemStatus(), "status");
        stemWebModule.registerSubCallHandler(new ConsoleOutput(), "console");
        stemWebModule.registerSubCallHandler(new Notifications(), "notifications");
        stemWebModule.registerSubCallHandler(new InformationBlocks(), "informationblocks");
        stemWebModule.registerSubCallHandler(new PluginsInfo(), "pluginsinfo");
        stemWebModule.registerSubCallHandler(new TaskInfo(), "taskinfo");
        stemWebModule.registerSubCallHandler(new HealthCheckInfo(), "healthcheckinfo");
        stemWebModule.registerSubCallHandler(new ComplianceChecks(), "compliancecheck");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(stemWebModule);
    }

}
