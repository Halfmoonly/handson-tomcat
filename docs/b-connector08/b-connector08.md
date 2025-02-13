在处理完参数解析之后，我们接下来考虑解析 Cookie，Cookie 也是放在 Header 里的，固定格式是 `Cookie: userName=xxxx;password=pwd;`，

因此我们再次解析 Header 的时候，如果发现 Header 的名称是 Cookie，就进一步解析 Cookie。因为 Cookie 的数据结构需要遵从 javax.servlet.http.Cookie 规定，而 request 里可以包含多个 Cookie，所以我们会用数组来存储

在解析 Cookie 后我们再看一下 Session，其实这两部分的改造可以放在一起，所以我们本节一起讨论。

HTTP 协议本身是无状态的，但是在网站登录后我们又不希望一跳转页面就需要重新输入账号密码登录，在这种情况下就需要记住第一次登录状态，而 Servlet 规范就规定了使用 Session 记住用户状态，定义接口为 javax.servlet.http.HttpSession。

Session 由服务器创建，存在 SessionID，依靠 URL 或者是 Cookie 传送，把名称定义成 jsessionid。今后浏览器与服务器之间的数据交换都带上这个 jsessionid. 然后程序可以根据 jsessionid 拿到这个 Session，把一些状态数据存储在 Session 里。

## 服务器Session定义

一个 Session 其实可以简单地看成一个 Map 结构，然后由我们的 Server 为每个客户端创建一个 Session。

首先我们先定义 Session 类与 SessionFacade 类，用来处理 Server 中的逻辑。

Session 类主要是作为 javax.servlet.http.HttpSession 接口的实现类
```java
package server;
public class Session implements HttpSession{
    private String sessionid;
    private long creationTime;
    private boolean valid;
    private Map<String,Object> attributes = new ConcurrentHashMap<>();
    @Override
    public long getCreationTime() {
        return this.creationTime;
    }
    @Override
    public String getId() {
        return this.sessionid;
    }
    @Override
    public long getLastAccessedTime() {
        return 0;
    }
    @Override
    public ServletContext getServletContext() {
        return null;
    }
    @Override
    public void setMaxInactiveInterval(int interval) {
    }
    @Override
    public int getMaxInactiveInterval() {
        return 0;
    }
    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }
    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
    @Override
    public Object getValue(String name) {
        return this.attributes.get(name);
    }
    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }
    @Override
    public String[] getValueNames() {
        return null;
    }
    @Override
    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }
    @Override
    public void putValue(String name, Object value) {
        this.attributes.put(name, value);
    }
    @Override
    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }
    @Override
    public void removeValue(String name) {
    }
    @Override
    public void invalidate() {
        this.valid = false;
    }
    @Override
    public boolean isNew() {
        return false;
    }
    public void setValid(boolean b) {
        this.valid = b;
    }
    public void setCreationTime(long currentTimeMillis) {
        this.creationTime = currentTimeMillis;
    }
    public void setId(String sessionId) {
        this.sessionid = sessionId;
    }
}
```

SessionFacade.java 类定义如下：

SessionFacade 类则封装了希望对外暴露的接口，隐藏我们内部的实现。

```java
package server;
public class SessionFacade implements HttpSession{
    private HttpSession session;
    public SessionFacade(HttpSession session) {
        this.session = session;
    }
    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }
    @Override
    public String getId() {
        return session.getId();
    }
    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }
    @Override
    public ServletContext getServletContext() {
        return session.getServletContext();
    }
    @Override
    public void setMaxInactiveInterval(int interval) {
        session.setMaxInactiveInterval(interval);
    }
    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }
    @Override
    public HttpSessionContext getSessionContext() {
        return session.getSessionContext();
    }
    @Override
    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }
    @Override
    public Object getValue(String name) {
        return session.getValue(name);
    }
    @Override
    public Enumeration<String> getAttributeNames() {
        return session.getAttributeNames();
    }
    @Override
    public String[] getValueNames() {
        return session.getValueNames();
    }
    @Override
    public void setAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }
    @Override
    public void putValue(String name, Object value) {
        session.putValue(name, value);
    }
    @Override
    public void removeAttribute(String name) {
        session.removeAttribute(name);
    }
    @Override
    public void removeValue(String name) {
        session.removeValue(name);
    }
    @Override
    public void invalidate() {
        session.invalidate();
    }
    @Override
    public boolean isNew() {
        return session.isNew();
    }
}
```

## Session流程详细分析
我们需要明确，创建 Session 的是 Server，也就是 Servlet 容器，比如 Tomcat 或者 MiniTomcat。

客户端 Client 对 Servlet 的处理流程里只是使用 Session 存储的数据。

程序员通过 HttpServletRequest#getSession() 获取返回的 HttpSession。

上述过程完毕之后，Response 返回参数内会回写 Sessionid，这是通过在响应头中设置 set-cookie 参数实现的。

整个 Session 创建获取的情况是这样的：
1. 对一个全新的客户端发送 HTTP 请求到 Server：Server 发现这是一个全新的请求，为它创建 Session，分配 Sessionid，并在 Response 的返回头中设置 Set-Cookie。 生成的 Session 可能存放了某些身份认证识别的内容。
2. 客户端再次请求，这次在请求头 Cookie 内带上回传的 Sessionid：Server 发现第二次请求带有 Sessionid，根据 id 匹配到 Session，并取出之前存放在 Session 里的内容。

我们要明确一个事实，虽然我们一直将 Cookie 与 Session 放在一起来讲，甚至有可能将二者混为一谈，但 Session 不一定要依赖 Cookie（某些时候存在设置不接受 Cookie 的情况），

也可以通过 URL 中参数带的 jsessionid 来做到，比如 /test/TestServlet;jsessionid=5AC6268DD8D4D5D1FDF5D41E9F2FD960?curAlbumID=9。浏览器是在 URL 之后加上 ;jsessionid= 这个固定搭配来传递 Session，不是普通的参数格式。

## 服务器程序调整
接下来我们看看怎么处理。首先是接收请求时在 HttpRequest 类中解析 Session。提取 parseRequestLine 公共方法，你可以看一下 HttpRequest 类完整定义，接口实现类我只列出有调整的。

```java
package server;

public class HttpRequest implements HttpServletRequest {
    private InputStream input;
    private SocketInputStream sis;
    private String uri;
    private String queryString;
    InetAddress address;
    int port;
    private boolean parsed = false;
    protected HashMap<String, String> headers = new HashMap<>();
    protected Map<String, String[]> parameters = new ConcurrentHashMap<>();
    HttpRequestLine requestLine = new HttpRequestLine();
    Cookie[] cookies;
    HttpSession session;
    String sessionid;
    SessionFacade sessionFacade;
    public HttpRequest(InputStream input) {
        this.input = input;
        this.sis = new SocketInputStream(this.input, 2048);
    }
    //解析请求行和头header
    public void parse(Socket socket) {
        try {
            parseConnection(socket);
            this.sis.readRequestLine(requestLine);
            parseRequestLine();
            parseHeaders();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
    //处理请求行
    private void parseRequestLine() {
        //以问号判断是否带有参数串
        int question = requestLine.indexOf("?");
        if (question >= 0) {
            queryString = new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1);
            uri = new String(requestLine.uri, 0, question);
            //处理参数串中带有jsessionid的情况
            int semicolon = uri.indexOf(DefaultHeaders.JSESSIONID_NAME);
            if (semicolon >= 0) {
                sessionid = uri.substring(semicolon+DefaultHeaders.JSESSIONID_NAME.length());
                uri = uri.substring(0, semicolon);
            }
        } else {
            queryString = null;
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
            int semicolon = uri.indexOf(DefaultHeaders.JSESSIONID_NAME);
            if (semicolon >= 0) {
                sessionid = uri.substring(semicolon+DefaultHeaders.JSESSIONID_NAME.length());
                uri = uri.substring(0, semicolon);
            }
        }
    }
    private void parseConnection(Socket socket) {
        address = socket.getInetAddress();
        port = socket.getPort();
    }
    //解析所有header信息
    private void parseHeaders() throws IOException, ServletException {
        while (true) {
            HttpHeader header = new HttpHeader();
            sis.readHeader(header);
            if (header.nameEnd == 0) {
                if (header.valueEnd == 0) {
                    return;
                } else {
                    throw new ServletException("httpProcessor.parseHeaders.colon");
                }
            }
            String name = new String(header.name,0,header.nameEnd);
            String value = new String(header.value, 0, header.valueEnd);
            // 设置相应的头信息
            if (name.equals(DefaultHeaders.ACCEPT_LANGUAGE_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.CONTENT_LENGTH_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.CONTENT_TYPE_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.HOST_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.CONNECTION_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.TRANSFER_ENCODING_NAME)) {
                headers.put(name, value);
            } else if (name.equals(DefaultHeaders.COOKIE_NAME)) {
                headers.put(name, value);
                //处理cookie和session
                Cookie[] cookiearr = parseCookieHeader(value);
                this.cookies = cookiearr;
                for (int i = 0; i < cookies.length; i++) {
                    if (cookies[i].getName().equals("jsessionid")) {
                        this.sessionid = cookies[i].getValue();
                    }
                }
            }
            else {
                headers.put(name, value);
            }
        }
    }
    //解析Cookie头，格式为: key1=value1;key2=value2
    public  Cookie[] parseCookieHeader(String header) {
        if ((header == null) || (header.length() < 1) )
            return (new Cookie[0]);
        ArrayList<Cookie> cookieal = new ArrayList<>();
        while (header.length() > 0) {
            int semicolon = header.indexOf(';');
            if (semicolon < 0)
                semicolon = header.length();
            if (semicolon == 0)
                break;
            String token = header.substring(0, semicolon);
            if (semicolon < header.length())
                header = header.substring(semicolon + 1);
            else
                header = "";
            try {
                int equals = token.indexOf('=');
                if (equals > 0) {
                    String name = token.substring(0, equals).trim();
                    String value = token.substring(equals+1).trim();
                    cookieal.add(new Cookie(name, value));
                }
            } catch (Throwable e) {
            }
        }
        return ((Cookie[]) cookieal.toArray (new Cookie [cookieal.size()]));
    }
    
    @Override
    public Cookie[] getCookies() {
        return this.cookies;
    }
    
    @Override
    public HttpSession getSession() {
        return this.sessionFacade;
    }
    //如果有存在的session，直接返回，如果没有，创建一个新的session
    public HttpSession getSession(boolean create) {
        if (sessionFacade != null)
            return sessionFacade;
        if (sessionid != null) {
            session = HttpConnector.sessions.get(sessionid);
            if (session != null) {
                sessionFacade = new SessionFacade(session);
                return sessionFacade;
            } else {
                session = HttpConnector.createSession();
                sessionFacade = new SessionFacade(session);
                return sessionFacade;
            }
        } else {
            session = HttpConnector.createSession();
            sessionFacade = new SessionFacade(session);
            sessionid = session.getId();
            return sessionFacade;
        }
    }
    public String getSessionId() {
        return this.sessionid;
    }
}
```
上述代码中，我们着重关注 parseRequestLine 与 parseCookieHeader 方法的实现，主要是解析 URL 里包含 jsessionid 的部分，以及从 Cookie 中获取 jsessionid。

但这个时候我们只是获取到了 id，并没有获取 Session，按照程序执行的顺序，如果在 URL 的查询字符串与 Cookie 中都存在 jsessonid，那么我们会优先获取 Cookie 里对应的这个值。

由于我们一个 Server 需要对应多个 Client，所以在 Server 内我们考虑采用 Map 结构存储 Session，其中 Key 为 Sessionid，Value 为 Session 认证信息。

因为 HttpConnector 类是全局的，所以现在我们先把这个 Map 存放在 HttpConnector 类里。同时将 createSession 方法以及 generateSesionId 方法也都放在 HttpConnector 中。你可以看一下相关代码。

```java
package server;
public class HttpConnector implements Runnable {
    int minProcessors = 3;
    int maxProcessors = 10;
    int curProcessors = 0;
    Deque<HttpProcessor> processors = new ArrayDeque<>();
    //sessions map存放session
    public static Map<String, HttpSession> sessions = new ConcurrentHashMap<>();
    //创建新的session
    public static Session createSession() {
        Session session = new Session();
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        String sessionId = generateSessionId();
        session.setId(sessionId);
        sessions.put(sessionId, session);
        return (session);
    }
    //以随机方式生成byte数组,形成sessionid
    protected static synchronized String generateSessionId() {
        Random random = new Random();
        long seed = System.currentTimeMillis();
        random.setSeed(seed);
        byte bytes[] = new byte[16];
        random.nextBytes(bytes);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
            byte b2 = (byte) (bytes[i] & 0x0f);
            if (b1 < 10)
                result.append((char) ('0' + b1));
            else
                result.append((char) ('A' + (b1 - 10)));
            if (b2 < 10)
                result.append((char) ('0' + b2));
            else
                result.append((char) ('A' + (b2 - 10)));
        }
        return (result.toString());
    }
}
```
将 Session 的生成和管理全部放在 Connector 内还是有点儿臃肿，后面我们会进一步分解功能，通过 Container 和 Manager 来管理 Session，现在暂且不深入讨论。

因为 HttpConnector 会在接受 Socket 之后，为 Processor 分配 Socket，所以我们先在 HttpProcessor 类中进行 Session 处理。

```java
            // create Request object and parse
            HttpRequest request = new HttpRequest(input);
            request.parse(socket);
            
            //handle session
            if (request.getSessionId()==null || request.getSessionId().equals("")) {
              request.getSession(true);
            }

            // create Response object
            HttpResponse response = new HttpResponse(output);
            response.setRequest(request);
```
代码里注释为“handle session”的代码就是我们添加的，判断是否存在 Sessionid，不存在则调用 getSession 方法，这个方法内会判断有没有 Session，如果没有服务器就创建并保存在sessions的Map中。

同时，如果请求中带有 jsessionid，我们会用这个 jsessionid 从 HttpConnector 类的全局 Map 里查找相应的 Session。

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

接下来通过GET请求进行cookie和session的测试

请求形式1
```shell
### jsessionid位于cookie中传递
GET http://localhost:8080/servlet/test.TestServlet?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Cookie: jsessionid=abc123; sex=male; age=18
Connection: keep-alive
```
请求形式2
```shell
### jsessionid位于uri中传递
GET http://localhost:8080/servlet/test.TestServlet;jsessionid=5AC6268DD8D4D5D1FDF5D41E9F2FD960?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```
请求形式3
```shell
### uri中和cookie中均有jsessionid，则cookie中的jsessionid会覆盖uri中的jsessionid
GET http://localhost:8080/servlet/test.TestServlet;jsessionid=5AC6268DD8D4D5D1FDF5D41E9F2FD960?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Cookie: jsessionid=abc123; sex=male; age=18
Connection: keep-alive
```

下面是连续发送三次GET请求的控制台日志
```shell
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@7fb63f87
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: null
gen user to session in server...
get user from session in server: Halfmoonly
0-->jsessionid= 00D482DB2A771222A5752B4C97ED10B4 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@39cb0f7c
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: null
gen user to session in server...
get user from session in server: Halfmoonly
0-->jsessionid= 00D482DB2A771222A5752B4C97ED10B4 session=Halfmoonly
1-->jsessionid= 0D378F5640BB9470F7A20648FEFF7CD7 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@28783df2
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: null
gen user to session in server...
get user from session in server: Halfmoonly
0-->jsessionid= 00D482DB2A771222A5752B4C97ED10B4 session=Halfmoonly
1-->jsessionid= F96AEA3E764CDECF994CE5144B0C30C5 session=Halfmoonly
2-->jsessionid= 0D378F5640BB9470F7A20648FEFF7CD7 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>
```