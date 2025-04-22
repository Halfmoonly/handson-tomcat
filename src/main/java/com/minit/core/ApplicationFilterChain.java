package com.minit.core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.minit.connector.HttpRequestFacade;
import com.minit.connector.HttpResponseFacade;
import com.minit.connector.http.HttpRequestImpl;
import com.minit.connector.http.HttpResponseImpl;
final class ApplicationFilterChain implements FilterChain {
    public ApplicationFilterChain() {
        super();
    }
    private ArrayList<ApplicationFilterConfig> filters = new ArrayList<>();
    private Iterator<ApplicationFilterConfig> iterator = null;
    private Servlet servlet = null;

    //核心方法，启动过滤
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        System.out.println("FilterChain doFilter()");
        internalDoFilter(request,response);
    }
    private void internalDoFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (this.iterator == null)
            this.iterator = filters.iterator();
        if (this.iterator.hasNext()) {
            //拿到下一个filter
            ApplicationFilterConfig filterConfig =
                    (ApplicationFilterConfig) iterator.next();
            Filter filter = null;
            try {
                //进行过滤，这是职责链模式，一个一个往下传
                filter = filterConfig.getFilter();
                System.out.println("Filter doFilter()");
                //调用filter的过滤逻辑，根据规范，filter中要再次调用filterChain.doFilter
                //这样又会回到internalDoFilter()方法，就会再拿到下一个filter，
                //如此实现一个一个往下传
                filter.doFilter(request, response, this);
            } catch (IOException e) {
                throw e;
            } catch (ServletException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new ServletException("filterChain.filter", e);
            }
            return;
        }
        try {
            //最后调用servlet
            HttpServletRequest requestFacade = new HttpRequestFacade((HttpRequestImpl) request);
            HttpServletResponse responseFacade = new HttpResponseFacade((HttpResponseImpl) response);
            servlet.service(requestFacade, responseFacade);
        } catch (IOException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new ServletException("filterChain.servlet", e);
        }
    }
    void addFilter(ApplicationFilterConfig filterConfig) {
        this.filters.add(filterConfig);
    }
    void release() {
        this.filters.clear();
        this.iterator = iterator;
        this.servlet = null;
    }
    void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }
}