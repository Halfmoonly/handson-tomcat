package com.minit;

import javax.servlet.ServletException;
import java.io.IOException;

public interface Valve {
    public String getInfo();
    public Container getContainer();
    public void setContainer(Container container);
    public void invoke(Request request, Response response,ValveContext context)
            throws IOException, ServletException;
}