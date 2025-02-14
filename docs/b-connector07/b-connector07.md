上一节课我们完成了对 Request、Response 的 Header 信息解析，并且采用 Facade 模式封装了我们希望暴露给外界的方法体，避免被业务程序员直接调用实现类的内部方法。

我们这节课需要研究的部分如下:
1. HttpRequestLine：在实际的请求结构中，请求的 URI 后缀往往会带上请求参数，例如 /app1/servlet1?username=Tommy&docid=TS0001，路径和参数之间用“?”分隔，参数与参数之间使用“&”隔开。
2. 请求体解析。POST方法
3. 最后改造 SocketInputStream，由继承 InputStream 改为继承 ServletInputStream。使输入流适配Servlet规范

## GET请求 URI 请求参数的解析
前面我们做到了解析 RequestLine 第一行，包括请求方法、URI 与请求协议。但在实际请求中，URI 后面经常会增加请求参数。比如：

```shell
GET /app1/servlet1?username=Tommy&docid=TS0001 HTTP/1.1
```

在这种情况下，以问号分隔的 URI，前一部分是我们常说的请求地址，而后面则是请求的具体参数，接下来我们要把这部分的参数解析出来。

此前，在 HttpRequest 类的 parse() 方法中，我们已经用 this.sis.readRequestLine(requestLine) 这一行代码，获取到了 Request Line。但我们把整个地址都当作了 URI，因此有了下面这种写法。
```java
this.uri = new String(requestLine.uri,0,requestLine.uriEnd);
```

但现在我们需要截取一部分，将地址与参数分离，所以改写一下，新增 parseRequestLine 方法。
```java
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
private void parseRequestLine() {
    int question = requestLine.indexOf("?");
      if (question >= 0) {
          queryString=new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1);
          uri = new String(requestLine.uri, 0, question);
      } else {
          queryString = null;
          uri = new String(requestLine.uri, 0, requestLine.uriEnd);
      }
    }
}
```
parseRequestLine 方法比较简单，主要是判断路径里是否有问号，通过问号分隔，取出地址和参数。

## POST请求 请求体的解析
上述考虑的主要是 GET 请求的处理，而 POST 请求则一般把请求参数放入请求体之中。我们来看一个 POST 请求的示例。

```java
POST /test HTTP/1.1
Host: www.test.com
User-Agent: Mozilla/5.0(Windows; U; Windows NT 5.1; en-US; rv:1.7.6)Gecko/20050225 Firefox/1.0.1
Content-Type:application/x-www-form-urlencoded
Content-Length: 40
Connection: Keep-Alive

name=Professional%20Ajax&publisher=Wiley
```
我们可以看到，这种情况与 GET 请求类似，只是参数在请求体内，而且对 URL 做了 encode 编码，在这将一个空格转换成了 %20。

接下来我们在 HttpRequest 类里定义数据结构用来存储参数信息。
```java
    //既存储GET路径参数，也存储POST请求体参数
    protected Map<String, String[]> parameters = new ConcurrentHashMap<>();
```
注意其中的 value 是字符串数组，因为部分参数存在多个值与之对应，例如 options、checkbox 等

目前我们处理 POST 方法比较简单，只考虑文本类型。其实可以支持文本、二进制、压缩包，都是通过 Content-Type 指定。常见的有 application/json、application/xml 等。

还有 POST 可以混合，也就是 multipart/form-data 多部分，有的是文本，有的是二进制，比如图片之类的。我们现在也先暂时放到一边。

## 改造SocketInputStream
首先我们改造 SocketInputStream，由继承 InputStream 改为继承 ServletInputStream。使输入流适配Servlet规范，你可以看一下完整代码。

```java
package server;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;
public class SocketInputStream extends ServletInputStream {
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte SP = (byte) ' ';
    private static final byte HT = (byte) '\t';
    private static final byte COLON = (byte) ':';
    private static final int LC_OFFSET = 'A' - 'a';
    protected byte buf[];
    protected int count;
    protected int pos;
    protected InputStream is;
    public SocketInputStream(InputStream is, int bufferSize) {
        this.is = is;
        buf = new byte[bufferSize];
    }
    //按照格式解析请求行
    public void readRequestLine(HttpRequestLine requestLine)
            throws IOException {
        int chr = 0;
        do {
            try {
                chr = read();
            } catch (IOException e) {
            }
        } while ((chr == CR) || (chr == LF));
        pos--;
        int maxRead = requestLine.method.length;
        int readStart = pos;
        int readCount = 0;
        boolean space = false;
        //这里先获取请求的method
        while (!space) {
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }
        requestLine.uriEnd = readCount - 1;
        maxRead = requestLine.uri.length;
        readStart = pos;
        readCount = 0;
        space = false;
        boolean eol = false;
        //再获取请求的uri
        while (!space) {
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException("requestStream.readline.error");
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }
        requestLine.uriEnd = readCount - 1;
        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;
        //最后获取请求的协议
        while (!eol) {
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException("requestStream.readline.error");
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == CR) {
                // Skip CR.
            } else if (buf[pos] == LF) {
                eol = true;
            } else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }
        requestLine.protocolEnd = readCount;
    }
    //读头信息，格式是header name:value
    public void readHeader(HttpHeader header)
            throws IOException {
        int chr = read();
        if ((chr == CR) || (chr == LF)) { // Skipping CR
            if (chr == CR)
                read(); // Skipping LF
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos--;
        }
        // 读取header名
        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;
        boolean colon = false;
        //以:分隔，前面的字符认为是header name
        while (!colon) {
            // 我们处于内部缓冲区的末尾
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            if ((val >= 'A') && (val <= 'Z')) {
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount++;
            pos++;
        }
        header.nameEnd = readCount - 1;
        // 读取 header 值（可以多行）
        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;
        int crPos = -2;
        boolean eol = false;
        boolean validLine = true;
        //处理行，因为一个header的值有可能多行(一行的前面是空格或者制表符)，需要连续处理
        while (validLine) {
            boolean space = true;
            // Skipping spaces
            // Note : 只有前面的空格被跳过
            while (space) {
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException("requestStream.readline.error");
                    pos = 0;
                    readStart = 0;
                }
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }
            //一直处理到行结束
            while (!eol) {
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException("requestStream.readline.error");
                    pos = 0;
                    readStart = 0;
                }
                //回车换行表示行结束
                if (buf[pos] == CR) {
                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }
            //再往前读一个字符，如果是空格或制表符号则继续，多行处理的情况
            int nextChr = read();
            if ((nextChr != SP) && (nextChr != HT)) {
                pos--;
                validLine = false;
            } else {
                eol = false;
                header.value[readCount] = ' ';
                readCount++;
            }
        }
        header.valueEnd = readCount;
    }
    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return buf[pos++] & 0xff;
    }
    public int available() throws IOException {
        return (count - pos) + is.available();
    }
    public void close() throws IOException {
        if (is == null) {
            return;
        }
        is.close();
        is = null;
        buf = null;
    }
    protected void fill() throws IOException {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0) {
            count = nRead;
        }
    }
    @Override
    public boolean isFinished() {
        return false;
    }
    @Override
    public boolean isReady() {
        return false;
    }
    @Override
    public void setReadListener(ReadListener readListener) {
    }
}
```
## 改造HttpRequest

随后在 HttpRequest 的 parseParameters 方法内，我们就可以通过 getInputStream() 方法读取请求体内容。

通过 getInputStream 方法，一次性将字节流读入到 buf[]里，
```java
protected void parseParameters() {
    //设置字符集
    String encoding = getCharacterEncoding();
    if (encoding == null) {
        encoding = "ISO-8859-1";
    }
    //获取查询串
    String qString = getQueryString();
    if (qString != null) {
        byte[] bytes = new byte[qString.length()];
        try {
            bytes=qString.getBytes(encoding);
            parseParameters(this.parameters, bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();;
        }
    }
    //获取 content Type
    String contentType = getContentType();
    if (contentType == null)
        contentType = "";
    int semicolon = contentType.indexOf(';');
    if (semicolon >= 0) {
        contentType = contentType.substring(0, semicolon).trim();
    }
    else {
        contentType = contentType.trim();
    }
    //对POST方法，从body中解析参数
    if ("POST".equals(getMethod()) && (getContentLength() > 0)
            && "application/x-www-form-urlencoded".equals(contentType)) {
        try {
            int max = getContentLength();
            int len = 0;
            byte buf[] = new byte[getContentLength()];
            ServletInputStream is = getInputStream();
            while (len < max) {
                int next = is.read(buf, len, max - len);
                if (next < 0) {
                    break;
                }
                len += next;
            }
            is.close();
            if (len < max) {
                throw new RuntimeException("Content length mismatch");
            }
            parseParameters(this.parameters, buf, encoding);
        }
        catch (UnsupportedEncodingException ue) {
        }
        catch (IOException e) {
            throw new RuntimeException("Content read fail");
        }
    }
}

//十六进制字符到数字的转换
private byte convertHexDigit(byte b) {
    if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
    if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
    if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
    return 0;
}
public void parseParameters(Map<String,String[]> map, byte[] data, String encoding)
        throws UnsupportedEncodingException {
    if (parsed)
        return;
    if (data != null && data.length > 0) {
        int    pos = 0;
        int    ix = 0;
        int    ox = 0;
        String key = null;
        String value = null;
        //解析参数串，处理特殊字符
        while (ix < data.length) {
            byte c = data[ix++];
            switch ((char) c) {
                case '&':   //两个参数之间的分隔符，遇到这个字符保存已经解析的key和value
                    value = new String(data, 0, ox, encoding);
                    if (key != null) {
                        putMapEntry(map,key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case '=': //参数的key/value的分隔符
                    key = new String(data, 0, ox, encoding);
                    ox = 0;
                    break;
                case '+': //特殊字符，空格
                    data[ox++] = (byte)' ';
                    break;
                case '%': //处理%NN表示的ASCII字符
                    data[ox++] = (byte)((convertHexDigit(data[ix++]) << 4)
                            + convertHexDigit(data[ix++]));
                    break;
                default:
                    data[ox++] = c;
            }
        }
        //最后一个参数没有&结尾
        //The last value does not end in '&'.  So save it now.
        if (key != null) {
            value = new String(data, 0, ox, encoding);
            putMapEntry(map,key, value);
        }
    }
    parsed = true;
}
//给key设置新值，多值用数组来存储
private static void putMapEntry( Map<String,String[]> map, String name, String value) {
    String[] newValues = null;
    String[] oldValues = (String[]) map.get(name);
    if (oldValues == null) {
        newValues = new String[1];
        newValues[0] = value;
    } else {
        newValues = new String[oldValues.length + 1];
        System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
        newValues[oldValues.length] = value;
    }
    map.put(name, newValues);
}
```
然后统一通过重载方法 parseParameters(Map map, byte[] data, String encoding) 进行具体的解析工作 ，在这个方法中主要进行参数解析：依次读取 byte，在这个过程中判断 “&”“=”“+”等特殊字符。

而且对于“%20”这样经过 encode 的字符要特殊处理，我们要用十六进制还原它的字符。

```java
    private byte convertHexDigit(byte b) {
    if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
    if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
    if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
    return 0;
}
```

先拿到“2”这个字符，变成数字 2，再拿到“0”这个字符，变成数字 0，随后进行计算：2*16+0=32，再按照 ascii 变成字符，也就是空格。
```java
(byte)((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
```

最后我们完善 HttpRequest 类中与 parameter 相关的方法。

```java
  public String getParameter(String name) {
    parseParameters();
    String values[] = (String[]) parameters.get(name);
    if (values != null)
        return (values[0]);
    else
        return (null);  }

public Map<String, String[]> getParameterMap() {
    parseParameters();
    return (this.parameters);
}

public Enumeration<String> getParameterNames() {
    parseParameters();
    return (Collections.enumeration(parameters.keySet()));
}

public String[] getParameterValues(String name) {
    parseParameters();
    String values[] = (String[]) parameters.get(name);
    if (values != null)
        return (values);
    else
        return null;
}
```

这里我们初步完成了 HttpRequest 类里对请求参数 parameter 的解析(GET路径参数和POST请求体参数)，所有的处理都是在获取到具体参数的时候，才调用 parseParameters() 方法，把时序放到这里，是为了性能考虑。

## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

我们再输入 http://localhost:8080/servlet/test.TestServlet?name=Tommy&docid=TS0001 就可以看到浏览器显示：Test GET 你好，同时控制台也显示：
```shell
requestFacade = server.HttpRequestFacade@11a068e1
Enter doGet()
parameter name : Tommy
parameter docid : TS0001
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test GET 你好</h1>
```

测试POST请求体解析，手工写一个请求串，或者使用HTTP插件模拟如下POST请求，注意只支持x-www-form-urlencoded，不支持json
```
POST /servlet/test.TestServlet HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Cookie: jsessionid=43C65BC81B4B4DE4623CD48A13E7FF84; userId=123
Content-Length: 9

name=Professional%20Ajax&publisher=Wiley
```
其中%20表示空格

控制台返回
```shell
Enter doPost()
parameter body name : Professional Ajax
java.lang.NullPointerException
```
NPE原因是解析POST请求体中第一个参数name=Professional%20Ajax之后，就顺带关闭了SocketInputStream

HttpRequest#parseParameters()中执行了is.close();
```java
        //对POST方法，从body中解析参数
        if ("POST".equals(getMethod()) && (getContentLength() > 0)
        && "application/x-www-form-urlencoded".equals(contentType)) {
        try {
int max = getContentLength();
int len = 0;
byte buf[] = new byte[getContentLength()];
ServletInputStream is = getInputStream();
                while (len < max) {
int next = is.read(buf, len, max - len);
                    if (next < 0) {
        break;
        }
len += next;
                }
                        is.close();
                if (len < max) {
        throw new RuntimeException("Content length mismatch");
                }
parseParameters(this.parameters, buf, encoding);
            } catch (UnsupportedEncodingException ue) {
        } catch (IOException e) {
        throw new RuntimeException("Content read fail");
            }
                    }
```
导致解析POST请求体中第二个参数&publisher=Wiley的时候，遇到java.lang.NullPointerException

解决方案：HttpRequest#getParameter(String name)，补充if (parameters.isEmpty())判断
```java
    @Override
public String getParameter(String name) {
    if (parameters.isEmpty()) {
        parseParameters();
    }
    String values[] = parameters.get(name);
    if (values != null)
        return (values[0]);
    else
        return (null);
}
```

致此，POST请求体解析大功告成
```shell
requestFacade = server.HttpRequestFacade@2e006341
Enter doPost()
parameter body name : Professional Ajax
parameter body publisher : Wiley
<!DOCTYPE html> 
<html>
<head><meta charset="utf-8"><title>Test</title></head>
<body bgcolor="#f0f0f0">
<h1 align="center">Test POST 你好</h1>
```