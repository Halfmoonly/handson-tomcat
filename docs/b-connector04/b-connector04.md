## Servlet 规范适配
之前我们只是给Servlet实现了Servlet规范。但Servlet规范远不止如此

在 Servlet 规范中，对 Request 请求和 Response 返回也提供了对应的 HTTP 协议接口定义，分别是 javax.servlet.http.HttpServletRequest 和 javax.servlet.http.HttpServletResponse。

接下来我们为请求和响应，实现Servlet规范定义的接口，并逐步开始解析工作。

- 本节只对 Request 请求相关的代码进行实现

## 定义 HttpRequest
首先，定义 HttpRequest 与 HttpResponse，分别实现 HttpServletRequest 与 HttpServletResponse 接口。

实现了这个接口之后，就会要求实现很多接口里面的方法，绝大部分我们都没有用到，所以这里我只列举出几个基本的，不过后面我也给出了完整的代码链接，你可以参考。

```java
package server;
public class HttpRequest implements HttpServletRequest {
    public Object getAttribute(String arg0) {
        return null;
    }
    public Enumeration<String> getAttributeNames() {
        return null;
    }
    public String getCharacterEncoding() {
        return null;
    }
    public int getContentLength() {
        return 0;
    }
    public String getContentType() {
        return null;
    }
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }
    public String getParameter(String arg0) {
        return null;
    }
    public Map<String, String[]> getParameterMap() {
        return null;
    }
    public Enumeration<String> getParameterNames() {
        return null;
    }
    public String[] getParameterValues(String arg0) {
        return null;
    }
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }
    public ServletContext getServletContext() {
        return null;
    }
    public void setAttribute(String arg0, Object arg1) {
    }
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
    }
    public Cookie[] getCookies() {
        return null;
    }
    public String getQueryString() {
        return null;
    }
    public String getRequestURI() {
        return null;
    }
    public StringBuffer getRequestURL() {
        return null;
    }
    public String getServletPath() {
        return null;
    }
    public HttpSession getSession() {
        return null;
    }
    public HttpSession getSession(boolean arg0) {
        return null;
    }
}
```

## 定义HttpResponse
同样的，HttpServletResponse 接口也会有很多实现方法。我们先定义，然后再开始慢慢实现，并不是每个方法都需要用到，绝大多数方法暂时可以不理会。
```java
package server;
public class HttpResponse implements HttpServletResponse {
    public String getCharacterEncoding() {
        return null;
    }
    public String getContentType() {
        return null;
    }
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }
    public PrintWriter getWriter() throws IOException {
        return null;
    }
    public void setCharacterEncoding(String arg0) {
    }
    public void setContentLength(int arg0) {
    }
    public void setContentType(String arg0) {
    }
    public void addCookie(Cookie arg0) {
    }
    public String getHeader(String arg0) {
        return null;
    }
    public Collection<String> getHeaderNames() {
        return null;
    }
    public Collection<String> getHeaders(String arg0) {
        return null;
    }
}
```

## 改造 HttpRequest
持有三个重要成员变量，分别是请求头，请求参数，和HTTP请求规范中的第一行
```java
    protected HashMap<String, String> headers = new HashMap<>();
    protected Map<String, String> parameters = new ConcurrentHashMap<>();
    HttpRequestLine requestLine = new HttpRequestLine();//HTTP请求规范中的第一行
```

## 代理类 SocketInputStream

过去我们都是用 InputStream 进行解析，以字节为单位依次读取，效率不高。这里我们自己准备一个类——SocketInputStream，直接按行读取，获取 Request line 与 Header。

HttpRequest持有SocketInputStream，HttpRequest通过SocketInputStream直接拿到请求头等参数，SocketInputStream定义如下：

```java
public void readRequestLine(HttpRequestLine requestLine){}
public void readHeader(HttpHeader header){}
```

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

我们再输入 http://localhost:8080/servlet/test.HelloServlet 就可以看到浏览器显示：Hello World 你好，这也是我们在 HelloServlet 中定义的返回资源内容。