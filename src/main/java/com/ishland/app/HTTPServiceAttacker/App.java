package com.ishland.app.HTTPServiceAttacker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ishland.app.HTTPServiceAttacker.attacker.Attack;
import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;
import com.ishland.app.HTTPServiceAttacker.manager.webserver.WebServer;

public class App {
    private static final Logger logger = LoggerFactory.getLogger("Launcher");
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
		logger.error("Cannot shutdown JVM normally, the JVM is shutting down forcibly.");
		Runtime.getRuntime().halt(0);
	    }
	}));
	WebServer.run();
	try {
	    Attack.startAttack();
	} catch (IllegalAccessException e) {
	    logger.error("Failed to start attack", e);
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
