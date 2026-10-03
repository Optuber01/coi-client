package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.ikeepcalm.coi.domain.effect.visual.EffectPaint;
import dev.ua.ikeepcalm.coi.util.CoiLog;
import dev.ua.ikeepcalm.coi.util.JsonRead;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Toast queue for {@code coi-client:notify}. At most {@link #MAX_VISIBLE} are on screen; the
 * rest wait and are promoted with a fresh {@code shownAt} as slots free up, so a burst of
 * events reads as a sequence rather than a pile.
 */
public class NotificationState {

    public static final int MAX_VISIBLE = 3;
    public static final long SLIDE_MS = 250;
    public static final long FADE_MS = 300;

    private static final int DEFAULT_ARGB = 0xFFFFD870;
    private static final long DEFAULT_DURATION_MS = 4000;

    /** {@code "RRGGBB"}, with or without a leading {@code #}, to opaque ARGB. */
    private static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT_ARGB;
        try {
            return 0xFF000000 | (Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return DEFAULT_ARGB;
        }
    }

    private static final Deque<Toast> pending = new ArrayDeque<>();
    private static final List<Toast> visible = new ArrayList<>();

    private NotificationState() {
    }

    public static void handle(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            push(new Toast(
                    JsonRead.string(root, "kind", "info"),
                    JsonRead.string(root, "title"),
                    JsonRead.string(root, "body"),
                    parseColor(JsonRead.string(root, "color")),
                    JsonRead.longOf(root, "durationMs", DEFAULT_DURATION_MS)));
        } catch (Exception e) {
            CoiLog.LOG.warn("Malformed notify payload: {}", json);
        }
    }

    /** Retires, promotes, and returns what should be drawn this frame. */
    public static List<Toast> visibleToasts(long now) {
        synchronized (pending) {
            visible.removeIf(toast -> now >= toast.endsAt());
            while (visible.size() < MAX_VISIBLE && !pending.isEmpty()) {
                Toast toast = pending.removeFirst();
                toast.shownAt = now;
                visible.add(toast);
            }
            return List.copyOf(visible);
        }
    }

    private static void push(Toast toast) {
        synchronized (pending) {
            pending.addLast(toast);
        }
    }

    /** Debug only. */
    public static void debugToast() {
        push(new Toast("advancement", "Sequence 8 Reached",
                "You have digested the potion and become a Sun pathway Beyonder.",
                0xFFFFD870, DEFAULT_DURATION_MS));
    }

    /**
     * {@code shownAt} is 0 until the toast is promoted — that is when its timeline starts.
     */
    public static final class Toast {
        private final String kind;
        private final String title;
        private final String body;
        private final int argb;
        private final long durationMs;
        private long shownAt;

        Toast(String kind, String title, String body, int argb, long durationMs) {
            this.kind = kind;
            this.title = title;
            this.body = body;
            this.argb = argb;
            this.durationMs = durationMs;
        }

        public String kind() {
            return kind;
        }

        public String title() {
            return title;
        }

        public String body() {
            return body;
        }

        public int argb() {
            return argb;
        }

        long endsAt() {
            return shownAt + SLIDE_MS + durationMs + FADE_MS;
        }

        public float alpha(long now, boolean epilepsyMode) {
            long elapsed = now - shownAt;
            if (elapsed < SLIDE_MS) return epilepsyMode ? elapsed / (float) SLIDE_MS : 1f;
            long fadeStart = SLIDE_MS + durationMs;
            if (elapsed < fadeStart) return 1f;
            return Math.clamp(1f - (elapsed - fadeStart) / (float) FADE_MS, 0f, 1f);
        }

        /** Pixels still off the right edge. Always 0 under epilepsy mode, which fades instead. */
        public int slideOffset(long now, int width, boolean epilepsyMode) {
            long elapsed = now - shownAt;
            if (epilepsyMode || elapsed >= SLIDE_MS) return 0;
            float t = EffectPaint.easeOutCubic(elapsed / (float) SLIDE_MS);
            return Math.round(width * (1f - t));
        }
    }

    public static void reset() {
        synchronized (pending) {
            pending.clear();
            visible.clear();
        }
    }
}
