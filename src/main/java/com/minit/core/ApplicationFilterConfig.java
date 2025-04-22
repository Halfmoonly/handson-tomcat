package com.minit.core;

import com.minit.Context;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

final class ApplicationFilterConfig implements FilterConfig {
    public ApplicationFilterConfig(Context context, FilterDef filterDef)
            throws ClassCastException, ClassNotFoundException,
            IllegalAccessException, InstantiationException,
            ServletException {
        super();
        this.context = context;
        setFilterDef(filterDef);
    }
    private Context context = null;
    private Filter filter = null;
    private FilterDef filterDef = null;
    public String getFilterName() {
        return (filterDef.getFilterName());
    }
    public String getInitParameter(String name) {
        Map<String,String> map = filterDef.getParameterMap();
        if (map == null)
            return (null);
        else
            return ((String) map.get(name));
    }
    public Enumeration<String> getInitParameterNames() {
        Map<String,String> map = filterDef.getParameterMap();
        if (map == null)
            return Collections.enumeration(new ArrayList<String>());
        else
            return (Collections.enumeration(map.keySet()));
    }
    public ServletContext getServletContext() {
        return (this.context.getServletContext());
    }
    public String toString() {
        StringBuffer sb = new StringBuffer("ApplicationFilterConfig[");
        sb.append("name=");
        sb.append(filterDef.getFilterName());
        sb.append(", filterClass=");
        sb.append(filterDef.getFilterClass());
        sb.append("]");
        return (sb.toString());
    }
    Filter getFilter() throws ClassCastException, ClassNotFoundException,
            IllegalAccessException, InstantiationException, ServletException {
        // 返回现有的过滤器实例（如果有的话）
        if (this.filter != null)
            return (this.filter);
        // 确定我们将使用的类加载器
        String filterClass = filterDef.getFilterClass();
        ClassLoader classLoader = null;
        classLoader = context.getLoader();
        ClassLoader oldCtxClassLoader =
                Thread.currentThread().getContextClassLoader();
        // 实例化这个过滤器的新实例并返回
        Class clazz = classLoader.loadClass(filterClass);
        this.filter = (Filter) clazz.newInstance();
        filter.init(this);
        return (this.filter);
    }
    FilterDef getFilterDef() {
        return (this.filterDef);
    }
    void release() {
        if (this.filter != null)
            filter.destroy();
        this.filter = null;
    }
    void setFilterDef(FilterDef filterDef)
            throws ClassCastException, ClassNotFoundException,
            IllegalAccessException, InstantiationException,
            ServletException {
        this.filterDef = filterDef;
        if (filterDef == null) {
            // 释放之前分配的所有过滤器实例
            if (this.filter != null)
                this.filter.destroy();
            this.filter = null;
        } else {
            // 分配一个新的过滤器实例
            Filter filter = getFilter();
        }
    }
}