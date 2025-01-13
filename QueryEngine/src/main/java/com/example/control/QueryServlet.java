package com.example.control;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

public class QueryServlet extends HttpServlet {
    private final QueryProcessor queryProcessor;

    public QueryServlet(QueryProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("word");
        if (query == null || query.isEmpty()) {
            resp.setContentType("text/html");
            resp.getWriter().write("<h1>Error: Missing 'word' parameter</h1>");
            return;
        }

        Map<String, Object> result = queryProcessor.processQuery(query);
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<html><body>");
        out.println("<h1>Search Results for: " + query + "</h1>");

        if (result.containsKey("message")) {
            out.println("<p>" + result.get("message") + "</p>");
        } else {
            List<Map<String, Object>> books = (List<Map<String, Object>>) result.get("results");
            for (Map<String, Object> book : books) {
                out.println("<h3>Book: " + book.get("Title") + "</h3>");
                out.println("<p>Author: " + book.get("Author") + "</p>");
                out.println("<ul>");
                for (String line : (List<String>) book.get("Lines")) {
                    out.println("<li>" + line + "</li>");
                }
                out.println("</ul>");
            }
        }

        out.println("</body></html>");
    }
}
