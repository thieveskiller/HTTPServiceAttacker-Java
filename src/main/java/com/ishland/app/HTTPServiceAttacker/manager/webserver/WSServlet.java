package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WSServlet extends WebSocketServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -1402093932351364232L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	response.getWriter().println("HTTP GET method not implemented.");
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
	factory.getPolicy().setIdleTimeout(10L * 60L * 1000L);
	factory.getPolicy().setAsyncWriteTimeout(10L * 1000L);
	factory.register(WSConnection.class);
    }

}
