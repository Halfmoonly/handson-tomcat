在上一节课，我们基于最早的最小可用 HttpServer 服务器进行了改造。主要包括对 HTTP 协议返回内容中的状态行与返回头进行封装，以及引入动态资源和 Servlet 的概念，对 Web 端返回内容进行了扩充，已经有点 Servlet 容器的雏形了。

但我也提到，当前我们自定义的 Servlet 接口是不满足 Java Servlet 规范的。因此这节课我们首先会讨论如何符合 Servlet 规范，在 Java 的规则下实现 MiniTomcat。

## 项目结构

我们自定义的 Servlet 接口就不用了。根据 Servlet 规范，取而代之的应该是 javax.servlet.Servlet 类。接下来我们需要把 javax.servlet.Servlet 引入到代码之中，参考以下 pom.xml：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>handson-tomcat</groupId>
    <artifactId>handson-tomcat</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.4</version>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
        </dependency>
    </dependencies>

</project>
```
## Request 适配 Servlet 规范

所谓符合规范，对编写程序来讲，就是遵守流程调用时序和使用规定的 API 接口及数据格式。对 Servlet 规范来讲，第一个要遵守的就是必须实现 Servlet 接口。

在引入上文的 servlet-api 依赖后，我们可以把原来自己定义的 Servlet 接口删除，用 javax.servlet.Servlet 替换。我们先看看 javax.servlet.Servlet 的接口定义。

```java
package javax.servlet;
import java.io.IOException;
public interface Servlet {
    void init(ServletConfig var1) throws ServletException;
    ServletConfig getServletConfig();
    void service(ServletRequest var1, ServletResponse var2) throws ServletException, IOException;
    String getServletInfo();
    void destroy();
}
```
在工程中替换后，原先的代码会立即报错，因为 service 方法传入的参数是 ServletRequest 和 ServletResponse，而我们目前使用的是自定义的 Request 类和 Response 类。

因此，接下来分别让 Request 和 Response 实现 ServletRequest 与 ServletResponse，实现如下:

```java
public class Request implements ServletRequest{
    private InputStream input;
    private String uri;
    //以输入流作为Request的接收参数
    public Request(InputStream input) {
        this.input = input;
    }
    //简单的parser，假定从输入流中一次性获取全部字节，存放到2K缓存中
    public void parse() {
        StringBuffer request = new StringBuffer(2048);
        int i;
        byte[] buffer = new byte[2048];
        try {
            i = input.read(buffer);
        }
        catch (IOException e) {
            e.printStackTrace();
            i = -1;
        }
        for (int j=0; j<i; j++) {
            request.append((char) buffer[j]);
        }
        //从输入的字符串中解析URI
        uri = parseUri(request.toString());
    }
    //根据协议格式，以空格为界，截取中间的一段，即为URI
    private String parseUri(String requestString) {
        int index1, index2;
        index1 = requestString.indexOf(' ');
        if (index1 != -1) {
            index2 = requestString.indexOf(' ', index1 + 1);
            if (index2 > index1)
                return requestString.substring(index1 + 1, index2);
        }
        return null;
    }
    public String getUri() {
        return uri;
    }
    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }
    @Override
    public Object getAttribute(String arg0) {
        return null;
    }
    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }
    @Override
    public String getCharacterEncoding() {
        return null;
    }
    @Override
    public int getContentLength() {
        return 0;
    }
    @Override
    public long getContentLengthLong() {
        return 0;
    }
    @Override
    public String getContentType() {
        return null;
    }
    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }
    @Override
    public String getLocalAddr() {
        return null;
    }
    @Override
    public String getLocalName() {
        return null;
    }
    @Override
    public int getLocalPort() {
        return 0;
    }
    @Override
    public Locale getLocale() {
        return null;
    }
    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }
    @Override
    public String getParameter(String arg0) {
        return null;
    }
    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }
    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }
    @Override
    public String[] getParameterValues(String arg0) {
        return null;
    }
    @Override
    public String getProtocol() {
        return null;
    }
    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }
    @Override
    public String getRealPath(String arg0) {
        return null;
    }
    @Override
    public String getRemoteAddr() {
        return null;
    }
    @Override
    public String getRemoteHost() {
        return null;
    }
    @Override
    public int getRemotePort() {
        return 0;
    }
    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }
    @Override
    public String getScheme() {
        return null;
    }
    @Override
    public String getServerName() {
        return null;
    }
    @Override
    public int getServerPort() {
        return 0;
    }
    @Override
    public ServletContext getServletContext() {
        return null;
    }
    @Override
    public boolean isAsyncStarted() {
        return false;
    }
    @Override
    public boolean isAsyncSupported() {
        return false;
    }
    @Override
    public boolean isSecure() {
        return false;
    }
    @Override
    public void removeAttribute(String arg0) {
    }
    @Override
    public void setAttribute(String arg0, Object arg1) {
    }
    @Override
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
    }
    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }
    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        return null;
    }
}
```
从代码中可以看出，我们只是简单地实现了对 URI 的解析，别的方法都是留空的。

Java 的 API 考虑得很全面，在 Request 里面新增了许多接口实现方法，但是就基本功能来讲，只要很少的方法就可以了，我们暂且先把这些现在不用的方法放在一边不实现。

## Response 适配 Servlet 规范
接下来我们看看 Response 类的改造:

```java
package server;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
public class Response implements ServletResponse{
    Request request;
    OutputStream output;
    PrintWriter writer;
    String contentType = null;
    long contentLength = -1;
    String charset = null;
    String characterEncoding = null;

    //以输出流作为接收参数
    public Response(OutputStream output) {
        this.output = output;
    }
    public void setRequest(Request request) {
        this.request = request;
    }
    public OutputStream getOutput() {
        return this.output;
    }
    @Override
    public void flushBuffer() throws IOException {
    }
    @Override
    public int getBufferSize() {
        return 0;
    }
    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    @Override
    public String getContentType() {
        return null;
    }
    @Override
    public Locale getLocale() {
        return null;
    }
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }
    @Override
    public PrintWriter getWriter() throws IOException {
        writer = new PrintWriter(new OutputStreamWriter(output,getCharacterEncoding()), true);
        return writer;
    }
    @Override
    public boolean isCommitted() {
        return false;
    }
    @Override
    public void reset() {
    }
    @Override
    public void resetBuffer() {
    }
    @Override
    public void setBufferSize(int arg0) {
    }
    @Override
    public void setCharacterEncoding(String arg0) {
        this.characterEncoding = arg0;
    }
    @Override
    public void setContentLength(int arg0) {
    }
    @Override
    public void setContentLengthLong(long arg0) {
    }
    @Override
    public void setContentType(String arg0) {
    }
    @Override
    public void setLocale(Locale arg0) {
    }
}
```
同样的，这个 API 也提供了一大堆方法。Response 类里也因为实现接口，新增了许多接口实现方法，在目前这个阶段，我们只需要关注 getWriter() 这一个方法。

```java
public PrintWriter getWriter() throws IOException {
    writer = new PrintWriter(new OutputStreamWriter(output,getCharacterEncoding()), true);
    return writer;
}
```

看上述实现，在这之前我们用 byte[] 数组类型作为 output 的输出，这对业务程序员来说是不太便利的，因此我们现在支持往输出流里写入 String 字符串数据，于是就需要用到 PrintWriter 类。

可以看到这里调用了 getCharacterEncoding() 方法，一般常用的是 UTF-8，所以在调用 getWriter() 之前，一定要先调用 setCharacterEncoding() 设置字符集。

在 PrintWriter 构造函数中，我们目前设置了一个值为 true。这个值的含义为 autoflush，当为 true 时，println、printf 等方法会自动刷新输出流的缓冲。

## ServletProcessor 适配 Servlet 规范

当提供了 writer 后，我们着手改造 ServletProcessor，改造后如下所示：

```java
public class ServletProcessor {
    //返回串的模板，实际返回时替换变量
    private static String OKMessage = "HTTP/1.1 ${StatusCode} ${StatusName}\r\n"+
            "Content-Type: ${ContentType}\r\n"+
            "Server: minit\r\n"+
            "Date: ${ZonedDateTime}\r\n"+
            "\r\n";
    public void process(Request request, Response response) {
        String uri = request.getUri(); //获取URI
        //按照简单规则确定servlet名，认为最后一个/符号后的就是servlet名
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        URLClassLoader loader = null;
        PrintWriter writer = null;
        try {
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            //从全局变量HttpServer.WEB_ROOT中设置类的目录
            File classPath = new File(HttpServer.WEB_ROOT);
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        }
        catch (IOException e) {
            System.out.println(e.toString() );
        }
        //获取PrintWriter
        try {
            response.setCharacterEncoding("UTF-8");
            writer = response.getWriter();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        //加载servlet
        Class<?> servletClass = null;
        try {
            servletClass = loader.loadClass(servletName);
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
        
        //生成返回头
        String head = composeResponseHead();
        writer.println(head);
        Servlet servlet = null;
        try {  
            //调用servlet，由servlet写response体
            servlet = (Servlet) servletClass.newInstance();
            servlet.service(request, response);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        catch (Throwable e) {
            System.out.println(e.toString());
        }
    }
    //生成返回头，根据协议格式替换变量
    private String composeResponseHead() {
        Map<String,Object> valuesMap = new HashMap<>();
        valuesMap.put("StatusCode","200");
        valuesMap.put("StatusName","OK");
        valuesMap.put("ContentType","text/html;charset=UTF-8");
        valuesMap.put("ZonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now()));
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String responseHead = sub.replace(OKMessage);
        return responseHead;
    }
}
```
主要变化有 3 处:
- 使用 PrintWriter 接口替换了原来的 OutputStream。
- 在加载 Servlet 之前设置 characterEncoding 为 UTF-8，再获取 Writer。
- Writer 中设置了 autoflush，因此不再需要像原来一样手动设置 output.flush。

## HelloServlet 适配 Servlet 规范
最后则是调整用来测试的 HelloServlet，实现 Servlet 接口，在输出之前设置 characterEncoding。

```java
public class HelloServlet implements Servlet{
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        res.setCharacterEncoding("UTF-8");
        String doc = "<!DOCTYPE html> \n" +
                "<html>\n" +
                "<head><meta charset=\"utf-8\"><title>Test</title></head>\n"+
                "<body bgcolor=\"#f0f0f0\">\n" +
                "<h1 align=\"center\">" + "Hello World 你好" + "</h1>\n";
        res.getWriter().println(doc);
    }
    @Override
    public void destroy() {
    }
    @Override
    public ServletConfig getServletConfig() {
        return null;
    }
    @Override
    public String getServletInfo() {
        return null;
    }
    @Override
    public void init(ServletConfig arg0) throws ServletException {
    }
}
```

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。


键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。


我们再输入 http://localhost:8080/servlet/test.HelloServlet 就可以看到浏览器显示：Hello World 你好，这也是我们在 HelloServlet 中定义的返回资源内容。

servlet规范适配完成