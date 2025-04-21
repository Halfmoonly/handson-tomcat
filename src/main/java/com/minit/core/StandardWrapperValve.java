package com.minit.core;

import com.minit.Request;
import com.minit.Response;
import com.minit.ValveContext;
import com.minit.connector.HttpRequestFacade;
import com.minit.connector.HttpResponseFacade;
import com.minit.connector.http.HttpRequestImpl;
import com.minit.connector.http.HttpResponseImpl;
import com.minit.valves.ValveBase;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StandardWrapperValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {
        // TODO Auto-generated method stub
        System.out.println("StandardWrapperValve invoke()");

        HttpServletRequest requestFacade = new HttpRequestFacade((HttpRequestImpl) request);
        HttpServletResponse responseFacade = new HttpResponseFacade((HttpResponseImpl) response);

        Servlet instance = ((StandardWrapper)getContainer()).getServlet();
        if (instance != null) {
            instance.service(requestFacade, responseFacade);
        }
    }
}
