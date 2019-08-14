package com.ishland.app.HTTPServiceAttacker.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.esotericsoftware.yamlbeans.YamlReader;

public class Configuration {
    private File configfile = new File(System.getProperty("user.dir") + "/config.yml");

    public Configuration() {
	FileReader reader = null;
	try {
	    reader = new FileReader(configfile);
	} catch (FileNotFoundException e) {

	}
	YamlReader conf = new YamlReader(reader);
    }
}
