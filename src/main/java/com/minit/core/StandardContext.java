package com.minit.core;

import com.minit.Context;
import com.minit.Request;
import com.minit.Response;
import com.minit.Wrapper;
import com.minit.connector.http.HttpConnector;
import com.minit.startup.BootStrap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardContext extends ContainerBase implements Context{
    HttpConnector connector = null;
    Map<String,String> servletClsMap = new ConcurrentHashMap<>(); //servletName - ServletClassName
    Map<String,StandardWrapper> servletInstanceMap = new ConcurrentHashMap<>();//servletName - servletWrapper

    public StandardContext() {
        super();
        pipeline.setBasic(new StandardContextValve());

        try {
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(BootStrap.WEB_ROOT);
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            System.out.println(e.toString() );
        }
        log("Container created.");
    }
    public String getInfo() {
        return "Minit Servlet Context, vesion 0.1";
    }

    public HttpConnector getConnector() {
        return connector;
    }
    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public void invoke(Request request, Response response)
            throws IOException, ServletException {
        System.out.println("StandardContext invoke()");

        super.invoke(request, response);
    }
    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void setDisplayName(String displayName) {
        // TODO Auto-generated method stub

    }
    @Override
    public String getDocBase() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void setDocBase(String docBase) {
        // TODO Auto-generated method stub

    }
    @Override
    public String getPath() {
        return null;
    }
    @Override
    public void setPath(String path) {

    }
    @Override
    public ServletContext getServletContext() {
        return null;
    }
    @Override
    public int getSessionTimeout() {
        return 0;
    }
    @Override
    public void setSessionTimeout(int timeout) {
    }
    @Override
    public String getWrapperClass() {
        return null;
    }
    @Override
    public void setWrapperClass(String wrapperClass) {
    }
    @Override
    public Wrapper createWrapper() {
        return null;
    }
    public Wrapper getWrapper(String name){
        StandardWrapper servletWrapper = servletInstanceMap.get(name);
        if ( servletWrapper == null) {
            String servletClassName = name;
            servletWrapper = new StandardWrapper(servletClassName,this);
            this.servletClsMap.put(name, servletClassName);
            this.servletInstanceMap.put(name, servletWrapper);
        }
        return servletWrapper;
    }
    @Override
    public String findServletMapping(String pattern) {
        return null;
    }
    @Override
    public String[] findServletMappings() {
        return null;
    }
    @Override
    public void reload() {
    }
}
