package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WSServlet extends WebSocketServlet {

    /**
     *
     */
    private static final long serialVersionUID = -1402093932351364232L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().println(WSConnection.lastBroadcact);
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(10L * 60L * 1000L);
        factory.getPolicy().setAsyncWriteTimeout(10L * 1000L);
        factory.register(WSConnection.class);
    }

}
