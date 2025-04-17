package server;

import javax.servlet.ServletException;
import java.io.IOException;

public class ServletProcessor {
    private HttpConnector connector;

    public ServletProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    public void process(HttpRequest request, HttpResponse response) throws IOException, ServletException {
        this.connector.getContainer().invoke(request, response);
    }

}
