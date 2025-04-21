package com.minit.connector.http;

import com.minit.Request;
import com.minit.Response;

import javax.servlet.ServletException;
import java.io.IOException;

public class ServletProcessor {
    private HttpConnector connector;

    public ServletProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    public void process(Request request, Response response) throws IOException, ServletException {
        System.out.println("ServletProcessor process()");
        this.connector.getContainer().invoke(request, response);
    }

}
