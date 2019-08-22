package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

import com.google.gson.Gson;
import com.ishland.app.HTTPServiceAttacker.manager.WSContent;
import com.ishland.app.HTTPServiceAttacker.manager.webserver.WSConnection;

public class MonitorThread extends Thread {

    private static BlockingQueue<CloseableHttpResponse> queue = new LinkedBlockingQueue<>();
    private boolean isStopping = false;
    private static Gson gson = new Gson();

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

    private static TimerTask logging = new TimerTask() {
	private final Logger logger = LogManager.getLogger("Monitor Timer");

	@Override
	public void run() {
	    wsContent.vaildRPS = (float) (timeReqsNoFail) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
	    wsContent.totalRPS = (float) (timeReqs) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
	    wsContent.maxVaildRPS = wsContent.maxVaildRPS > wsContent.vaildRPS ? wsContent.maxVaildRPS
		    : wsContent.vaildRPS;
	    wsContent.maxTotalRPS = wsContent.maxTotalRPS > wsContent.totalRPS ? wsContent.maxTotalRPS
		    : wsContent.totalRPS;
	    wsContent.freeHeap = Runtime.getRuntime().freeMemory();
	    wsContent.allocatedHeap = Runtime.getRuntime().totalMemory();
	    wsContent.freeHeap = wsContent.allocatedHeap - wsContent.freeHeap;
	    wsContent.maxHeap = Runtime.getRuntime().maxMemory();

	    logger.info(
		    "Total: " + String.valueOf(wsContent.successcount + wsContent.failurecount + wsContent.errored));
	    logger.info("Success info: " + wsContent.success.toString());
	    logger.info("Failure info: " + wsContent.failure.toString() + " + " + wsContent.errored + " exceptions");
	    logger.info("RPS: " + String.valueOf(wsContent.vaildRPS) + "/" + String.valueOf(wsContent.totalRPS));
	    logger.info(
		    "RPM: " + String.valueOf(wsContent.vaildRPS * 60) + "/" + String.valueOf(wsContent.totalRPS * 60));
	    timeReqs = 0;
	    timeReqsNoFail = 0;
	    timeEl = System.currentTimeMillis();
	    Iterator<Entry<Integer, Session>> it = WSConnection.sessions.entrySet().iterator();
	    String json = "";
	    try {
		json = gson.toJson(wsContent);
	    } catch (IllegalArgumentException e) {
		return;
	    }
	    while (it.hasNext()) {
		Entry<Integer, Session> entry = it.next();
		entry.getValue().getRemote().sendStringByFuture(json);
	    }
	}

    };

    public static synchronized void newError() {
	wsContent.errored++;
	timeReqs++;
    }

    public static synchronized void pushResult(CloseableHttpResponse result)
	    throws InterruptedException, NullPointerException, IllegalArgumentException {
	queue.put(result);
    }

    public void run() {
	new Timer().scheduleAtFixedRate(logging, 0, 500);
	while (!isStopping) {
	    CloseableHttpResponse result = null;
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
	    try {
		result.close();
	    } catch (IOException e) {
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
