package com.minit;

import javax.servlet.ServletException;
import java.io.IOException;

public interface Pipeline {
    public Valve getBasic();
    public void setBasic(Valve valve);
    public void addValve(Valve valve);
    public Valve[] getValves();
    public void invoke(Request request, Response response) throws IOException, ServletException;
    public void removeValve(Valve valve);
}
