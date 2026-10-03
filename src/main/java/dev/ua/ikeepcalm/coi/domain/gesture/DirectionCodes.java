package dev.ua.ikeepcalm.coi.domain.gesture;

import java.util.ArrayList;
import java.util.List;

/**
 * A resampled stroke as a string of 8-way direction codes ({@code '0'}–{@code '7'}), which is
 * what the templates are written in. Almost all of this class is denoising: oscillation along a
 * sector boundary, the entry/exit hooks a wrist leaves on a straight line, and the sample or two
 * of an unintended direction a corner produces.
 */
public class DirectionCodes {

    private static final float DOMINANT_RUN_FRACTION = 0.7f;
    /**
     * Shorter runs are corner noise — unless dropping them would leave nothing at all.
     */
    private static final int MIN_SIGNIFICANT_RUN = 2;
    /** Fewer alternating runs than this is a genuine change of direction, not oscillation. */
    private static final int MIN_OSCILLATION_RUNS = 3;

    private DirectionCodes() {
    }

    /** Empty when the stroke had no movement in it at all. */
    static String of(List<GestureRecognizer.StrokePoint> pts) {
        List<int[]> runs = mergeOscillations(quantize(pts));

        String line = asStraightLine(runs);
        if (line != null) return line;

        String denoised = collapseRuns(runs, MIN_SIGNIFICANT_RUN);
        return denoised.isEmpty() ? collapseRuns(runs, 1) : denoised;
    }

    /** Run-length encoded into {@code [direction, count]}; zero-length segments contribute nothing. */
    private static List<int[]> quantize(List<GestureRecognizer.StrokePoint> pts) {
        List<int[]> runs = new ArrayList<>();
        for (int i = 1; i < pts.size(); i++) {
            float dx = pts.get(i).x() - pts.get(i - 1).x();
            float dy = pts.get(i).y() - pts.get(i - 1).y();
            if (dx == 0 && dy == 0) continue;
            int dir = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dy, dx)) / 45.0), 8);
            if (!runs.isEmpty() && runs.getLast()[0] == dir) {
                runs.getLast()[1]++;
            } else {
                runs.add(new int[]{dir, 1});
            }
        }
        return runs;
    }

    /** Null when no direction dominates. */
    private static String asStraightLine(List<int[]> runs) {
        int total = 0;
        int[] dominant = null;
        for (int[] run : runs) {
            total += run[1];
            if (dominant == null || run[1] > dominant[1]) dominant = run;
        }
        if (dominant != null && dominant[1] >= total * DOMINANT_RUN_FRACTION) {
            return String.valueOf((char) ('0' + dominant[0]));
        }
        return null;
    }

    /**
     * A stroke along a sector boundary flickers between the two adjacent directions (7,6,7,6);
     * such blocks collapse into the dominant one. Two long adjacent runs — as around a circle —
     * are NOT oscillation and pass through untouched.
     */
    private static List<int[]> mergeOscillations(List<int[]> runs) {
        List<int[]> out = new ArrayList<>(runs.size());
        int i = 0;
        while (i < runs.size()) {
            int a = runs.get(i)[0];
            int b = -1;
            int j = i + 1;
            while (j < runs.size()) {
                int d = runs.get(j)[0];
                if (b == -1) {
                    if (d != a && adjacent(d, a)) b = d;
                    else break;
                } else if (d != a && d != b) {
                    break;
                }
                j++;
            }
            if (b != -1 && j - i >= MIN_OSCILLATION_RUNS) {
                int countA = 0, countB = 0;
                for (int k = i; k < j; k++) {
                    int[] run = runs.get(k);
                    if (run[0] == a) countA += run[1];
                    else countB += run[1];
                }
                out.add(new int[]{countA >= countB ? a : b, countA + countB});
                i = j;
            } else {
                out.add(runs.get(i));
                i++;
            }
        }
        return out;
    }

    private static boolean adjacent(int a, int b) {
        int diff = Math.floorMod(a - b, 8);
        return diff == 1 || diff == 7;
    }

    private static String collapseRuns(List<int[]> runs, int minRun) {
        StringBuilder sb = new StringBuilder();
        for (int[] run : runs) {
            if (run[1] < minRun) continue;
            char c = (char) ('0' + run[0]);
            if (sb.isEmpty() || sb.charAt(sb.length() - 1) != c) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
