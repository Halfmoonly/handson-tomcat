本节是连接层设计的最后一个章节：

现在我们对一个 Socket 的管理是这样的：建立一个 Socket，交给 Processor 处理，当 Processor 处理完毕后随即把这个 Socket 关闭。

这样也引出一个问题：一个网页的页面上可能有很多模块，每次都需要访问服务器拿到相应资源，导致本可以使用同一个 Socket 解决的问题，却需要创建多个 Socket，这是对资源的浪费。

所以最后这节课我们也来探讨一下用什么技术来解决这个问题。接下来我们一起来动手实现。

## Socket 重复使用
在 HTTP 协议 1.1 版本中支持了可持续的连接，而且是默认的方式，在 Request 请求头中可以展现。
```shell
connection: keep-alive
```
在持续的连接中，服务器不会关闭 Socket，这样一个网页的相关资源在发请求时可以共用一个 Socket。 所以在客户端和服务器端之间会有多次的请求流和返回流的交互。

这样又会产生一个新的问题，就是我们怎么知道什么时候应该关闭它呢？答案是传输完毕后，通过一个头信息告知对方可以关闭了。

```shell
connection: close
```

## 分块传输chunk
还有，对一次请求和返回的交互，对于动态生成的内容我们也无法获取 Content-Length 的数值，客户端怎么知道服务器返回的数据传完了呢？之前把这个值固定写死只适用于简单的返回固定内容的场景。

HTTP 协议也考虑到了这一点，在 1.1 版本中，采用了一个特殊的头部信息 transfer-encoding，来表明数据流采用分块（chunk）的方式进行发送传输。
```shell
Transfer-Encoding: chunked
```
你可以看一下传输的数据格式的定义。

```shell
[chunk size][\r\n][chunk data][\r\n][chunk size][\r\n][chunk data][\r\n] …… [chunk size = 0][\r\n][\r\n]
```
这种格式我们在这里简单解释一下。
1. 编码是由若干个块组成，由一个标明长度为 0 的块结束，也就是说，检测到 chunk size = 0 表示数据流已传输完毕。
2. 每个 chunk 有两部分组成，第一部分是这个分块的长度，第二部分则是前一部分指定长度对应的内容，每个部分用 CRLF 换行符隔开。
3. 结束时只标识 CRLF。

我们用下面这个示意图可以更好地展示数据传输的格式。
```shell
分块的长度 —— chunk size ｜ CRLF
分块的数据 —— chunk data ｜ CRLF
分块的长度 —— chunk size ｜ CRLF
分块的数据 —— chunk data ｜ CRLF
       ……  // 此处省略
分块的长度 —— chunk size ｜ CRLF
分块的数据 —— chunk data ｜ CRLF
分块的长度 —— 0 ｜ CRLF
结束标识符 —— CRLF
```
有了固定格式之后，就可以按照规定，一部分一部分地发送数据了。也因为分块的存在，我们今后就不需要考虑 content length 这个值了。

这里我给出了一个响应包的参考示例，你可以看一下。

```shell
HTTP/1.1 200 OK
Content-Type: text/plain
Transfer-Encoding: chunked

35
This is the data in the first chunk
26
and this is the second one
3
con
8
sequence
0
```

## Keep-alive程序改造(HttpProcessor)
其中的道理我们理解了之后，接下来我们就可以开始着手改造代码了。

在代码里面，我们只是在 Processor 中加上 Keep-alive 的判断，决定是否关闭 Socket。因为是一个粗浅的探讨，所以我们没有真的按照 chunk 的模式回送 response。后面我们也没有实现，探讨这一部分内容主要是为了了解原理。

你可以看一下需要调整的代码。HttpProcessor
```java
package server;
public class HttpProcessor implements Runnable{
    private Socket socket;
    private boolean available = false;
    private HttpConnector connector;
    private int serverPort = 0;
    private boolean keepAlive = false;
    private boolean http11 = true;

    public void process(Socket socket) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            keepAlive = true;
            while (keepAlive) {
                // create Request object and parse
                HttpRequest request = new HttpRequest(input);
                request.parse(socket);
                // handle session
                if (request.getSessionId() == null || request.getSessionId().equals("")) {
                    request.getSession(true);
                }
                // create Response object
                HttpResponse response = new HttpResponse(output);
                response.setRequest(request);
//               response.sendStaticResource();
                request.setResponse(response);
                try {
                    response.sendHeaders();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                // check if this is a request for a servlet or a static resource
                // a request for a servlet begins with "/servlet/"
                if (request.getUri().startsWith("/servlet/")) {
                    ServletProcessor processor = new ServletProcessor();
                    processor.process(request, response);
                }
                else {
                    StaticResourceProcessor processor = new StaticResourceProcessor();
                    processor.process(request, response);
                }
                finishResponse(response);
                System.out.println("response header connection------"+response.getHeader("Connection"));
                if ("close".equals(response.getHeader("Connection"))) {
                    keepAlive = false;
                }
            }
            // Close the socket
            socket.close();
            socket = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void finishResponse(HttpResponse response) {
        response.finishResponse();
    }
}
```

我们来分析一下这段代码，一个小变动在于新增了 serverPort、keepAlive 与 http11 三个域，而且为了更好的安全性，都用 private 关键字修饰。

而核心改动在于 process 方法，把里面的多数方法放置在 while 循环之中，使用 keepAlive 变量控制，如果检测到 response 的头部信息是 close，那么就把 keepAlive 设置成 false，退出循环，关闭 Socket。

## Keep-alive程序改造(客户请求HttpRequest)
而客户端也可以在请求头中加上 Connection: close 指定要关闭连接，因此 HttpRequest 的 parseHeaders 方法内也可以进行调整，现在 HttpRequest 中将 parseHeaders 方法调整成下面这个样子了。

增加了是否为 close 的判断，用来设置响应头。
```java
else if (name.equals(DefaultHeaders.CONNECTION_NAME)) {
    headers.put(name, value);
    if (value.equals("close")) {
        response.setHeader("Connection", "close");
    }
}
```

HttpRequest 还有其他调整：新增域response和 setter 方法，其他未改动的部分我就不在这里列出了。
```java
package server；
public class HttpRequest implements HttpServletRequest {
    private HttpResponse response;
    public HttpRequest() {
    }
    public void setStream(InputStream input) {
        this.input = input;
        this.sis = new SocketInputStream(this.input, 2048);
    }
    public void setResponse(HttpResponse response) {
        this.response = response;
    }   
}      
```

## Keep-alive程序改造(服务响应HttpResponse)
HttpResponse
```java
package server；
public class HttpResponse implements HttpServletResponse {
    String characterEncoding = "UTF-8";
    public HttpResponse() {
    }
    public void setStream(OutputStream output) {
        this.output = output;
    }
    //提供这个方法完成输出
    public void finishResponse() {
      try {
          this.getWriter().flush();
      } catch (IOException e) {
          e.printStackTrace();
      }
   }
}
```

而 HttpProcessor 类，就要调用 HttpResponse 中的 finishResponse 方法，这是因为我们修改了一下时序，在 ServletProcessor 中不管 header 头处理了，只调用 servlet.service(requestFacade, responseFacade) 这一方法。

```java
    public void process(Socket socket) {
        ...
        finishResponse(response);
        ...
    }
```
因此 ServletProcessor 就不再需要调用 response.sendHeaders 方法了


## Keep-alive程序改造(服务连接层HttpResponse)
过去我们在 ServletProcessor 中初始化 ClassLoader，现在把类加载器改写成全局可用的，把初始化放在 HttpConnector 里。

所以接下来我们要改写 ServletProcessor、HttpConnector 两个类。

HttpConnector 主要修改如下：
```java
package server;
public class HttpConnector implements Runnable {
    int minProcessors = 3;
    int maxProcessors = 10;
    int curProcessors = 0;
    Deque<HttpProcessor> processors = new ArrayDeque<>();
    public static Map<String, HttpSession> sessions = new ConcurrentHashMap<>();
    //一个全局的class loader
    public static URLClassLoader loader = null;
    public void run() {
        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        try {
            //class loader初始化
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(HttpServer.WEB_ROOT);
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        }
        catch (IOException e) {
            System.out.println(e.toString() );
        }
        // initialize processors pool
        for (int i = 0; i < minProcessors; i++) {
            HttpProcessor initprocessor = new HttpProcessor(this);
            initprocessor.start();
            processors.push(initprocessor);
        }
        curProcessors = minProcessors;
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                HttpProcessor processor = createProcessor();
                if (processor == null) {
                    socket.close();
                    continue;
                }
                processor.assign(socket);
                // Close the socket
//                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

HttpConnector 新增了 loader 变量，还将下面这段代码从 ServletProcessor 中移除了。
```java
        try {
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(HttpServer.WEB_ROOT);
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        }
        catch (IOException e) {
            System.out.println(e.toString());
        }
```

因此 ServletProcessor 的调整就比较简单了，将 servletClass 变量的赋值，直接交由 HttpConnector 处理，你可以看一下当前 ServletProcessor 的代码。
```java
package server;
public class ServletProcessor {
    public void process(HttpRequest request, HttpResponse response) {
        String uri = request.getUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        response.setCharacterEncoding("UTF-8");
        Class<?> servletClass = null;
        try {
            servletClass = HttpConnector.loader.loadClass(servletName);
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
        Servlet servlet = null;
        try {
            servlet = (Servlet) servletClass.newInstance();
            HttpRequestFacade requestFacade = new HttpRequestFacade(request);
            HttpResponseFacade responseFacade = new HttpResponseFacade(response);
            System.out.println("Call Service()");
            servlet.service(requestFacade, responseFacade);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        catch (Throwable e) {
            System.out.println(e.toString());
        }
    }
}
```
可以看到，这个类变简单了。

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

---

这节课我们还是在 src/test/java/test 目录下使用 TestServlet 进行测试，这一次我们改写 doGet 方法。

我们在 TestServlet 里面用一个静态全局计数器，如果是第三次以上，就把头部信息设置为 Connection:close 。这样从浏览器里可以看到，第一次第二次访问之后，数据返回但是浏览器连接没有停，第三次后才停下来。

这说明 keepAlive 参数生效了。但是我们要知道这些探讨都是粗浅的，代码只是演示了这个概念，我们没有完整实现 Keep-alive 以及 Chunked。
```java
package test;
public class TestServlet extends HttpServlet{
    static int count = 0;
    private static final long serialVersionUID = 1L;
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        System.out.println("Enter doGet()");
        System.out.println("parameter name : "+request.getParameter("name"));
        TestServlet.count++;
        System.out.println("::::::::call count ::::::::: " + TestServlet.count);
        if (TestServlet.count > 2) {
            response.addHeader("Connection", "close");
        }
        HttpSession session = request.getSession(true);
        String user = (String) session.getAttribute("user");
        System.out.println("get user from session : " + user);
        if (user == null || user.equals("")) {
            session.setAttribute("user", "yale");
        }
        response.setCharacterEncoding("UTF-8");
        String doc = "<!DOCTYPE html> \n" +
                "<html>\n" +
                "<head><meta charset=\"utf-8\"><title>Test</title></head>\n"+
                "<body bgcolor=\"#f0f0f0\">\n" +
                "<h1 align=\"center\">" + "Test 你好" + "</h1>\n";
        System.out.println(doc);
        response.getWriter().println(doc);
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        System.out.println("Enter doGet()");
        System.out.println("parameter name : "+request.getParameter("name"));
        response.setCharacterEncoding("UTF-8");
        String doc = "<!DOCTYPE html> \n" +
                "<html>\n" +
                "<head><meta charset=\"utf-8\"><title>Test</title></head>\n"+
                "<body bgcolor=\"#f0f0f0\">\n" +
                "<h1 align=\"center\">" + "Test 你好" + "</h1>\n";
        System.out.println(doc);
        response.getWriter().println(doc);
    }
}
```

接下来通过GET请求进行cookie和session的测试，外带keep-alive测试

初次请求不携带jsessionid
```shell
### 初次请求无jsessionid
GET http://localhost:8080/servlet/test.TestServlet?name=Tommy&docid=TS0001
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
Accept-Encoding: gzip, deflate, br, zstd
Accept-Language: zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7
Cache-Control: max-age=0
Connection: keep-alive
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

第次请求通过路径参数携带初次请求返回的jsessionid
```shell
### jsessionid位于uri中传递
GET http://localhost:8080/servlet/test.TestServlet;jsessionid=46FD59B1DB3F4BEF097713219C7DE39F?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```

前两次请求的返回日志如下，可见保证了第二次请求与第一次请求的连续性，同一人在访问

但是请求次数未达到3，因此这两次的响应头connection都为null
```shell
set cookie jsessionid string : jsessionid=46FD59B1DB3F4BEF097713219C7DE39F
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@4370cb1b
parameter name : Tommy
::::::::call count ::::::::: 1
get user from session : null
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

response header connection------null
set cookie jsessionid string : jsessionid=46FD59B1DB3F4BEF097713219C7DE39F
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@503c95b4
parameter name : Tommy
::::::::call count ::::::::: 2
get user from session : yale
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

response header connection------null
```
重复第二次请求（请求三次）；
```shell
### jsessionid位于uri中传递
GET http://localhost:8080/servlet/test.TestServlet;jsessionid=46FD59B1DB3F4BEF097713219C7DE39F?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```
最后一次打印如下，关闭了socket连接
```shell
set cookie jsessionid string : jsessionid=46FD59B1DB3F4BEF097713219C7DE39F
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@61efd92c
parameter name : Tommy
::::::::call count ::::::::: 3
get user from session : yale
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

response header connection------close
请求到达三次，服务端设置keepAlive------false------服务端即将关闭
```

本节总结：

本节我们简单探讨了一下 Keep-alive 和 chunked 模式，让同一个 Socket 可以用于多次访问，减少了 Socket 的连接和关闭。

但是我们实际实现中对这个的支持并不充分，后面也没有用到。