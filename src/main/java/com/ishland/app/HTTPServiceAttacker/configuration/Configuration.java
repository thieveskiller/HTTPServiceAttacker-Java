package com.ishland.app.HTTPServiceAttacker.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.esotericsoftware.yamlbeans.YamlReader;

public class Configuration {
    private File configfile = new File(System.getProperty("user.dir") + "/config.yml");
    private static final Logger logger = LogManager.getLogger("Configuration loader");

    public Configuration() {
	logger.info("Loading configurations...");
	FileReader reader = null;
	try {
	    reader = new FileReader(configfile);
	} catch (FileNotFoundException e) {
	    logger.fatal("Unable to load configuration file, creating one and exit...", e);
	    createFile();
	    System.exit(0);
	}
	YamlReader conf = new YamlReader(reader);
    }

    public void createFile() {
	FileOutputStream out = null;
	InputStream confin = null;
	try {
	    configfile.createNewFile();
	    out = new FileOutputStream(configfile);
	    confin = Configuration.class.getClassLoader()
		    .getResourceAsStream("com/ishland/app/HTTPServiceAttacker/configuration/config.yml");
	} catch (IOException e) {
	    logger.fatal("Unable to create configuration file!", e);
	    return;
	}
	try {
	    while (confin.available() > 0)
		out.write(confin.read());
	} catch (IOException e) {
	    logger.fatal("Unable to write configuration file!", e);
	    System.exit(1);
	}
    }
}
