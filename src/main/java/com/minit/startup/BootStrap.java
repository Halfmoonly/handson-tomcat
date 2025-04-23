package com.minit.startup;

import com.minit.connector.http.HttpConnector;
import com.minit.core.StandardHost;
import com.minit.core.WebappClassLoader;

import java.io.File;

public class BootStrap {
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webroot";
    private static int debug = 0;

    public static void main(String[] args) {
        if (debug >= 1)
            log(".... startup ....");

        System.setProperty("minit.base", WEB_ROOT);

        HttpConnector connector = new HttpConnector();
        StandardHost container = new StandardHost();

        WebappClassLoader loader = new WebappClassLoader();
        container.setLoader(loader);
        loader.start();

        connector.setContainer(container);
        container.setConnector(connector);

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