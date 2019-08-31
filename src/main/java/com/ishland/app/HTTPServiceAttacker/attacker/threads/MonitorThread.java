package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ishland.app.HTTPServiceAttacker.attacker.Attack;
import com.ishland.app.HTTPServiceAttacker.manager.WSContent;
import com.ishland.app.HTTPServiceAttacker.manager.webserver.WSConnection;

public class MonitorThread extends Thread {

    private static BlockingQueue<HttpResponse> queue = new LinkedBlockingQueue<>();
    private boolean isStopping = false;
    private static Gson gson;

    /*
     * private static Map<Integer, Long> success = new HashMap<>(); private static
     * long successcount = 0; private static Map<Integer, Long> failure = new
     * HashMap<>(); private static long failurecount = 0; private static long
     * errored = 0; private static long timeReqs = 0; private static long
     * timeReqsNoFail = 0; private static long timeEl = 0;
     */

    public static WSContent wsContent = null;
    private static long timeReqs = 0;
    private static long timeReqsNoFail = 0;
    private static long timeEl = 0;
    private static long timeCre = 0;

    public static TimerTask logging;

    private static List<Callable<Object>> listCallbacks = new ArrayList<>();

    public static synchronized void newError(Exception e) {
	wsContent.errored++;
	timeReqs++;
	if (!wsContent.errors.containsKey(e.getMessage()))
	    wsContent.errors.put(e.getMessage(), 1L);
	else
	    wsContent.errors.put(e.getMessage(), wsContent.errors.get(e.getMessage()) + 1L);
    }

    public static synchronized void newCreation() {
	timeCre++;
    }

    public static synchronized void pushResult(HttpResponse httpResponse)
	    throws InterruptedException, NullPointerException, IllegalArgumentException {
	queue.put(httpResponse);
    }

    public static boolean registerCallback(Callable<Object> callback) {
	return listCallbacks.add(callback);
    }

    private void createTimer() {
	wsContent = new WSContent();
	logging = new TimerTask() {
	    private final Logger logger = LoggerFactory.getLogger("Monitor Timer");

	    @Override
	    public void run() {
		if (((float) (System.currentTimeMillis() - timeEl) / 1000.0) < 0.1)
		    return;
		wsContent.vaildRPS = (float) (timeReqsNoFail)
			/ ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
		wsContent.totalRPS = (float) (timeReqs) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
		wsContent.creationSpeed = (long) ((timeCre) / ((float) (System.currentTimeMillis() - timeEl) / 1000));
		wsContent.maxVaildRPS = wsContent.maxVaildRPS.doubleValue() > wsContent.vaildRPS.doubleValue()
			? wsContent.maxVaildRPS.doubleValue()
			: wsContent.vaildRPS.doubleValue();
		wsContent.maxTotalRPS = wsContent.maxTotalRPS.doubleValue() > wsContent.totalRPS.doubleValue()
			? wsContent.maxTotalRPS.doubleValue()
			: wsContent.totalRPS.doubleValue();
		wsContent.maxCreationSpeed = wsContent.maxCreationSpeed.longValue() > wsContent.creationSpeed
			.longValue() ? wsContent.maxCreationSpeed.longValue() : wsContent.creationSpeed.longValue();
		wsContent.freeHeap = Runtime.getRuntime().freeMemory();
		wsContent.allocatedHeap = Runtime.getRuntime().totalMemory();
		wsContent.freeHeap = wsContent.allocatedHeap.longValue() - wsContent.freeHeap.longValue();
		wsContent.maxHeap = Runtime.getRuntime().maxMemory();
		wsContent.createdConnections = AttackerThread.openedCount;

		logger.info("Total: " + String.valueOf(wsContent.successcount.longValue()
			+ wsContent.failurecount.longValue() + wsContent.errored.longValue()));
		logger.info("Success info: " + wsContent.success.toString());
		logger.info("Failure info: " + wsContent.failure.toString() + " + " + wsContent.errored.toString()
			+ " exceptions");
		Iterator<Entry<String, Long>> ita = wsContent.errors.entrySet().iterator();
		while (ita.hasNext()) {
		    Entry<String, Long> entry = ita.next();
		    logger.info(entry.getKey() + ": " + entry.getValue());
		}
		logger.info("RPS: " + String.valueOf(wsContent.vaildRPS) + "/" + String.valueOf(wsContent.totalRPS));
		logger.info("RPM: " + String.valueOf(wsContent.vaildRPS.longValue() * 60) + "/"
			+ String.valueOf(wsContent.totalRPS.longValue() * 60));
		logger.info("Queued requests: " + String.valueOf(wsContent.createdConnections) + "/"
			+ String.valueOf(wsContent.maxAllowedConnections));
		logger.info("--------------------------------");
		timeReqs = 0;
		timeReqsNoFail = 0;
		timeEl = System.currentTimeMillis();
		String json = "";
		try {
		    json = gson.toJson(wsContent);
		} catch (IllegalArgumentException e) {
		    logger.warn("Error while searlizing json: " + e.getMessage());
		}
		WSConnection.boardcast(json);
		Iterator<Callable<Object>> it = listCallbacks.iterator();
		while (it.hasNext())
		    try {
			it.next().call();
		    } catch (Exception e) {
			logger.warn("Cannot pass onMonitorRefresh event", e);
		    }
	    }

	};
    }

    public void run() {
	GsonBuilder gsona = new GsonBuilder();
	gsona.serializeSpecialFloatingPointValues();
	gson = gsona.create();
	createTimer();
	new Timer().schedule(logging, 0, 500);
	int totalthreads = 0;
	for (int i = 0; i < Attack.getConfig().getTarget().size(); i++)
	    totalthreads += Integer.valueOf(String.valueOf(Attack.getConfig().getTarget().get(i).get("threads")))
		    .intValue();
	wsContent.maxAllowedConnections = (long) (totalthreads * Attack.maxConnectionPerThread);
	while (!isStopping) {
	    HttpResponse result = null;
	    try {
		result = queue.poll(100, TimeUnit.MILLISECONDS);
	    } catch (InterruptedException e) {
		continue;
	    }
	    if (result == null)
		continue;
	    timeReqs++;
	    timeReqsNoFail++;
	    if (result.getCode() >= 200 && result.getCode() < 300) {
		if (wsContent.success.get(result.getCode()) == null)
		    wsContent.success.put(result.getCode(), 1L);
		wsContent.success.put(result.getCode(), wsContent.success.get(result.getCode()).longValue() + 1);
		wsContent.successcount++;
	    } else {
		if (wsContent.failure.get(result.getCode()) == null)
		    wsContent.failure.put(result.getCode(), 1L);
		wsContent.failure.put(result.getCode(), wsContent.failure.get(result.getCode()).longValue() + 1);
		wsContent.failurecount++;
	    }
	    result = null;
	}
	logging.cancel();
	// logging = null;
    }

    /**
     * @return the isStopping
     */
    public boolean isStopping() {
	return isStopping;
    }

    /**
     * @param isStopping the isStopping to set
     */
    public void stopTask() {
	this.isStopping = true;
    }

}
