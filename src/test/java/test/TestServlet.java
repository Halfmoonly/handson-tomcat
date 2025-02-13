package test;

import server.HttpConnector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public class TestServlet extends HttpServlet{
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        System.out.println("Enter doGet()");
        System.out.println("parameter name : "+request.getParameter("name"));
        System.out.println("parameter docid : "+request.getParameter("docid"));
        HttpSession session = request.getSession(true);
        String user = (String) session.getAttribute("user");
        System.out.println("get user from session in server: " + user);
        if (user == null || user.equals("")) {
            System.out.println("gen user to session in server...");
            session.setAttribute("user", "Halfmoonly");
        }
        user = (String) session.getAttribute("user");
        System.out.println("get user from session in server: " + user);

        int i = 0;
        for (Map.Entry<String, HttpSession> entry: HttpConnector.sessions.entrySet()){
            String id = entry.getKey();
            HttpSession s = entry.getValue();
            System.out.println(i++ +"-->jsessionid= "+id+" session="+ s.getAttribute("user"));
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
        System.out.println("Enter doPost()");
        System.out.println("parameter name : "+request.getParameter("name"));
        System.out.println("parameter publisher : "+request.getParameter("publisher"));
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
