package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ishland.app.HTTPServiceAttacker.attacker.Attack;
import com.ishland.app.HTTPServiceAttacker.manager.WSContent;
import com.ishland.app.HTTPServiceAttacker.manager.webserver.WSConnection;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MonitorThread extends Thread {

    private static final BlockingQueue<SimpleHttpResponse> queue = new LinkedBlockingQueue<>();
    private static final List<Callable<Object>> listCallbacks = new ArrayList<>();
    public static final WSContent wsContent = new WSContent();

    /*
     * private static Map<Integer, Long> success = new HashMap<>(); private static
     * long successcount = 0; private static Map<Integer, Long> failure = new
     * HashMap<>(); private static long failurecount = 0; private static long
     * errored = 0; private static long timeReqs = 0; private static long
     * timeReqsNoFail = 0; private static long timeEl = 0;
     */
    public static TimerTask logging;
    private static Gson gson;
    private static long timeReqs = 0;
    private static long timeReqsNoFail = 0;
    private static long timeEl = 0;
    private static long timeCre = 0;
    private boolean isStopping = false;

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

    public static synchronized void pushResult(SimpleHttpResponse httpResponse)
            throws InterruptedException, NullPointerException, IllegalArgumentException {
        queue.put(httpResponse);
    }

    public static boolean registerCallback(Callable<Object> callback) {
        return listCallbacks.add(callback);
    }

    private void createTimer() {
        logging = new TimerTask() {
            private final Logger logger = LoggerFactory.getLogger("Monitor Timer");

            @Override
            public void run() {
                if (((float) (System.currentTimeMillis() - timeEl) / 1000.0) < 0.1)
                    return;
                synchronized (wsContent) {
                    wsContent.vaildRPS = (float) (timeReqsNoFail)
                            / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
                    wsContent.totalRPS = (float) (timeReqs) / ((float) (System.currentTimeMillis() - timeEl) / 1000.0);
                    wsContent.creationSpeed = (long) ((timeCre)
                            / ((float) (System.currentTimeMillis() - timeEl) / 1000));
                    wsContent.maxVaildRPS = Math.max(wsContent.maxVaildRPS, wsContent.vaildRPS);
                    wsContent.maxTotalRPS = Math.max(wsContent.maxTotalRPS, wsContent.totalRPS);
                    wsContent.maxCreationSpeed = Math.max(wsContent.maxCreationSpeed, wsContent.creationSpeed);
                    wsContent.freeHeap = Runtime.getRuntime().freeMemory();
                    wsContent.allocatedHeap = Runtime.getRuntime().totalMemory();
                    wsContent.freeHeap = wsContent.allocatedHeap - wsContent.freeHeap;
                    wsContent.maxHeap = Runtime.getRuntime().maxMemory();
                    wsContent.createdConnections = AttackerThread.openedCount;

                    logger.info("Total: " + (wsContent.successcount
                            + wsContent.failurecount + wsContent.errored));
                    logger.info("Success info: " + wsContent.success.toString());
                    logger.info("Failure info: " + wsContent.failure.toString() + " + " + wsContent.errored.toString()
                            + " exceptions");
                    logger.info("Exceptions info: ");
                    try {
                        synchronized (wsContent.errors) {
                            int i = 0;
                            Iterator<Entry<String, Long>> ita = wsContent.errors.entrySet().iterator();
                            while (ita.hasNext() && i < 10) {
                                Entry<String, Long> entry = ita.next();
                                logger.info(entry.getValue() + " x " + entry.getKey());
                                i++;
                            }
                        }
                    } catch (java.util.ConcurrentModificationException e) {
                        logger.warn("Incomplete output");
                    }
                    logger.info("Response info: ");
                    try {
                        synchronized (wsContent.responses) {
                            int i = 0;
                            Iterator<Entry<String, Long>> itb = wsContent.responses.entrySet().iterator();
                            while (itb.hasNext() && i < 10) {
                                Entry<String, Long> entry = itb.next();
                                if (entry.getKey().length() < 50)
                                    logger.info(entry.getValue() + " x " + entry.getKey());
                                else
                                    logger.info(entry.getValue() + " x " + entry.getKey().substring(0, 50) + "...");
                                i++;
                            }
                        }
                    } catch (java.util.ConcurrentModificationException e) {
                        logger.warn("Incomplete output");
                    }
                    logger.info(
                            "RPS: " + wsContent.vaildRPS + "/" + wsContent.totalRPS);
                    logger.info("RPM: " + wsContent.vaildRPS * 60 + "/"
                            + wsContent.totalRPS * 60);
                    logger.info("Queued requests: " + wsContent.createdConnections + "/"
                            + wsContent.maxAllowedConnections);
                    logger.info("--------------------------------");
                    timeReqs = 0;
                    timeReqsNoFail = 0;
                    timeEl = System.currentTimeMillis();
                    String json = "";
                    try {
                        json = gson.toJson(wsContent);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Error while searlizing json: " + e.getMessage());
                    } catch (java.util.ConcurrentModificationException e) {
                        logger.warn("wsContent is busy");
                        return;
                    }
                    WSConnection.boardcast(json);
                    for (Callable<Object> listCallback : listCallbacks)
                        try {
                            listCallback.call();
                        } catch (Exception e) {
                            logger.warn("Cannot pass onMonitorRefresh event", e);
                        }
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
            totalthreads += Integer.parseInt(String.valueOf(Attack.getConfig().getTarget().get(i).get("threads")));
        wsContent.maxAllowedConnections = (long) (totalthreads * Attack.maxConnectionPerThread);
        while (!isStopping) {
            SimpleHttpResponse result;
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
                else
                    wsContent.success.put(result.getCode(), wsContent.success.get(result.getCode()) + 1);
                wsContent.successcount++;
            } else {
                if (wsContent.failure.get(result.getCode()) == null)
                    wsContent.failure.put(result.getCode(), 1L);
                else
                    wsContent.failure.put(result.getCode(), wsContent.failure.get(result.getCode()) + 1);
                wsContent.failurecount++;
            }
            if (!result.getBody().isText())
                if (!wsContent.responses.containsKey(
                        result.getBody().getBodyText().length() == 0 ? "[Empty]" : result.getBody().getBodyText()))
                    wsContent.responses.put(
                            result.getBody().getBodyText().length() == 0 ? "[Empty]" : result.getBody().getBodyText(),
                            1L);
                else
                    wsContent.responses.put(
                            result.getBody().getBodyText().length() == 0 ? "[Empty]" : result.getBody().getBodyText(),
                            wsContent.responses.get(result.getBody().getBodyText().length() == 0 ? "[Empty]"
                                    : result.getBody().getBodyText()) + 1L);
        }
        logging.cancel();
        // logging = null;
    }

// --Commented out by Inspection START (5/16/2020 11:28):
//    /**
//     * @return the isStopping
//     */
//    public boolean isStopping() {
//        return isStopping;
//    }
// --Commented out by Inspection STOP (5/16/2020 11:28)

    /**
     *
     */
    public void stopTask() {
        this.isStopping = true;
    }

}
