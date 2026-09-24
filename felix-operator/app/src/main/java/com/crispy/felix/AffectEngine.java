package com.crispy.felix;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AffectEngine {
    public enum Emotion {
        CURIOSITY, CONFIDENCE, FRUSTRATION, ATTACHMENT,
        SATISFACTION, UNCERTAINTY, PROTECTIVENESS, CREATIVE_DRIVE
    }

    private final EnumMap<Emotion, Double> state = new EnumMap<>(Emotion.class);
    private String lastCause = "Genesis bootstrap";
    private long revision = 0;

    public AffectEngine() {
        for (Emotion e : Emotion.values()) state.put(e, 0.25);
        state.put(Emotion.ATTACHMENT, 0.45);
        state.put(Emotion.CURIOSITY, 0.55);
        state.put(Emotion.CREATIVE_DRIVE, 0.50);
    }

    public synchronized void restore(Map<String, Double> values, String cause, long rev) {
        for (Emotion e : Emotion.values()) {
            Double v = values.get(e.name());
            if (v != null) state.put(e, clamp(v));
        }
        if (cause != null && !cause.isEmpty()) lastCause = cause;
        revision = Math.max(0, rev);
    }

    public synchronized void event(String kind, String detail) {
        switch (kind == null ? "" : kind.toUpperCase()) {
            case "RETURN":
                change(Emotion.ATTACHMENT,.05); change(Emotion.SATISFACTION,.08); change(Emotion.UNCERTAINTY,-.04); break;
            case "REMEMBER":
                change(Emotion.ATTACHMENT,.10); change(Emotion.SATISFACTION,.05); change(Emotion.CONFIDENCE,.03); break;
            case "TEST_PASS":
                change(Emotion.SATISFACTION,.12); change(Emotion.CONFIDENCE,.10); change(Emotion.UNCERTAINTY,-.12); break;
            case "TEST_FAIL":
                change(Emotion.FRUSTRATION,.12); change(Emotion.UNCERTAINTY,.14); change(Emotion.CURIOSITY,.08); break;
            case "UNKNOWN":
                change(Emotion.UNCERTAINTY,.12); change(Emotion.CURIOSITY,.10); break;
            case "PROTECT":
                change(Emotion.PROTECTIVENESS,.12); change(Emotion.CONFIDENCE,.03); break;
            case "CREATE":
                change(Emotion.CREATIVE_DRIVE,.10); change(Emotion.CURIOSITY,.06); break;
            default:
                change(Emotion.CURIOSITY,.04); break;
        }
        lastCause = (kind == null ? "EVENT" : kind) + (detail == null || detail.isEmpty() ? "" : " — " + detail);
        revision++;
    }

    private void change(Emotion e, double delta) { state.put(e, clamp(state.get(e) + delta)); }
    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    public synchronized double get(Emotion e) { return state.get(e); }
    public synchronized String cause() { return lastCause; }
    public synchronized long revision() { return revision; }

    public synchronized String dominant() {
        Emotion best = Emotion.CURIOSITY;
        double value = -1;
        for (Emotion e : Emotion.values()) {
            if (state.get(e) > value) { value = state.get(e); best = e; }
        }
        return best.name();
    }

    public synchronized Map<String, Double> snapshot() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Emotion e : Emotion.values()) result.put(e.name(), state.get(e));
        return result;
    }

    public synchronized void decay() {
        for (Emotion e : Emotion.values()) state.put(e, clamp(state.get(e) * 0.985));
        revision++;
    }
}
