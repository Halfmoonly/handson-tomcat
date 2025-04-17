package server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServletContainer {
    HttpConnector connector = null;
    ClassLoader loader = null;
    Map<String,String> servletClsMap = new ConcurrentHashMap<>(); //servletName - ServletClassName
    Map<String,ServletWrapper> servletInstanceMap = new ConcurrentHashMap<>();//servletName - servlet

    public ServletContainer() {
        try {
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(HttpServer.WEB_ROOT);
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            System.out.println(e.toString() );
        }
    }
    public String getInfo() {
        return null;
    }

    public ClassLoader getLoader(){
        return this.loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public HttpConnector getConnector() {
        return connector;
    }
    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public String getName() {
        return null;
    }

    public void setName(String name) {
    }

    public void invoke(HttpRequest request, HttpResponse response)
            throws IOException, ServletException {
        ServletWrapper servletWrapper = null;
        String uri = request.getUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        String servletClassName = servletName;

        servletWrapper = servletInstanceMap.get(servletName);
        if ( servletWrapper == null) {
            servletWrapper = new ServletWrapper(servletClassName,this);
            //servletWrapper.setParent(this);

            this.servletClsMap.put(servletName, servletClassName);
            this.servletInstanceMap.put(servletName, servletWrapper);
        }

        try {
            HttpServletRequest requestFacade = new HttpRequestFacade(request);
            HttpServletResponse responseFacade = new HttpResponseFacade(response);
            System.out.println("Call service()");

            servletWrapper.invoke(requestFacade, responseFacade);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        catch (Throwable e) {
            System.out.println(e.toString());
        }
    }
}
