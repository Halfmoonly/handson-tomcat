package com.minit.startup;

import com.minit.Logger;
import com.minit.connector.http.HttpConnector;
import com.minit.core.ContainerListenerDef;
import com.minit.core.FilterDef;
import com.minit.core.FilterMap;
import com.minit.core.StandardContext;
import com.minit.logger.FileLogger;

import java.io.File;

public class BootStrap {
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webroot";
    private static int debug = 0;

    public static void main(String[] args) {
        if (debug >= 1)
            log(".... startup ....");
        HttpConnector connector = new HttpConnector();
        StandardContext container = new StandardContext();
        connector.setContainer(container);
        container.setConnector(connector);

        Logger logger = new FileLogger();
        container.setLogger(logger);

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("TestFilter");
        filterDef.setFilterClass("test.TestFilter");
        container.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("TestFilter");
        filterMap.setURLPattern("/*");
        container.addFilterMap(filterMap);

        container.filterStart();

        ContainerListenerDef listenerDef = new ContainerListenerDef();
        listenerDef.setListenerName("TestListener");
        listenerDef.setListenerClass("test.TestListener");
        container.addListenerDef(listenerDef);
        container.listenerStart();

        container.start();

        connector.start();
    }
    private static void log(String message) {
        System.out.print("Bootstrap: ");
        System.out.println(message);
    }

    private static void log(String message, Throwable exception) {
        log(message);
        exception.printStackTrace(System.out);

    }
}