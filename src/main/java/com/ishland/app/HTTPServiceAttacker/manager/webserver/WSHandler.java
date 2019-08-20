package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WSHandler extends WebSocketHandler {

    @Override
    public void configure(WebSocketServletFactory factory) {
	factory.register(this.getClass());
	factory.getPolicy().setIdleTimeout(10L * 60L * 1000L);
	factory.getPolicy().setAsyncWriteTimeout(10L * 1000L);
    }

}
