package com.ishland.app.HTTPServiceAttacker.attacker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ishland.app.HTTPServiceAttacker.attacker.threads.AttackerThread;
import com.ishland.app.HTTPServiceAttacker.attacker.threads.MonitorThread;
import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;

public class Attack {

    private static Configuration config = null;
    private static ArrayList<AttackerThread> thrs = new ArrayList<>();
    private static MonitorThread monitorThread = null;
    private static final Logger logger = LogManager.getLogger("Attack manager");

    public static void startAttack() throws IllegalAccessException {
	logger.info("Starting attack...");
	if (getConfig() == null)
	    throw new IllegalAccessException("Configuration is not set!");
	monitorThread = new MonitorThread();
	monitorThread.start();
	Iterator<Map<String, Object>> itall = getConfig().getTarget().iterator();
	while (itall.hasNext()) {
	    Map<String, Object> itallmap = itall.next();
	    for (long i = 0; i < Long.valueOf(String.valueOf(itallmap.get("threads"))).longValue(); i++) {
		AttackerThread thr = new AttackerThread();
		if (itallmap.get("mode") == "POST")
		    thr.setMethod(AttackerThread.POST);
		else
		    thr.setMethod(AttackerThread.GET);
		thr.setTarget(String.valueOf(itallmap.get("addr")));
		thr.setData(String.valueOf(itallmap.get("data")));
		thr.setShowExceptions(getConfig().isShowExceptions());
		thr.start();
		thrs.add(thr);
	    }
	}
	logger.info("Attack started.");
    }

    public static void stopAttack() {
	logger.info("Sending stop signal to all attacker threads...");
	Iterator<AttackerThread> it = thrs.iterator();
	while (it.hasNext())
	    it.next().stopTask();
	logger.info("Waiting for all the threads to die...");
	it = thrs.iterator();
	while (it.hasNext()) {
	    AttackerThread thr = it.next();
	    try {
		thr.join();
	    } catch (InterruptedException e) {
		logger.warn("Could not wait for " + thr.getName() + " to die", e);
	    }
	}
	monitorThread.stopTask();
	try {
	    monitorThread.join();
	} catch (InterruptedException e) {
	    logger.warn("Could not wait for Monitor Thread to die", e);
	}
	logger.info("Attack stopped.");
    }

    /**
     * @return the config
     */
    public static Configuration getConfig() {
	return config;
    }

    /**
     * @param config the config to set
     */
    public static void setConfig(Configuration config) {
	Attack.config = config;
    }

}
