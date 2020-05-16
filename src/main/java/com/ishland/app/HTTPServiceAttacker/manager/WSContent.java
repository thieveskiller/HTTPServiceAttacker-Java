package com.ishland.app.HTTPServiceAttacker.manager;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class WSContent {
    public final Map<Integer, Long> success = new HashMap<>();
    public Long successcount = 0L;
    public final Map<Integer, Long> failure = new HashMap<>();
    public Long failurecount = 0L;
    public final Map<String, Long> errors = new HashMap<>();
    public Long errored = 0L;
    public final Map<String, Long> responses = new HashMap<>();
    public Double vaildRPS = 0.0;
    public Double totalRPS = 0.0;
    public Double maxVaildRPS = 0.0;
    public Double maxTotalRPS = 0.0;
    public Long creationSpeed = 0L;
    public Long maxCreationSpeed = 0L;
    public Long createdConnections = 0L;
    public Long maxAllowedConnections = 0L;
    public Long usedHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    public Long freeHeap = Runtime.getRuntime().freeMemory();
    public Long allocatedHeap = Runtime.getRuntime().totalMemory();
    public Long maxHeap = Runtime.getRuntime().maxMemory();
}
