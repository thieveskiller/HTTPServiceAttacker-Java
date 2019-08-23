package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class WSConnection {
    private static final Logger logger = LogManager.getLogger("Websocket Server");

    public static List<Session> sessions = new CopyOnWriteArrayList<>();

    public static String lastBroadcact = "";

    @OnWebSocketConnect
    public void onConnect(Session session) {
	if (session.isOpen()) {
	    logger.info(session.getRemoteAddress().toString() + " - " + "New connection");
	    sessions.add(session);
	}
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String text) {
	session.getRemote().sendStringByFuture(lastBroadcact);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
	logger.info(session.getRemoteAddress().toString() + " - " + "Connection closed");
	sessions.remove(session);
    }

    public static void boardcast(String str) {
	WSConnection.lastBroadcact = str;
	for (Session session : sessions)
	    session.getRemote().sendStringByFuture(str);
    }

}
