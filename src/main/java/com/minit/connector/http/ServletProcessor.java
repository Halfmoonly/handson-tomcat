package com.minit.connector.http;

import javax.servlet.ServletException;
import java.io.IOException;

public class ServletProcessor {
    private HttpConnector connector;

    public ServletProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    public void process(HttpRequestImpl request, HttpResponseImpl response) throws IOException, ServletException {
        this.connector.getContainer().invoke(request, response);
    }

}
