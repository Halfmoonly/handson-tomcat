package com.minit.startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLClassLoader;

import com.minit.Loader;
import com.minit.Logger;
import com.minit.connector.http.HttpConnector;
import com.minit.core.*;
import com.minit.logger.FileLogger;

public class BootStrap {
    public static final String MINIT_HOME = System.getProperty("user.dir");
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webapps";
    public static final int PORT = 8080;
    private static int debug = 0;

    public static void main(String[] args) {
        if (debug >= 1)
            log(".... startup ....");

        System.setProperty("minit.home", MINIT_HOME);
        System.setProperty("minit.base", WEB_ROOT);

        HttpConnector connector = new HttpConnector();
        StandardHost container = new StandardHost();

        Loader loader = new CommonLoader();
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