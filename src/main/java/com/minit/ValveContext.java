package com.minit;

import javax.servlet.ServletException;
import java.io.IOException;

public interface ValveContext {
    public String getInfo();
    public void invokeNext(Request request, Response response) throws IOException, ServletException;
}