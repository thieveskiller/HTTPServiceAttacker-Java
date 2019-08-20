package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class WSCreator implements WebSocketCreator {

    WSConnection conn = new WSConnection();

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
	return conn;
    }

}
