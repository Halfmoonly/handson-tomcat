package com.minit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Container {
    public static final String ADD_CHILD_EVENT = "addChild";
    public static final String REMOVE_CHILD_EVENT = "removeChild";
    public String getInfo();
    public ClassLoader getLoader();
    public void setLoader(ClassLoader loader);
    public String getName();
    public void setName(String name);
    public Container getParent();
    public void setParent(Container container);
    public void addChild(Container child);
    public Container findChild(String name);
    public Container[] findChildren();
    public void invoke(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;
    public void removeChild(Container child);
}
