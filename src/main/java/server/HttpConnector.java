package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 持有对象池：processors
 */
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