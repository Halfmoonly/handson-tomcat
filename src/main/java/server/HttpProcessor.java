package server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor implements Runnable{
    Socket socket;
    boolean available = false;
    HttpConnector connector;
    public HttpProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        while (true) {
            // Wait for the next socket to be assigned
            Socket socket = await();

            if (socket == null) continue;

            // Process the request from this socket
            process(socket);

            // Finish up this request
            connector.recycle(this);

        }
    }
    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void process(Socket socket) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();

            // create Request object and parse
            HttpRequest request = new HttpRequest(input);
            request.parse(socket);

            // create Response object
            Response response = new Response(output);
            response.setRequest(request);
//               response.sendStaticResource();

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

            // Close the socket
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    synchronized void assign(Socket socket) {
        // wait for the connector to provide a new Socket
        while (available) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        // Store the newly available Socket and notify our thread
        this.socket = socket;
        available = true;
        notifyAll();
    }

    private synchronized Socket await() {
        // Wait for the Connector to provide a new Socket
        while (!available) {
            try {
                wait();
            }catch (InterruptedException e) {
            }
        }
        // Notify the Connector that we have received this Socket
        Socket socket = this.socket;
        available = false;
        notifyAll();

        return (socket);
    }
}
