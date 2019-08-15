package com.ishland.app.HTTPServiceAttacker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;

public class App {
    private static final Logger logger = LogManager.getLogger("Launcher");
    private static Configuration config = null;

    public static void main(String[] args) {
	logger.info("Starting HTTP Service Attacker...");
	config = new Configuration();
    }
}
