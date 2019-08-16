package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitorThread extends Thread {

    private static BlockingQueue<CloseableHttpResponse> queue = new LinkedBlockingQueue<>();
    private boolean isStopping = false;

    private static Map<Integer, Long> success = new HashMap<>();
    private static long successcount = 0;
    private static Map<Integer, Long> failure = new HashMap<>();
    private static long failurecount = 0;
    private static long errored = 0;
    private static long timeReqs = 0;
    private static long timeReqsNoFail = 0;
    private static long timeEl = 0;

    private static TimerTask logging = new TimerTask() {
	private final Logger logger = LogManager.getLogger("Monitor Timer");

	@Override
	public void run() {
	    logger.info("Total: "
		    + String.valueOf(MonitorThread.successcount + MonitorThread.failurecount + MonitorThread.errored));
	    logger.info("Success info: " + success.toString());
	    logger.info("Failure info: " + failure.toString());
	    logger.info("Thrown exceptions count: " + errored);
	    logger.info("RPS: "
		    + String.valueOf(
			    (float) (timeReqsNoFail) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0))
		    + "/"
		    + String.valueOf((float) (timeReqs) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0)));
	    timeReqs = 0;
	    timeReqsNoFail = 0;
	    timeEl = System.currentTimeMillis();
	}

    };

    public static synchronized void newError() {
	errored++;
	timeReqs++;
    }

    public static synchronized void pushResult(CloseableHttpResponse result)
	    throws InterruptedException, NullPointerException, IllegalArgumentException {
	queue.put(result);
    }

    public void run() {
	new Timer().schedule(logging, 0, 5000);
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
		if (success.get(result.getStatusLine().getStatusCode()) == null)
		    success.put(result.getStatusLine().getStatusCode(), 1L);
		success.put(result.getStatusLine().getStatusCode(),
			success.get(result.getStatusLine().getStatusCode()).longValue() + 1);
		successcount++;
	    } else {
		if (failure.get(result.getStatusLine().getStatusCode()) == null)
		    failure.put(result.getStatusLine().getStatusCode(), 1L);
		failure.put(result.getStatusLine().getStatusCode(),
			failure.get(result.getStatusLine().getStatusCode()).longValue() + 1);
		failurecount++;
	    }
	    try {
		result.close();
	    } catch (IOException e) {
	    }
	}
	logging.cancel();
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