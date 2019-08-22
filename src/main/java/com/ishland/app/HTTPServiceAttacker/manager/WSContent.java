package com.ishland.app.HTTPServiceAttacker.manager;

import java.util.HashMap;
import java.util.Map;

public class WSContent {
    public Map<Integer, Long> success = new HashMap<>();
    public long successcount = 0;
    public Map<Integer, Long> failure = new HashMap<>();
    public long failurecount = 0;
    public long errored = 0;
    public double vaildRPS = 0.0;
    public double totalRPS = 0.0;
    public double maxVaildRPS = 0.0;
    public double maxTotalRPS = 0.0;
    public long usedHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    public long freeHeap = Runtime.getRuntime().freeMemory();
    public long allocatedHeap = Runtime.getRuntime().totalMemory();
    public long maxHeap = Runtime.getRuntime().maxMemory();
}
