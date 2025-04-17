package com.minit.startup;

import com.minit.connector.http.HttpConnector;
import com.minit.core.StandardContext;

import java.io.File;

public class Bootstrap {
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webroot";

    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        StandardContext container = new StandardContext();
        connector.setContainer(container);
        container.setConnector(connector);
        connector.start();
    }

}