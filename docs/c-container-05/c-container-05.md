监听器一般和事件共同存在，也就是先有事件定义，后面对这个事件进行监听。

在 MiniTomcat 里，我们定义 InstanceListener、ContainerListener、SessionListener 三种监听器以及对应的事件， 分别作用于整个对象实例、容器 Container 和 Session 处理。

这样我们 MiniTomcat 内部主要对象的各种行为都能够被监听了。

具体思路如下：
- 定义事件接口和监听器接口
- 监听器接口交给用户自定义实现
- 框架启动时通过反射提前收集所有用户的监听器实现
- 框架指定在合适的时候发布相应的事件（其实就是框架帮用户来执行监听方法，同时给用户监听方法中传入相应的事件参数）

```java
public class TestListener implements ContainerListener{

    @Override
    public void containerEvent(ContainerEvent event) {
        System.out.println(event);
    }

}
```

我们先来看看事件和监听器的代码定义。
## 容器事件接口和监听器接口定义
ContainerEvent 是一个基础的容器事件对象。
```java
package com.minit;
public final class ContainerEvent extends EventObject {
    private Container container = null;
    private Object data = null;
    private String type = null;
    public ContainerEvent(Container container, String type, Object data) {
        super(container);
        this.container = container;
        this.type = type;
        this.data = data;
    }
    public Object getData() {
        return (this.data);
    }
    public Container getContainer() {
        return (this.container);
    }
    public String getType() {
        return (this.type);
    }
    public String toString() {
        return ("ContainerEvent['" + getContainer() + "','" +
                getType() + "','" + getData() + "']");
    }
}
```

ContainerListener 用于监听容器事件。
```java
package com.minit;
public interface ContainerListener {
    public void containerEvent(ContainerEvent event);
}
```

## 实例事件定义与监听器接口定义
InstanceEvent 是 Servlet 的事件。
```java
package com.minit;
public final class InstanceEvent extends EventObject {
    public static final String BEFORE_INIT_EVENT = "beforeInit";
    public static final String AFTER_INIT_EVENT = "afterInit";
    public static final String BEFORE_SERVICE_EVENT = "beforeService";
    public static final String AFTER_SERVICE_EVENT = "afterService";
    public static final String BEFORE_DESTROY_EVENT = "beforeDestroy";
    public static final String AFTER_DESTROY_EVENT = "afterDestroy";
    public static final String BEFORE_DISPATCH_EVENT = "beforeDispatch";
    public static final String AFTER_DISPATCH_EVENT = "afterDispatch";
    public static final String BEFORE_FILTER_EVENT = "beforeFilter";
    public static final String AFTER_FILTER_EVENT = "afterFilter";
    public InstanceEvent(Wrapper wrapper, Filter filter, String type) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
    }
    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         Throwable exception) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.exception = exception;
    }
    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         ServletRequest request, ServletResponse response) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.request = request;
        this.response = response;
    }
    public InstanceEvent(Wrapper wrapper, Filter filter, String type,
                         ServletRequest request, ServletResponse response,
                         Throwable exception) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = filter;
        this.servlet = null;
        this.type = type;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }
    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
    }
    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         Throwable exception) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.exception = exception;
    }
    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         ServletRequest request, ServletResponse response) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.request = request;
        this.response = response;
    }
    public InstanceEvent(Wrapper wrapper, Servlet servlet, String type,
                         ServletRequest request, ServletResponse response,
                         Throwable exception) {
        super(wrapper);
        this.wrapper = wrapper;
        this.filter = null;
        this.servlet = servlet;
        this.type = type;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }
    private Throwable exception = null;
    private Filter filter = null;
    private ServletRequest request = null;
    private ServletResponse response = null;
    private Servlet servlet = null;
    private String type = null;
    private Wrapper wrapper = null;
    public Throwable getException() {
        return (this.exception);
    }
    public Filter getFilter() {
        return (this.filter);
    }
    public ServletRequest getRequest() {
        return (this.request);
    }
    public ServletResponse getResponse() {
        return (this.response);
    }
    public Servlet getServlet() {
        return (this.servlet);
    }
    public String getType() {
        return (this.type);
    }
    public Wrapper getWrapper() {
        return (this.wrapper);
    }
}
```

InstanceListener 是 Servlet 事件的监听器。
```java
package com.minit;
public interface InstanceListener {
    public void instanceEvent(InstanceEvent event);
}
```

## Session事件定义与监听器接口定义
SessionEvent 是 session 事件。
```java
package com.minit;
public final class SessionEvent extends EventObject {
    private Object data = null;
    private Session session = null;
    private String type = null;
    public SessionEvent(Session session, String type, Object data) {
        super(session);
        this.session = session;
        this.type = type;
        this.data = data;
    }
    public Object getData() {
        return (this.data);
    }
    public Session getSession() {
        return (this.session);
    }
    public String getType() {
        return (this.type);
    }
    public String toString() {
        return ("SessionEvent['" + getSession() + "','" +
                getType() + "']");
    }
}
```

SessionListener 是 session 事件监听器。
```java
package com.minit;
public interface SessionListener {
    public void sessionEvent(SessionEvent event);
}
```

## 框架改造
有了这些事件和监听器的定义后，在相应的类里面加上 addlistener()、removelistener() 方法以及 fireEvent() 就可以了。所以在 StandardContext 类和 StandardSession 类中，我们可以添加这三个方法，你可以看一下相关实现。

首先是 StandardContext 类，我们在代码里新增方法实现。这些新代码处理了容器监听器。
```java
package com.minit.core;
public class StandardContext extends ContainerBase implements Context{
    private ArrayList<ContainerListenerDef> listenerDefs = new ArrayList<>();
    private ArrayList<ContainerListener> listeners = new ArrayList<>();
    
    public void start(){
        // 触发一个容器启动事件  
        fireContainerEvent("Container Started",this);
    }
    public void addContainerListener(ContainerListener listener) {
        // 添加一个新的容器监听器到监听器列表，并确保线程安全  
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public void removeContainerListener(ContainerListener listener) {
        // 移除指定的容器监听器，并确保线程安全  
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    public void fireContainerEvent(String type, Object data) {
        // 检查是否已经有监听器，如果没有则直接返回  
        if (listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners) {
            list = (ContainerListener[]) listeners.toArray(list);
        }
        // 遍历所有监听器并触发事件  
        for (int i = 0; i < list.length; i++)
            ((ContainerListener) list[i]).containerEvent(event);
    }
    public void addListenerDef(ContainerListenerDef listenererDef) {
        synchronized (listenerDefs) {
            listenerDefs.add(listenererDef);
        }
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
                    // 确定我们将要使用的类加载器
                    String listenerClass = def.getListenerClass();
                    ClassLoader classLoader = null;
                    classLoader = this.getLoader();
                    ClassLoader oldCtxClassLoader =
                            Thread.currentThread().getContextClassLoader();
                    // 创建这个过滤器的新实例并返回它
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
```

## 定义元数据ContainerListenerDef
和前面说的一样，我们在这儿新增了 addContainerListener、removeContainerListener 以及 fireContainerEvent 三个方法，使方法名称更加具体化了。

有一些不同的是我们还引入了 addListenerDef 方法，接受 ContainerListenerDef 类型的传参数。其实 ContainerListenerDef 和上一部分的 FilterDef 类似，也只是对 Container 监听器的属性进行定义，我们看看具体定义内容
```java
package com.minit.core;
public final class ContainerListenerDef {
    private String description = null;
    public String getDescription() {
        return (this.description);
    }
    public void setDescription(String description) {
        this.description = description;
    }
    private String displayName = null;
    public String getDisplayName() {
        return (this.displayName);
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    private String listenerClass = null;
    public String getListenerClass() {
        return (this.listenerClass);
    }
    public void setListenerClass(String listenerClass) {
        this.listenerClass = listenerClass;
    }
    private String listenerName = null;
    public String getListenerName() {
        return (this.listenerName);
    }
    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }
    private Map<String,String> parameters = new ConcurrentHashMap<>();
    public Map<String,String> getParameterMap() {
        return (this.parameters);
    }
    public void addInitParameter(String name, String value) {
        parameters.put(name, value);
    }
    public String toString() {
        StringBuffer sb = new StringBuffer("ListenerDef[");
        sb.append("listenerName=");
        sb.append(this.listenerName);
        sb.append(", listenerClass=");
        sb.append(this.listenerClass);
        sb.append("]");
        return (sb.toString());
    }
}
```

接下来我们看看 StandardSession 类如何进行改造，你可以看一下新增的代码实现。代码里面增加了 session 监听器的处理
```java
package com.minit.session;
public class StandardSession implements HttpSession, Session {
    private transient ArrayList<SessionListener> listeners = new ArrayList<>();
    public void addSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public void removeSessionListener(SessionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    public void fireSessionEvent(String type, Object data) {
        if (listeners.size() < 1)
            return;
        SessionEvent event = new SessionEvent(this, type, data);
        SessionListener list[] = new SessionListener[0];
        synchronized (listeners) {
            list = (SessionListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((SessionListener) list[i]).sessionEvent(event);
    }
    
    public void setId(String sessionId) {
        this.sessionid = sessionId;
        fireSessionEvent(Session.SESSION_CREATED_EVENT, null);
    }
}
```
其实也基本上类似于 StandardContext。和过滤器一样
## BootStrap启动提前通过反射收集用户所有的监听器实现
最后我们在启动类 BootStrap 的 main 函数里启动监听器就可以了，这样在服务器运行过程中则会持续监听指定事件。你可以看一下改造后 BootStrap 类中的 main 方法。
```java
package com.minit.startup;
public class BootStrap {
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator + "webroot";
    private static int debug = 0;
    public static void main(String[] args) {
        if (debug >= 1)
            log(".... startup ....");
        HttpConnector connector = new HttpConnector();
        StandardContext container = new StandardContext();
        connector.setContainer(container);
        container.setConnector(connector);
        Logger logger = new FileLogger();
        container.setLogger(logger);
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("TestFilter");
        filterDef.setFilterClass("test.TestFilter");
        container.addFilterDef(filterDef);
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("TestFilter");
        filterMap.setURLPattern("/*");
        container.addFilterMap(filterMap);
        container.filterStart();
        ContainerListenerDef listenerDef = new ContainerListenerDef();
        listenerDef.setListenerName("TestListener");
        listenerDef.setListenerClass("test.TestListener");
        container.addListenerDef(listenerDef);
        container.listenerStart();
        container.start();
        connector.start();
    }
}
```
到这里监听器部分的改造就先告一段落，现在我们可以监听 Session 和 Container 的动态了。

## 用户自定义监听器TestListener
同样地，在测试目录中新增 TestListener，可以测试我们的功能，测试方法的实现比较简单，直接输出事件对象，能够在控制台中看到输出就可以了。
```java
package test;
import com.minit.ContainerEvent;
import com.minit.ContainerListener;
public class TestListener implements ContainerListener{
    @Override
    public void containerEvent(ContainerEvent event) {
        System.out.println(event);
    }
}
```

## 小结

测试方式，先重新编译项目，把编译后的HelloServlet.class，TestListener.class，TestServlet.class都放在webroot/test目录下

并且注意用户请求的路径还是test.TestServlet。  快来验证下Listener是否生效
```shell
### 初次请求无jsessionid
GET http://localhost:8080/servlet/test.TestServlet?name=Tommy&docid=TS0001
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
Accept-Encoding: gzip, deflate, br, zstd
Accept-Language: zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7
Cache-Control: max-age=0
//Connection: keep-alive
Host: localhost:8080
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Upgrade-Insecure-Requests: 1
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36
sec-ch-ua: "Not A(Brand";v="8", "Chromium";v="132", "Google Chrome";v="132"
sec-ch-ua-mobile: ?0
sec-ch-ua-platform: "Windows"

```