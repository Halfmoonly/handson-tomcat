package com.minit;

import javax.servlet.ServletException;
import java.io.IOException;

public interface Container {
    public static final String ADD_CHILD_EVENT = "addChild";
    public static final String REMOVE_CHILD_EVENT = "removeChild";

    public Loader getLoader();
    public void setLoader(Loader loader);
    public String getName();
    public void setName(String name);
    public Container getParent();
    public void setParent(Container container);
    public void addChild(Container child);
    public Container findChild(String name);
    public Container[] findChildren();
    public void invoke(Request request, Response response)
            throws IOException, ServletException;
    public void removeChild(Container child);
    public Logger getLogger();

    public void setLogger(Logger logger);
}
