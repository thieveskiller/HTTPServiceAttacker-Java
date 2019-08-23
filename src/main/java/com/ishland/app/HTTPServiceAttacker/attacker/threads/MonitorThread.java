package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static WSContent wsContent = new WSContent();
    private static long timeReqs = 0;
    private static long timeReqsNoFail = 0;
    private static long timeEl = 0;
    private static long timeCre = 0;

    private static TimerTask logging = new TimerTask() {
	private final Logger logger = LogManager.getLogger("Monitor Timer");

	@Override
	public void run() {
	    wsContent.vaildRPS = (float) (timeReqsNoFail) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
	    wsContent.totalRPS = (float) (timeReqs) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
	    wsContent.creationSpeed = (long) ((timeCre) / ((float) (System.currentTimeMillis() - timeEl) / 1000));
	    wsContent.maxVaildRPS = wsContent.maxVaildRPS.doubleValue() > wsContent.vaildRPS.doubleValue()
		    ? wsContent.maxVaildRPS.doubleValue()
		    : wsContent.vaildRPS.doubleValue();
	    wsContent.maxTotalRPS = wsContent.maxTotalRPS.doubleValue() > wsContent.totalRPS.doubleValue()
		    ? wsContent.maxTotalRPS.doubleValue()
		    : wsContent.totalRPS.doubleValue();
	    wsContent.maxCreationSpeed = wsContent.maxCreationSpeed.longValue() > wsContent.creationSpeed.longValue()
		    ? wsContent.maxCreationSpeed.longValue()
		    : wsContent.creationSpeed.longValue();
	    wsContent.freeHeap = Runtime.getRuntime().freeMemory();
	    wsContent.allocatedHeap = Runtime.getRuntime().totalMemory();
	    wsContent.freeHeap = wsContent.allocatedHeap.longValue() - wsContent.freeHeap.longValue();
	    wsContent.maxHeap = Runtime.getRuntime().maxMemory();

	    logger.info("Total: " + String.valueOf(wsContent.successcount.longValue()
		    + wsContent.failurecount.longValue() + wsContent.errored.longValue()));
	    logger.info("Success info: " + wsContent.success.toString());
	    logger.info("Failure info: " + wsContent.failure.toString() + " + " + wsContent.errored + " exceptions");
	    logger.info("RPS: " + String.valueOf(wsContent.vaildRPS) + "/" + String.valueOf(wsContent.totalRPS));
	    logger.info("RPM: " + String.valueOf(wsContent.vaildRPS.longValue() * 60) + "/"
		    + String.valueOf(wsContent.totalRPS.longValue() * 60));
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
	}

    };

    public static synchronized void newError() {
	wsContent.errored++;
	timeReqs++;
    }

    public static synchronized void newCreation() {
	timeCre++;
    }

    public static synchronized void pushResult(HttpResponse httpResponse)
	    throws InterruptedException, NullPointerException, IllegalArgumentException {
	queue.put(httpResponse);
    }

    public void run() {
	GsonBuilder gsona = new GsonBuilder();
	gson = gsona.create();
	new Timer().scheduleAtFixedRate(logging, 0, 500);
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
	    if (result.getStatusLine().getStatusCode() >= 200 && result.getStatusLine().getStatusCode() < 300) {
		if (wsContent.success.get(result.getStatusLine().getStatusCode()) == null)
		    wsContent.success.put(result.getStatusLine().getStatusCode(), 1L);
		wsContent.success.put(result.getStatusLine().getStatusCode(),
			wsContent.success.get(result.getStatusLine().getStatusCode()).longValue() + 1);
		wsContent.successcount++;
	    } else {
		if (wsContent.failure.get(result.getStatusLine().getStatusCode()) == null)
		    wsContent.failure.put(result.getStatusLine().getStatusCode(), 1L);
		wsContent.failure.put(result.getStatusLine().getStatusCode(),
			wsContent.failure.get(result.getStatusLine().getStatusCode()).longValue() + 1);
		wsContent.failurecount++;
	    }
	    result = null;
	}
	logging.cancel();
	logging = null;
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
