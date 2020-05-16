package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@WebSocket
public class WSConnection {
    private static final Logger logger = LoggerFactory.getLogger("Websocket Server");

    public static final List<WSConnection> sessions = new CopyOnWriteArrayList<>();

    public static String lastBroadcact = "";

    public Session currentSession;

    public static void boardcast(String str) {
        WSConnection.lastBroadcact = str;
        for (WSConnection session : sessions)
            session.currentSession.getRemote().sendStringByFuture(str);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        if (session.isOpen()) {
            logger.info(session.getRemoteAddress().toString() + " - " + "New connection");
            sessions.add(this);
            currentSession = session;
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, @SuppressWarnings("unused") String text) {
        session.getRemote().sendStringByFuture(lastBroadcact);
    }

    @OnWebSocketClose
    public void onClose(Session session, @SuppressWarnings("unused") int statusCode, @SuppressWarnings("unused") String reason) {
        logger.info(session.getRemoteAddress().toString() + " - " + "Connection closed");
        sessions.remove(this);
    }

}
