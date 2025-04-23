package com.minit.core;

import com.minit.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

public class StandardWrapper extends ContainerBase implements Wrapper {
    private Servlet instance = null;
    private String servletClass;

    public StandardWrapper(String servletClass, StandardContext parent) {
        super();
        pipeline.setBasic(new StandardWrapperValve());

        this.parent = parent;
        this.servletClass = servletClass;
        try {
            loadServlet();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    public String getServletClass() {
        return servletClass;
    }
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }
    public Servlet getServlet(){
        return this.instance;
    }
    public Servlet loadServlet() throws ServletException {
        if (instance!=null)
            return instance;

        Servlet servlet = null;
        String actualClass = servletClass;
        if (actualClass == null) {
            throw new ServletException("servlet class has not been specified");
        }

        Loader classLoader = getLoader();

        // Load the specified servlet class from the appropriate class loader
        Class classClass = null;
        try {
            if (classLoader!=null) {
                classClass = classLoader.getClassLoader().loadClass(actualClass);
            }
        }
        catch (ClassNotFoundException e) {
            throw new ServletException("Servlet class not found");
        }
        // Instantiate and initialize an instance of the servlet class itself
        try {
            servlet = (Servlet) classClass.newInstance();
        }
        catch (Throwable e) {
            throw new ServletException("Failed to instantiate servlet");
        }

        // Call the initialization method of this servlet
        try {
            servlet.init(null);
        }
        catch (Throwable f) {
            throw new ServletException("Failed initialize servlet.");
        }
        instance =servlet;
        return servlet;
    }

    public void invoke(Request request, Response response)
            throws IOException, ServletException {
        System.out.println("StandardWrapper invoke()");

        super.invoke(request, response);
    }
    @Override
    public String getInfo() {
        return "Minit Servlet Wrapper, version 0.1";
    }

    public void addChild(Container child) {}
    public Container findChild(String name) {return null;}
    public Container[] findChildren() {return null;}
    public void removeChild(Container child) {}
    @Override
    public int getLoadOnStartup() {
        return 0;
    }

    @Override
    public void setLoadOnStartup(int value) {
    }

    @Override
    public void addInitParameter(String name, String value) {
    }

    @Override
    public Servlet allocate() throws ServletException {
        return null;
    }

    @Override
    public String findInitParameter(String name) {
        return null;
    }

    @Override
    public String[] findInitParameters() {
        return null;
    }

    @Override
    public void load() throws ServletException {
    }

    @Override
    public void removeInitParameter(String name) {
    }
}
