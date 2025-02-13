

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