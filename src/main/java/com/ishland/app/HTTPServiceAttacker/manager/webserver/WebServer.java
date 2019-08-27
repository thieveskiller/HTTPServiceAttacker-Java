package com.ishland.app.HTTPServiceAttacker.manager.webserver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger("Webserver Launcher");
    private static Server server;

    public static void run() {
	logger.info("Starting webserver...");
	server = new Server(8080);
	ServerConnector connector = new ServerConnector(server);
	server.addConnector(connector);

	ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
	servlet.setContextPath("/");
	server.setHandler(servlet);

	URL f = WebServer.class.getClassLoader().getResource("web");
	if (f == null) {
	    throw new RuntimeException("Unable to find resource directory");
	}
	URI webRootUri = null;
	try {
	    webRootUri = f.toURI();
	} catch (URISyntaxException e1) {
	    throw new RuntimeException(e1);
	}
	logger.info("WebRoot is " + webRootUri.toString());
	try {
	    servlet.setBaseResource(Resource.newResource(webRootUri));
	} catch (MalformedURLException e1) {
	    throw new RuntimeException(e1);
	}
	servlet.setWelcomeFiles(new String[] { "index.html" });

	ServletHolder holderEvents = new ServletHolder("ws", WSServlet.class);
	holderEvents.setInitParameter("dirAllowed", "true");
	holderEvents.setInitOrder(0);
	servlet.addServlet(holderEvents, "/ws/*");

	ServletHolder holderDef = new ServletHolder("default", DefaultServlet.class);
	holderDef.setInitParameter("dirAllowed", "true");
	servlet.addServlet(holderDef, "/");

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
	    server.join();
	} catch (Exception e) {
	    logger.error("Failed to stop the webserver");
	}
    }

}
