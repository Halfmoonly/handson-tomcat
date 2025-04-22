package com.minit.core;

import com.minit.*;
import com.minit.connector.http.HttpConnector;
import com.minit.startup.BootStrap;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardContext extends ContainerBase implements Context{
    HttpConnector connector = null;
    Map<String,String> servletClsMap = new ConcurrentHashMap<>(); //servletName - ServletClassName
    Map<String,StandardWrapper> servletInstanceMap = new ConcurrentHashMap<>();//servletName - servletWrapper

    private Map<String,ApplicationFilterConfig> filterConfigs = new ConcurrentHashMap<>();
    private Map<String,FilterDef> filterDefs = new ConcurrentHashMap<>();
    private FilterMap filterMaps[] = new FilterMap[0];

    private ArrayList<ContainerListenerDef> listenerDefs = new ArrayList<>();
    private ArrayList<ContainerListener> listeners = new ArrayList<>();

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

    public void start(){
        fireContainerEvent("Container Started",this);
    }

    public void addContainerListener(ContainerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public void removeContainerListener(ContainerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    public void fireContainerEvent(String type, Object data) {
        if (listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners) {
            list = (ContainerListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((ContainerListener) list[i]).containerEvent(event);

    }
    public void addListenerDef(ContainerListenerDef listenererDef) {
        synchronized (listenerDefs) {
            listenerDefs.add(listenererDef);
        }
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

    public void addFilterDef(FilterDef filterDef) {
        filterDefs.put(filterDef.getFilterName(), filterDef);
    }

    public void addFilterMap(FilterMap filterMap) {
        // Validate the proposed filter mapping
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getURLPattern();
        if (findFilterDef(filterName) == null)
            throw new IllegalArgumentException("standardContext.filterMap.name"+filterName);
        if ((servletName == null) && (urlPattern == null))
            throw new IllegalArgumentException("standardContext.filterMap.either");
        if ((servletName != null) && (urlPattern != null))
            throw new IllegalArgumentException("standardContext.filterMap.either");
        // Because filter-pattern is new in 2.3, no need to adjust
        // for 2.2 backwards compatibility
        if ((urlPattern != null) && !validateURLPattern(urlPattern))
            throw new IllegalArgumentException("standardContext.filterMap.pattern"+urlPattern);

        // Add this filter mapping to our registered set
        synchronized (filterMaps) {
            FilterMap results[] =new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, results, 0, filterMaps.length);
            results[filterMaps.length] = filterMap;
            filterMaps = results;
        }
    }

    public FilterDef findFilterDef(String filterName) {
        return ((FilterDef) filterDefs.get(filterName));
    }
    public FilterDef[] findFilterDefs() {
        synchronized (filterDefs) {
            FilterDef results[] = new FilterDef[filterDefs.size()];
            return ((FilterDef[]) filterDefs.values().toArray(results));
        }
    }
    public FilterMap[] findFilterMaps() {
        return (filterMaps);
    }
    public void removeFilterDef(FilterDef filterDef) {
        filterDefs.remove(filterDef.getFilterName());
    }


    public void removeFilterMap(FilterMap filterMap) {
        synchronized (filterMaps) {
            // Make sure this filter mapping is currently present
            int n = -1;
            for (int i = 0; i < filterMaps.length; i++) {
                if (filterMaps[i] == filterMap) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified filter mapping
            FilterMap results[] = new FilterMap[filterMaps.length - 1];
            System.arraycopy(filterMaps, 0, results, 0, n);
            System.arraycopy(filterMaps, n + 1, results, n,
                    (filterMaps.length - 1) - n);
            filterMaps = results;

        }
    }

    public boolean filterStart() {
        System.out.println("Filter Start..........");
        // Instantiate and record a FilterConfig for each defined filter
        boolean ok = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            Iterator<String> names = filterDefs.keySet().iterator();
            while (names.hasNext()) {
                String name = names.next();
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig
                            (this, (FilterDef) filterDefs.get(name));
                    filterConfigs.put(name, filterConfig);
                } catch (Throwable t) {
                    ok = false;
                }
            }
        }

        return (ok);

    }
    public FilterConfig findFilterConfig(String name) {
        return (filterConfigs.get(name));
    }
    private boolean validateURLPattern(String urlPattern) {
        if (urlPattern == null)
            return (false);
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0)
                return (true);
            else
                return (false);
        }
        if (urlPattern.startsWith("/"))
            return (true);
        else
            return (false);
    }

    public boolean listenerStart() {
        System.out.println("Listener Start..........");
        boolean ok = true;
        synchronized (listeners) {
            listeners.clear();
            Iterator<ContainerListenerDef> defs = listenerDefs.iterator();
            while (defs.hasNext()) {
                ContainerListenerDef def = defs.next();
                ContainerListener listener = null;
                try {
                    // Identify the class loader we will be using
                    String listenerClass = def.getListenerClass();
                    ClassLoader classLoader = null;
                    classLoader = this.getLoader();

                    ClassLoader oldCtxClassLoader =
                            Thread.currentThread().getContextClassLoader();

                    // Instantiate a new instance of this filter and return it
                    Class<?> clazz = classLoader.loadClass(listenerClass);
                    listener = (ContainerListener) clazz.newInstance();

                    addContainerListener(listener);
                } catch (Throwable t) {
                    t.printStackTrace();
                    ok = false;
                }
            }
        }

        return (ok);
    }
}
