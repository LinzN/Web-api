package de.linzn.webapi.handler;

import de.linzn.webapi.WebApiPlugin;

import java.util.ArrayList;
import java.util.List;

public class CallModule {

    private final String moduleName;
    private final List<SubCallHandler> subCallHandlers;

    public CallModule(String moduleName) {
        this.moduleName = moduleName;
        this.subCallHandlers = new ArrayList<>();
    }

    public void registerSubCallHandler(SubCallHandler subCallHandler) {
        this.subCallHandlers.add(subCallHandler);
        WebApiPlugin.webApiPlugin.getWebServer().registerContextSub(subCallHandler);
    }

    public void unregisterSubCallHandler(SubCallHandler subCallHandler) {
        this.subCallHandlers.remove(subCallHandler);
        WebApiPlugin.webApiPlugin.getWebServer().unregisterContextSub(subCallHandler);

    }


    public String getModuleName() {
        return moduleName;
    }
}
