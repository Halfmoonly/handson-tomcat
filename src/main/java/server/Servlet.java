package server;

import java.io.IOException;

public interface Servlet {
    public void service(Request req, Response res) throws IOException;
}
