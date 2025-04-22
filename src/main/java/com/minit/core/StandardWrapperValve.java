package com.minit.core;

import com.minit.Request;
import com.minit.Response;
import com.minit.ValveContext;
import com.minit.connector.HttpRequestFacade;
import com.minit.connector.HttpResponseFacade;
import com.minit.connector.http.HttpRequestImpl;
import com.minit.connector.http.HttpResponseImpl;
import com.minit.valves.ValveBase;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StandardWrapperValve extends ValveBase {

    private FilterDef filterDef = null;

    @Override
    public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {
        //创建filter Chain，再调用filter，然后调用servlet
        System.out.println("StandardWrapperValve invoke()");
        Servlet instance = ((StandardWrapper) getContainer()).getServlet();
        ApplicationFilterChain filterChain = createFilterChain(request, instance);
        if ((instance != null) && (filterChain != null)) {
            filterChain.doFilter((ServletRequest) request, (ServletResponse) response);
        }
        filterChain.release();
    }

    //根据context中的filter map信息挑选出符合模式的filter，创建filterChain
    private ApplicationFilterChain createFilterChain(Request request, Servlet servlet) {
        System.out.println("createFilterChain()");
        if (servlet == null)
            return (null);
        ApplicationFilterChain filterChain = new ApplicationFilterChain();
        filterChain.setServlet(servlet);
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        StandardContext context = (StandardContext) wrapper.getParent();
        //从context中拿到filter的信息
        FilterMap filterMaps[] = context.findFilterMaps();
        if ((filterMaps == null) || (filterMaps.length == 0))
            return (filterChain);
        //要匹配的路径
        String requestPath = null;
        if (request instanceof HttpServletRequest) {
            String contextPath = "";
            String requestURI = ((HttpRequestImpl) request).getUri(); //((HttpServletRequest) request).getRequestURI();
            if (requestURI.length() >= contextPath.length())
                requestPath = requestURI.substring(contextPath.length());
        }
        //要匹配的servlet名
        String servletName = wrapper.getName();

        //下面遍历filter Map，找到匹配URL模式的filter，加入到filterChain中
        int n = 0;
        for (int i = 0; i < filterMaps.length; i++) {
            if (!matchFiltersURL(filterMaps[i], requestPath))
                continue;
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
                    context.findFilterConfig(filterMaps[i].getFilterName());
            if (filterConfig == null) {
                continue;
            }
            filterChain.addFilter(filterConfig);
            n++;
        }
        //下面遍历filter Map，找到匹配servlet的filter，加入到filterChain中
        for (int i = 0; i < filterMaps.length; i++) {
            if (!matchFiltersServlet(filterMaps[i], servletName))
                continue;
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
                    context.findFilterConfig(filterMaps[i].getFilterName());
            if (filterConfig == null) {
                continue;
            }
            filterChain.addFilter(filterConfig);
            n++;
        }
        return (filterChain);
    }

    //字符串模式匹配filter的过滤路径
    private boolean matchFiltersURL(FilterMap filterMap, String requestPath) {
        if (requestPath == null)
            return (false);
        String testPath = filterMap.getURLPattern();
        if (testPath == null)
            return (false);
        if (testPath.equals(requestPath))
            return (true);
        if (testPath.equals("/*"))
            return (true);
        if (testPath.endsWith("/*")) { //路径符合/前缀，通配成功
            String comparePath = requestPath;
            while (true) {  //以/截取前段字符串，循环匹配
                if (testPath.equals(comparePath + "/*"))
                    return (true);
                int slash = comparePath.lastIndexOf('/');
                if (slash < 0)
                    break;
                comparePath = comparePath.substring(0, slash);
            }
            return (false);
        }
        if (testPath.startsWith("*.")) {
            int slash = requestPath.lastIndexOf('/');
            int period = requestPath.lastIndexOf('.');
            if ((slash >= 0) && (period > slash))
                return (testPath.equals("*." + requestPath.substring(period + 1)));
        }
        return (false); // NOTE - Not relevant for selecting filters
    }

    private boolean matchFiltersServlet(FilterMap filterMap, String servletName) {
        if (servletName == null)
            return (false);
        else
            return (servletName.equals(filterMap.getServletName()));
    }


}
