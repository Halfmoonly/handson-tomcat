上节我们为HTTP请求实现了Servlet规范，本节对 Response 相关的代码适配Servlet规范

我们在前面提到过，HTTP 协议中规定 Response 返回格式由以下几部分组成：
- 状态行
- 响应头
- 空行
- 响应体

## HttpResponse适配Servlet规范
我们常用的状态码一般为 200、401、404、500、503、504 等，我们在 HttpResponse 里用 switch 条件语句先关联常用的状态码与状态信息。
```java
package server;
public class HttpResponse implements HttpServletResponse {
  protected String getStatusMessage(int status) {
    switch (status) {
        case SC_OK:
            return ("OK");
        case SC_ACCEPTED:
            return ("Accepted");
        case SC_BAD_GATEWAY:
            return ("Bad Gateway");
        case SC_BAD_REQUEST:
            return ("Bad Request");
        case SC_CONTINUE:
            return ("Continue");
        case SC_FORBIDDEN:
            return ("Forbidden");
        case SC_INTERNAL_SERVER_ERROR:
            return ("Internal Server Error");
        case SC_METHOD_NOT_ALLOWED:
            return ("Method Not Allowed");
        case SC_NOT_FOUND:
            return ("Not Found");
        case SC_NOT_IMPLEMENTED:
            return ("Not Implemented");
        case SC_REQUEST_URI_TOO_LONG:
            return ("Request URI Too Long");
        case SC_SERVICE_UNAVAILABLE:
            return ("Service Unavailable");
        case SC_UNAUTHORIZED:
            return ("Unauthorized");
        default:
            return ("HTTP Response Status " + status);
    }
}
```

接下来我们关注头部信息的操作，使用 headers 这样一个 concurrentHashMap 存储头部的键值信息，里面是 content-length 和 content-type 的时候，我们还会设置相关属性。代码如下：
```java
public class HttpResponse implements HttpServletResponse {
    Map<String, String> headers = new ConcurrentHashMap<>();
    @Override
    public void addHeader(String name, String value) {
        headers.put(name, value);
        if (name.toLowerCase() == DefaultHeaders.CONTENT_LENGTH_NAME) {
            setContentLength(Integer.parseInt(value));
        }
        if (name.toLowerCase() == DefaultHeaders.CONTENT_TYPE_NAME) {
            setContentType(value);
        }
    }
    
    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
        if (name.toLowerCase() == DefaultHeaders.CONTENT_LENGTH_NAME) {
            setContentLength(Integer.parseInt(value));
        }
        if (name.toLowerCase() == DefaultHeaders.CONTENT_TYPE_NAME) {
            setContentType(value);
        }
    }
}
```

有了内部的头部信息，我们还会提供一个 sendHeaders 方法，按照 HTTP 协议的规定拼接，包含状态行、头信息和空行，将 Header 打印出来，如下所示：
```java
public void sendHeaders() throws IOException {
    PrintWriter outputWriter = getWriter();
    //下面这一端是输出状态行
    outputWriter.print(this.getProtocol());
    outputWriter.print(" ");
    outputWriter.print(status);
    if (message != null) {
        outputWriter.print(" ");
        outputWriter.print(message);
    }
    outputWriter.print("\r\n");
    
    if (getContentType() != null) {
        outputWriter.print("Content-Type: " + getContentType() + "\r\n");
    }
    if (getContentLength() >= 0) {
        outputWriter.print("Content-Length: " + getContentLength() + "\r\n");
    }
    
    //输出头信息
    Iterator<String> names = headers.keySet().iterator();
    while (names.hasNext()) {
        String name = names.next();
        String value = headers.get(name);
        outputWriter.print(name);
        outputWriter.print(": ");
        outputWriter.print(value);
        outputWriter.print("\r\n");
    }
    
    //最后输出空行
    outputWriter.print("\r\n");
    outputWriter.flush();
}
```

## 项目结构调整

修改完 response 之后，自然地，我们在 HttpProcessor 中，将原来引用或初始化 Response 类的地方，全部用 HttpResponse 替代。
```java
HttpProcessor.java
  // create Response object
  HttpResponse response = new HttpResponse(output);
  response.setRequest(request);
```

同理，ServletProcessor 类 process 方法签名改造如下：用 HttpResponse 替代。
```java
public class ServletProcessor {
    public void process(HttpRequest request, HttpResponse response) {
  
    }
}
```

同理，StaticResourceProcessor 类 process 方法签名改造如下：用 HttpResponse 替代。
```java

public class StaticResourceProcessor {
    public void process(HttpRequest request, HttpResponse response) throws IOException {
      
    }
}
```

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

我们再输入 http://localhost:8080/servlet/test.HelloServlet 就可以看到浏览器显示：Hello World 你好，这也是我们在 HelloServlet 中定义的返回资源内容。
