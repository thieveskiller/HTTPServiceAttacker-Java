package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class WSConnection {
    private static final Logger logger = LogManager.getLogger("Websocket Server");

    public static Map<Integer, Session> sessions = new HashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
	if (session.isOpen()) {
	    logger.info(session.getRemoteAddress().toString() + " - " + "New connection");
	    if (sessions.get(session.hashCode()) != null) {
		logger.warn(session.getRemoteAddress().toString() + " - " + "Unable to store this session.");
		session.close(503, null);
		return;
	    }
	    session.getRemote().sendStringByFuture("{\"code\":0}");
	}
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
	logger.info(session.getRemoteAddress().toString() + " - " + "Connection closed");
	sessions.remove(session.hashCode());
    }

}
