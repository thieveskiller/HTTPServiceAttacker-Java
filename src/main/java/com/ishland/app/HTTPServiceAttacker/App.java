package com.ishland.app.HTTPServiceAttacker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ishland.app.HTTPServiceAttacker.attacker.Attack;
import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;
import com.ishland.app.HTTPServiceAttacker.manager.webserver.WebServer;

public class App {
    private static final Logger logger = LogManager.getLogger("Launcher");
    private static Configuration config = null;

    public static void main(String[] args) {
	logger.info("Starting HTTP Service Attacker...");
	config = new Configuration();
	if (!config.isSuccess())
	    System.exit(1);
	logger.info("Initializing attackers...");
	Attack.setConfig(config);
	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	    public void run() {
		App.shutdown(true);
		try {
		    Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
		}
		logger.fatal("Cannot shutdown JVM normally, the JVM is shutting down forcibly.");
		Runtime.getRuntime().halt(0);
	    }
	}));
	WebServer.run();
	try {
	    Attack.startAttack();
	} catch (IllegalAccessException e) {
	    logger.fatal("Failed to start attack", e);
	    System.exit(2);
	}
    }

    public static void shutdown(boolean isInShutdownHook) {
	Attack.stopAttack();
	WebServer.stop();
	if (!isInShutdownHook)
	    System.exit(0);
    }
}
