package server;

import org.apache.commons.lang3.text.StrSubstitutor;

import javax.servlet.Servlet;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ServletProcessor {

    public void process(HttpRequest request, HttpResponse response) {
        String uri = request.getUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);

        response.setCharacterEncoding("UTF-8");

        Class<?> servletClass = null;
        try {
            servletClass = HttpConnector.loader.loadClass(servletName);
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }

        Servlet servlet = null;
        try {
            servlet = (Servlet) servletClass.newInstance();
            HttpRequestFacade requestFacade = new HttpRequestFacade(request);
            HttpResponseFacade responseFacade = new HttpResponseFacade(response);
            System.out.println("Call Service()");
            servlet.service(requestFacade, responseFacade);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        catch (Throwable e) {
            System.out.println(e.toString());
        }
    }

}
