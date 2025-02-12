package server;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpResponse implements HttpServletResponse {
    HttpRequest request;
    OutputStream output;
    PrintWriter writer;
    String contentType = null;
    long contentLength = -1;
    String charset = null;
    String characterEncoding = "utf-8";
    String protocol = "HTTP/1.1";
    //headers是一个保存头信息的map
    Map<String, String> headers = new ConcurrentHashMap<>();
    //默认返回OK
    String message = getStatusMessage(HttpServletResponse.SC_OK);
    int status = HttpServletResponse.SC_OK;

    public HttpResponse(OutputStream output) {
        this.output = output;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    //状态码以及消息文本，没有考虑国际化
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

    @Override
    public PrintWriter getWriter() throws IOException {
        writer = new PrintWriter(new OutputStreamWriter(output, getCharacterEncoding()), true);
        return writer;
    }

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

    public void setHeader(String name, String value) {
        headers.put(name, value);
        if (name.toLowerCase() == DefaultHeaders.CONTENT_LENGTH_NAME) {
            setContentLength(Integer.parseInt(value));
        }
        if (name.toLowerCase() == DefaultHeaders.CONTENT_TYPE_NAME) {
            setContentType(value);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }


    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public void setBufferSize(int arg0) {
    }

    @Override
    public void setCharacterEncoding(String arg0) {
    }

    @Override
    public void setContentLength(int arg0) {
    }

    @Override
    public void setContentLengthLong(long arg0) {
    }

    @Override
    public void setContentType(String arg0) {
    }

    @Override
    public void setLocale(Locale arg0) {
    }

    @Override
    public void addCookie(Cookie arg0) {
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
    }

    @Override
    public boolean containsHeader(String arg0) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String arg0) {
        return null;
    }

    @Override
    public String encodeURL(String arg0) {
        return null;
    }

    @Override
    public String encodeUrl(String arg0) {
        return null;
    }

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String arg0) {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void sendError(int arg0) throws IOException {
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
    }

    @Override
    public void setStatus(int arg0) {
    }

    @Override
    public void setStatus(int arg0, String arg1) {
    }
    public HttpRequest getRequest() {
        return request;
    }

    public OutputStream getOutput() {
        return output;
    }

    public void setOutput(OutputStream output) {
        this.output = output;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
