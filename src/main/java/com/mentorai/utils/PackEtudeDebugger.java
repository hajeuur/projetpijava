package com.mentorai.utils;

import java.util.UUID;

public class PackEtudeDebugger {

    private static final boolean DEBUG = true;

    // ANSI Escape Codes for coloring
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private final String requestId;
    private String currentStep;
    private long stepStartTime;
    private final long totalStartTime;

    public PackEtudeDebugger() {
        this.requestId = UUID.randomUUID().toString().substring(0, 8);
        this.totalStartTime = System.currentTimeMillis();
    }

    public void startStep(String stepName) {
        if (!DEBUG) return;
        this.currentStep = stepName;
        this.stepStartTime = System.currentTimeMillis();
        System.out.println(CYAN + "[PackEtude][" + requestId + "] [STEP] " + stepName + "..." + RESET);
    }

    public void endStep() {
        if (!DEBUG || currentStep == null) return;
        long duration = System.currentTimeMillis() - stepStartTime;
        System.out.println(GREEN + "[PackEtude][" + requestId + "] [SUCCESS] " + currentStep + " completed in " + duration + "ms" + RESET);
        this.currentStep = null;
    }

    public void logInfo(String msg) {
        if (!DEBUG) return;
        System.out.println(RESET + "[PackEtude][" + requestId + "] [INFO] " + msg + RESET);
    }

    public void logWarn(String msg) {
        if (!DEBUG) return;
        System.out.println(YELLOW + "[PackEtude][" + requestId + "] [WARN] " + msg + RESET);
    }

    public void logError(Exception e) {
        if (!DEBUG) return;
        System.err.println(RED + "[PackEtude][" + requestId + "] [ERROR] Failed at step: " + (currentStep != null ? currentStep : "UNKNOWN_STEP") + RESET);
        System.err.println(RED + e.getClass().getName() + ": " + e.getMessage() + RESET);
    }

    public void endExecution() {
        if (!DEBUG) return;
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        System.out.println(GREEN + "[PackEtude][" + requestId + "] [DONE] Total execution time: " + totalDuration + "ms" + RESET);
    }
}
