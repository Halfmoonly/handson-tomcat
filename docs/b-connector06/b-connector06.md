前面章节我们完成了基于标准Servlet规范的 HttpRequest 与 HttpResponse 的替换。

现在有一个问题需要我们解决：

我们直接使用的是 HttpRequest 与 HttpResponse，这两个对象要传入 Servlet 中，但在这两个类中我们也定义了许多内部的方法，一旦被用户知晓我们的实现类，则这些内部方法就暴露在用户面前了。不符合最小知识集原理。

另外，这个 Request 和 Response 类是要传给外部的 Servlet 程序的，跳出了 Tomcat 本身，如果这个写 Servlet 的程序员他知道传的这个类里面有一些额外的方法，原理上他可以进行强制转换之后调用，这样也不是很安全。

比如在Java中，如果你有一个Father类和它的两个子类Son1和Son2，并且你有一个方法接受一个类型为Father的参数，那么你可以尝试将这个参数向下转型（downcast）为具体的Son1或Son2。例如：

下面就暴露了具体类Son1/Son2的全部隐私，是不安全的
```java
public void someMethod(Father father) {
    if (father instanceof Son1) {
        Son1 s1 = (Son1) father;
        // 现在可以使用s1作为Son1对象
    } else if (father instanceof Son2) {
        Son2 s2 = (Son2) father;
        // 现在可以使用s2作为Son2对象
    }
}
```
注意要使用instanceof关键字检查传入的对象是否实际上是Son1或Son2的实例。这样做是为了避免ClassCastException，因为如果直接进行强制类型转换而不进行这种检查，且传入的对象不是目标类型的实例时，就会抛出这个异常。


## Facade 模式的应用
接下来我们看看如何使用门面设计模式，规避上述问题。先解释一下 Facade 模式，Facade 这个词来自于建筑学，字面意思是“立面”，就是我们在大街上看到的门面。门面把建筑的内部结构包装起来给人们展示了一个新的友好的有特色的外观。软件中用同样的手法，将软件的内部结构进行包装，对外用简单的 API 供人使用。

Facade 类是一个新类，外部使用者没法根据它来强制转换获得内部的结构和方法，这样将实际实现的几个类保护起来了，提高了安全性。这正是我们现在在处理 Request 和 Response 的时候所希望看到的。我们按照这个思路写自己的 Facade。

首先定义 HttpRequestFacade 与 HttpResponseFacade，分别实现 HttpServletRequest 与 HttpServletResponse。你可以看一下我给出的代码主体部分，大部分都是直接转到 request 和 response 里面的相应调用：

HttpRequestFacade.java：
```java
package server;
public class HttpRequestFacade implements HttpServletRequest {
    private HttpServletRequest request;
    public HttpRequestFacade(HttpRequest request) {
        this.request = request;
    }
    /* implementation of the HttpServletRequest*/
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }
    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }
    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }
    public int getContentLength() {
        return request.getContentLength();
    }
    public String getContentType() {
        return request.getContentType();
    }
    public Cookie[] getCookies() {
        return request.getCookies();
    }
    public Enumeration getHeaderNames() {
        return request.getHeaderNames();
    }
    public String getHeader(String name) {
        return request.getHeader(name);
    }
    public Enumeration getHeaders(String name) {
        return request.getHeaders(name);
    }
    public ServletInputStream getInputStream() throws IOException {
        return request.getInputStream();
    }
    public int getIntHeader(String name) {
        return request.getIntHeader(name);
    }
    public String getMethod() {
        return request.getMethod();
    }
    public String getParameter(String name) {
        return request.getParameter(name);
    }
    public Map getParameterMap() {
        return request.getParameterMap();
    }
    public Enumeration getParameterNames() {
        return request.getParameterNames();
    }
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }
    public String getQueryString() {
        return request.getQueryString();
    }
    public BufferedReader getReader() throws IOException {
        return request.getReader();
    }
    public String getRequestURI() {
        return request.getRequestURI();
    }
    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }
    public HttpSession getSession() {
        return request.getSession();
    }
    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }
    public void removeAttribute(String attribute) {
        request.removeAttribute(attribute);
    }
    public void setAttribute(String key, Object value) {
        request.setAttribute(key, value);
    }
    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        request.setCharacterEncoding(encoding);
    }
}
```

HttpResponseFacade.java 类主体定义如下：
```java
package server;

public class HttpResponseFacade implements HttpServletResponse {
    private HttpServletResponse response;
    public HttpResponseFacade(HttpResponse response) {
        this.response = response;
    }
    public void addDateHeader(String name, long value) {
        response.addDateHeader(name, value);
    }
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }
    public String encodeUrl(String url) {
        return response.encodeUrl(url);
    }
    public String encodeURL(String url) {
        return response.encodeURL(url);
    }
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }
    @Override
    public String getContentType() {
        return null;
    }
    @Override
    public void setCharacterEncoding(String s) {
    }
    public void setContentLength(int length) {
        response.setContentLength(length);
    }
    public void setContentType(String type) {
        response.setContentType(type);
    }
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }
    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }
    @Override
    public void addCookie(Cookie arg0) {
        response.addCookie(arg0);
    }
    @Override
    public String getHeader(String arg0) {
        return response.getHeader(arg0);
    }
    @Override
    public Collection<String> getHeaderNames() {
        return response.getHeaderNames();
    }
    @Override
    public Collection<String> getHeaders(String arg0) {
        return response.getHeaders(arg0);
    }
}
```

## 项目结构调整
在此两个 Facade 就定义完毕了，前面我们也说过，我们要做的是向 Servlet 传入参数时规避内部方法，而 Facade 的作用就是封装不希望暴露的方法，更深层的内部方法不予展示。

因而在 ServletProcessor 中，Servlet 调用 service 时我们传入 HttpRequestFacade 与 HttpResponseFacade，你可以看一下需要调整的部分代码

```java
ServletProcessor.java

Servlet servlet = null;
//调用servlet的service()方法时传入的是Facade对象
try {
    servlet = (Servlet) servletClass.newInstance();
    HttpRequestFacade requestFacade = new HttpRequestFacade(request);
    HttpResponseFacade responseFacade = new HttpResponseFacade(response);
    servlet.service(requestFacade, responseFacade);
}
```

这样在 Servlet 中，我们看到的只是 Facade，看不见内部方法，应用程序员想进行强制转化也不行，这样既简单又安全。

还有，按照 Servlet 的规范，客户自定义的 Servlet 是要继承 HttpServlet 的，在调用的 service 方法内，它的实际行为是通过 method 判断调用的是哪一个方法，如果是 Get 方法就调用 doGet()，如果是 Post 方法调用的就是 doPost()，其他的方法也是一样的道理。

所以在我们自定义的 HttpRequest 里，一定要实现 getMethod 方法:

```java
package server;
public class HttpRequest implements HttpServletRequest{
    @Override
    public String getMethod() {
      return new String(this.requestLine.method, 0, this.requestLine.methodEnd);
    }
}
```
这样做可以简化客户程序，让业务程序员写 Servlet 的时候只需要重载 doGet() 或 doPost() 方法即可。

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

我们再输入 http://localhost:8080/servlet/test.HelloServlet 就可以看到浏览器显示：Hello World 你好，这也是我们在 HelloServlet 中定义的返回资源内容。