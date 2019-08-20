package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class WebServer {
    private static final Logger logger = LogManager.getLogger("Webserver Launcher");
    private static Server server;

    public static void run() {
	logger.info("Starting webserver...");
	server = new Server(8080);
	ServerConnector connector = new ServerConnector(server);
	server.addConnector(connector);

	WSHandler wsHandler = new WSHandler();
	ContextHandler wsContext = new ContextHandler();
	wsContext.setContextPath("/ws");
	wsContext.setHandler(wsHandler);
	server.setHandler(wsContext);

	ResourceHandler resourceHandler = new ResourceHandler();
	resourceHandler.setDirectoriesListed(false);
	resourceHandler.setWelcomeFiles(new String[] { "index.html" });
	resourceHandler.setResourceBase("./web/");
	server.insertHandler(resourceHandler);

	try {
	    server.start();
	} catch (Exception e) {
	    logger.error("Failed to start the webserver, the server will not start.");
	    return;
	}
	server.dumpStdErr();
    }

    public static void stop() {
	try {
	    server.stop();
	} catch (Exception e) {
	    logger.error("Failed to stop the webserver");
	}
    }

}
