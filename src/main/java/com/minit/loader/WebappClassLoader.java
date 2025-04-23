package com.minit.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class WebappClassLoader extends URLClassLoader {
    protected boolean delegate = false;
    private ClassLoader parent = null;
    private ClassLoader system = null;

    public WebappClassLoader() {
        super(new URL[0]);
        this.parent = getParent();
        system = getSystemClassLoader();
    }
    public WebappClassLoader(URL[] urls) {
        super(urls);
        this.parent = getParent();
        system = getSystemClassLoader();
    }
    public WebappClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parent = parent;
        system = getSystemClassLoader();
    }
    public WebappClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.parent = parent;
        system = getSystemClassLoader();
    }

    public boolean getDelegate() {
        return (this.delegate);
    }
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }
    public Class findClass(String name) throws ClassNotFoundException {
        Class clazz = null;
        try {
            clazz = super.findClass(name);
        } catch (RuntimeException e) {
            throw e;
        }
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        // Return the class we have located
        return (clazz);
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return (loadClass(name, false));
    }
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class<?> clazz = null;
        try {
            clazz = system.loadClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
        }

        boolean delegateLoad = delegate;
        if (delegateLoad) {
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }

        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            ;
        }

        if (!delegateLoad) {
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (resolve)
                        resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }
        throw new ClassNotFoundException(name);
    }

    private void log(String message) {
        System.out.println("WebappClassLoader: " + message);
    }
    private void log(String message, Throwable throwable) {
        System.out.println("WebappClassLoader: " + message);
        throwable.printStackTrace(System.out);
    }
}
