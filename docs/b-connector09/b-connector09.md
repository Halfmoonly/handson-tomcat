上节课我们已经实现对 URI 里路径的解析，用于适配 GET 请求时，将参数代入请求地址的情况，而且在请求参数中引入了 Cookie 与 Session，为 HTTP 引入状态，存储用户的相关信息。

但我也提到了，我们暂未在 Response 返回参数中回写 Session 信息，所以客户端程序没办法接受这个信息，自然也无法再回传给 Server，这是我们接下来要改造的方向。

## 项目结构

这节课我们先只引入了一个工具类 CookieTools，用来处理 Cookie，其余项目结构并没有发生改变，你可以参考我给出的目录。
```shell
MiniTomcat
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  ├─ server
│  │  │  │  ├─ CookieTools.java
│  │  │  │  ├─ DefaultHeaders.java
│  │  │  │  ├─ HttpConnector.java
│  │  │  │  ├─ HttpHeader.java
│  │  │  │  ├─ HttpProcessor.java
│  │  │  │  ├─ HttpRequest.java
│  │  │  │  ├─ HttpRequestFacade.java
│  │  │  │  ├─ HttpRequestLine.java
│  │  │  │  ├─ HttpResponse.java
│  │  │  │  ├─ HttpResponseFacade.java
│  │  │  │  ├─ HttpServer.java
│  │  │  │  ├─ Request.java
│  │  │  │  ├─ Response.java
│  │  │  │  ├─ ServletProcessor.java
│  │  │  │  ├─ Session.java
│  │  │  │  ├─ SessionFacade.java
│  │  │  │  ├─ SocketInputStream.java
│  │  │  │  ├─ StatisResourceProcessor.java
│  │  ├─ resources
│  ├─ test
│  │  ├─ java
│  │  │  ├─ test
│  │  │  │  ├─ HelloServlet.java
│  │  │  │  ├─ TestServlet.java
│  │  ├─ resources
├─ webroot
│  ├─ test
│  │  ├─ HelloServlet.class
│  │  ├─ TestServlet.class
│  ├─ hello.txt
├─ pom.xml
```

## 提供响应头 Cookie 处理工具类
接下来我们开始一步步改造，让 Response 也拥有状态。

首先是添加 CookieTools.java 类，这个类主要用于给响应头提供 Cookie 处理工具类。你可以看一下示例代码。
```java
public class CookieTools {
    public static String getCookieHeaderName(Cookie cookie) {
        return "Set-Cookie";
    }
    public static void getCookieHeaderValue(Cookie cookie, StringBuffer buf) {
        String name = cookie.getName();
        if (name == null)
            name = "";
        String value = cookie.getValue();
        if (value == null)
            value = "";
        buf.append(name);
        buf.append("=");
        buf.append(value);
    }
    static void maybeQuote (int version, StringBuffer buf,String value){
        if (version == 0 || isToken (value))
            buf.append (value);
        else {
            buf.append ('"');
            buf.append (value);
            buf.append ('"');
        }
    }
    private static final String tspecials = "()<>@,;:\\\"/[]?={} \t";
    private static boolean isToken (String value) {
        int len = value.length ();
        for (int i = 0; i < len; i++) {
            char c = value.charAt (i);
            if (c < 0x20 || c >= 0x7f || tspecials.indexOf (c) != -1)
                return false;
        }
        return true;
    }
}
```

## 改造HttpRequest
为了更好地适配对 HTTP 协议的解析，DefaultHeaders 类和 HttpRequest 类中的代码也要一并调整。首先是调整 DefaultHeaders 类中，COOKIE_NAME 与 JSESSIONID_NAME 的值。
```java
package server;
public class DefaultHeaders {
    static final String COOKIE_NAME = "cookie";
    static final String JSESSIONID_NAME = "jsessionid";
}
```

对应 HttpRequest 类中 parseRequestLine 方法也要进行调整。
```java
package server;
public class HttpRequest implements HttpServletRequest {
    private void parseRequestLine() {
        int question = requestLine.indexOf("?");
        if (question >= 0) {
            queryString = new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1);
            uri = new String(requestLine.uri, 0, question);
            String tmp = ";" + DefaultHeaders.JSESSIONID_NAME + "=";
            int semicolon = uri.indexOf(tmp);
            if (semicolon >= 0) {
                sessionid = uri.substring(semicolon+tmp.length());
                uri = uri.substring(0, semicolon);
            }
        } else {
            queryString = null;
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
            String tmp = ";" + DefaultHeaders.JSESSIONID_NAME + "=";
            int semicolon = uri.indexOf(tmp);
            if (semicolon >= 0) {
                sessionid = uri.substring(semicolon+tmp.length());
                uri = uri.substring(0, semicolon);
            }
        }
    }
}
```

相比之前，主要调整了下面这段解析。
```java
  uri = new String(requestLine.uri, 0, requestLine.uriEnd);
  String tmp = ";" + DefaultHeaders.JSESSIONID_NAME + "=";
  int semicolon = uri.indexOf(tmp);
  if (semicolon >= 0) {
    sessionid = uri.substring(semicolon+tmp.length());
    uri = uri.substring(0, semicolon);
  }
```

还有在 parseHeaders 方法中，获取到 header 的 name 之后，增加两行转换代码，确保 header 都以小写字母进行比较处理。
```java
String name = new String(header.name,0,header.nameEnd);
String value = new String(header.value, 0, header.valueEnd);
name = name.toLowerCase();
```
上述是我们要做的些许前置准备工作，接下来让我们把目光投向 HttpResponse 类，看看如何将 Request 请求内获取的 Cookie 与 Server 生成的 Session 传入 Response 返回参数内，让 Client 也能获取到。

## 有状态的 Response
HttpResponse 返回类中需要调整的核心方法是 sendHeaders，通过这个方法把参数设置到 Response 返回头内

根据 Servlet 规范，Response 的 Header 中 Set-Cookie 需要满足下面的格式，任选一种即可
- Set-Cookie: <cookie-name>=<cookie-value>
- Set-Cookie: <cookie-name>=<cookie-value>; Expires=<date>
- Set-Cookie: <cookie-name>=<cookie-value>; Max-Age=<number>
- Set-Cookie: <cookie-name>=<cookie-value>; Domain=<domain-value>
- Set-Cookie: <cookie-name>=<cookie-value>; Path=<path-value>
- Set-Cookie: <cookie-name>=<cookie-value>; Secure

你可以看一下调整后的代码，未改变的部分没有在这里列举出来。

```java
package server;
public class HttpResponse implements HttpServletResponse {
    ArrayList<Cookie> cookies = new ArrayList<>();
    
    public void sendHeaders() throws IOException {
        PrintWriter outputWriter = getWriter();
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
        Iterator<String> names = headers.keySet().iterator();
        while (names.hasNext()) {
            String name = names.next();
            String value = headers.get(name);
            outputWriter.print(name);
            outputWriter.print(": ");
            outputWriter.print(value);
            outputWriter.print("\r\n");
        }
        HttpSession session = this.request.getSession(false);
        if (session != null) {
            Cookie cookie = new Cookie(DefaultHeaders.JSESSIONID_NAME, session.getId());
            cookie.setMaxAge(-1);
            addCookie(cookie);
        }
        synchronized (cookies) {
            Iterator<Cookie> items = cookies.iterator();
            while (items.hasNext()) {
                Cookie cookie = (Cookie) items.next();
                outputWriter.print(CookieTools.getCookieHeaderName(cookie));
                outputWriter.print(": ");
                StringBuffer sbValue = new StringBuffer();
                CookieTools.getCookieHeaderValue(cookie, sbValue);
                System.out.println("set cookie jsessionid string : "+sbValue.toString());
                outputWriter.print(sbValue.toString());
                outputWriter.print("\r\n");
            }
        }
        outputWriter.print("\r\n");
        outputWriter.flush();
   }
   
    @Override
    public void addCookie(Cookie cookie) {
      synchronized (cookies) {
          cookies.add(cookie);
      }
}
   // 省略其他 getter 和 setter 方法
```
在当前的 Server 中，我们就使用了最基本的格式：Set-Cookie: <cookie-name>=<cookie-value>。参考设置如下：
```java
Set-Cookie: jsessionid=FA73014B317A489994D0B394F4EBF4EA
```

就像上述 sendHeaders 方法里代码展示的，我们增加了一段代码，在 Set-Cookie 中把 Session 信息带入进去。
```java
HttpSession session = this.request.getSession(false);
if (session != null) {
    Cookie cookie = new Cookie(DefaultHeaders.JSESSIONID_NAME, session.getId());
    cookie.setMaxAge(-1);
    addCookie(cookie);
}
synchronized (cookies) {
    Iterator<Cookie> items = cookies.iterator();
    while (items.hasNext()) {
        Cookie cookie = (Cookie) items.next();
        outputWriter.print(CookieTools.getCookieHeaderName(cookie));
        outputWriter.print(": ");
        StringBuffer sbValue = new StringBuffer();
        CookieTools.getCookieHeaderValue(cookie, sbValue);
        System.out.println("set cookie jsessionid string : "+sbValue.toString());
        outputWriter.print(sbValue.toString());
        outputWriter.print("\r\n");
    }
}
```
这样，我们就做到了在返回给客户端的响应头中的Set-Cookie信息中带有jsessionid，

之后如果客户端再发请求，就可以把jsessionid通过cookie或者路径参数的形式再回传给服务器，服务器通过map映射关系直接拿到对应的session，这样就可以把多次没有上下文关联的 HTTP 访问打包成同一个用户访问。

## 本节测试
同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

接下来通过GET请求进行cookie和session的测试

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
二次请求通过路径参数携带初次请求返回的jsessionid
```shell
### jsessionid位于uri中传递
GET http://localhost:8080/servlet/test.TestServlet;jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2?name=Tommy&docid=TS0001
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
```
两次请求的返回日志如下，可见保证了第二次请求与第一次请求的连续性，同一人在访问
```shell
Connected to the target VM, address: '127.0.0.1:54546', transport: 'socket'
set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@1e39187b
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: null
gen user to session in server...
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@2105529d
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: Halfmoonly
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>
```
换一种传递jsessionid的方式，客户端通过Cookie传递jsessionid
```shell
set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@ac53d3e
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: Halfmoonly
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>
```
日志输出如下，证明以上三次请求返回的结果均是同一人的请求访问，保证了响应的有状态性
```shell
C:\Users\hasee\.jdks\corretto-17.0.13\bin\java.exe -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:54546,suspend=y,server=n -agentpath:C:\Users\hasee\AppData\Local\Temp\idea_libasyncProfiler_dll_temp_folder2\libasyncProfiler.dll=version,jfr,event=wall,interval=10ms,cstack=no,file=C:\Users\hasee\IdeaSnapshots\HttpServer_2025_02_13_202809.jfr,dbghelppath=C:\Users\hasee\AppData\Local\Temp\idea_dbghelp_dll_temp_folder\dbghelp.dll,log=C:\Users\hasee\AppData\Local\Temp\HttpServer_2025_02_13_202809.jfr.log.txt,logLevel=DEBUG -javaagent:C:\Users\hasee\AppData\Local\JetBrains\IntelliJIdea2024.3\captureAgent\debugger-agent.jar -Dkotlinx.coroutines.debug.enable.creation.stack.trace=false -Ddebugger.agent.enable.coroutines=true -Dkotlinx.coroutines.debug.enable.flows.stack.trace=true -Dkotlinx.coroutines.debug.enable.mutable.state.flows.stack.trace=true -Dfile.encoding=UTF-8 -classpath "E:\github\handson-tomcat\target\classes;C:\Users\hasee\.m2\repository\org\apache\commons\commons-lang3\3.4\commons-lang3-3.4.jar;C:\Users\hasee\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar;C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.1.1\lib\idea_rt.jar" server.HttpServer
Connected to the target VM, address: '127.0.0.1:54546', transport: 'socket'
set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@1e39187b
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: null
gen user to session in server...
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@2105529d
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: Halfmoonly
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>

set cookie jsessionid string : jsessionid=A9DC55E4AF056AA9174EE0E63F8796C2
Call Service()
Enter doGet()
null
getQueryString:name=Tommy&docid=TS0001
[B@ac53d3e
parameter name : Tommy
null
getQueryString:name=Tommy&docid=TS0001
parameter docid : TS0001
get user from session in server: Halfmoonly
get user from session in server: Halfmoonly
0-->jsessionid= A9DC55E4AF056AA9174EE0E63F8796C2 session=Halfmoonly
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test 你好</h1>


```