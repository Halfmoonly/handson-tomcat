提高connector的并发度有一系列技术。 能第一反应出来的就是池化+多线程：

将processor设计成多个线程，放到一个池子里面，服务器接受前端多个请求后交给后面线程池子里面的多个processor线程来并发处理。

这解决了一部分问题，因为对一个processor线程本身来说，它还是串行工作的，当它涉及到数据库访问网络访问文件操作的时候，可以进一步再分线程。不过程序模式需要调整成使用Future或者CompletableFuture,完全的响应式编程结构复杂。JDK21提出的virtual thread很好地解决了这个问题。实际工作中，要根据场景要求进行选择。

从本节起往后，我们逐步进行优化：
1. processor对象池
2. 将processor也设计为Runnable线程，同时实现connector线程和processor线程同步机制 //下节实现
3. processor内部异步化，如CompletableFuture、virtual thread //TODO

本节仅实现1. processor对象池，这有利于资源复用

之前我们在 HttpConnector 里获取到 Socket 后，每次都是创建一个全新的 HttpProcessor 对象，如下所示：
```java
socket = serverSocket.accept();
HttpProcessor processor = new HttpProcessor();
processor.process(socket);
```

在并发请求逐步增加后，构造新对象会对服务器的性能造成负担。

所以我们打算引入池，把对象初始化好之后，需要用的时候再拿出来使用，不需要使用的时候就再放回池里，不用再构造新的对象。

## 改造 HttpConnector
因此，接下来我们先改造 HttpConnector 类，使用 ArrayDeque 存放构造完毕的 HttpProcessor 对象。改造如下：

```java
public class HttpConnector implements Runnable {
    int minProcessors = 3;
    int maxProcessors = 10;
    int curProcessors = 0;
    //存放多个processor的池子
    Deque<HttpProcessor> processors = new ArrayDeque<>();
    public void run() {
        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // initialize processors pool
        for (int i = 0; i < minProcessors; i++) {
            HttpProcessor processor = new HttpProcessor();
            processors.push(processor);
        }
        curProcessors = minProcessors;
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                //得到一个新的processor，这个processor从池中获取(池中有可能新建)
                HttpProcessor processor = createProcessor();
                if (processor == null) {
                    socket.close();
                    continue;
                }
                processor.process(socket); //处理
                processors.push(processor); //处理完毕后放回池子
                // Close the socket
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
    //从池子中获取一个processor，如果池子为空且小于最大限制，则新建一个
    private HttpProcessor createProcessor() {
        synchronized (processors) {
            if (processors.size() > 0) {
                //获取一个
                return ((HttpProcessor) processors.pop());
            }
            if (curProcessors < maxProcessors) {
                //新建一个
                return (newProcessor());
            }
            else {
                return (null);
            }
        }
    }
    //新建一个processor
    private HttpProcessor newProcessor() {
        HttpProcessor initprocessor = new HttpProcessor();
        processors.push(initprocessor);
        curProcessors++;
        return ((HttpProcessor) processors.pop());
    }
}
```

从上述代码中，我们可以看到，run() 方法中先进行了 processors 池的初始化，就是循环创建新的 HttpProcessor 对象后，push 到 processors 这个 ArrayDeque 里，processors 作为 HttpProcessor 对象的存储池使用。

之后，每次接收到一个 socket，就用 createProcessor() 方法获取到一个 processor。看一下 createProcessor() 方法的代码，可以发现当 processors 内元素不为空的时候，直接从 ArrayDeque 内获取 HttpProcessor 对象，每次从池里获取一个 processor。如果池子里没有 processor，就新创建一个。

无论池子里有没有，最后的结果都是拿到了一个 processor，然后执行它，任务执行完之后再把它放回池里。完成了一次请求响应。


## 本节测试

同样在编写完毕HelloServlet后，我们需要单独编译这个类，生成 HelloServlet.class，把编译后的文件放到 /webroot/test 目录下，原因在于我们的服务器需要从 webroot 目录下获取资源文件。

键入 http://localhost:8080/hello.txt 后，可以发现 hello.txt 里所有的文本内容，都作为返回体展示在浏览器页面上了。

我们再输入 http://localhost:8080/servlet/test.HelloServlet 就可以看到浏览器显示：Hello World 你好，这也是我们在 HelloServlet 中定义的返回资源内容。

但是我们要注意，到现在为止，HttpProcessor 并没有做到多线程，也没有实现 NIO，只是在池中放置了多个对象，做到了多路复用。目前整体架构还是阻塞式运行的。

下一节我们会将processor也设计为Runnable线程，同时实现connector线程和processor线程同步机制