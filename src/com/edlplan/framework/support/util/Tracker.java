package com.edlplan.framework.support.util;

import com.edlplan.framework.support.Framework;
import com.edlplan.framework.utils.advance.ConsumerContainer;
import com.edlplan.framework.utils.interfaces.Consumer;

import java.util.ArrayList;
import java.util.HashMap;

public class Tracker {
    public static final String DRAW_ARRAY = "DRAW_ARRAY";
    public static final String PREPARE_VERTEX_DATA = "PREPARE_VERTEX_DATA";
    public static final String INJECT_DATA = "INJECT_DATA";
    public static final String MAIN_LOOPER = "MAIN_LOOPER";
    public static final String DRAW_UI = "DRAW_UI";
    public static final String INVALIDATE_MEASURE_AND_LAYOUT = "INVALIDATE_MEASURE";
    public static final String TOTAL_FRAME_TIME = "TOTAL_FRAME_TIME";
    public static final TrackNode DrawArray;
    public static final TrackNode PrepareVertexData;
    public static final TrackNode InjectData;
    public static final TrackNode MainLooper;
    public static final TrackNode DrawUI;
    public static final TrackNode TotalFrameTime;
    public static final TrackNode InvalidateMeasureAndLayout;
    private static boolean enable = true;
    private static ArrayList<TrackNode> nodes;
    private static HashMap<String, TrackNode> namemap;


    static {
        nodes = new ArrayList<TrackNode>();
        namemap = new HashMap<String, TrackNode>();

        DrawArray = register(DRAW_ARRAY);
        PrepareVertexData = register(PREPARE_VERTEX_DATA);
        InjectData = register(INJECT_DATA);
        MainLooper = register(MAIN_LOOPER);

        InvalidateMeasureAndLayout = register(INVALIDATE_MEASURE_AND_LAYOUT);
        DrawUI = register(DRAW_UI);
        TotalFrameTime = register(TOTAL_FRAME_TIME);
    }

    public static TrackNode register(String name) {
        TrackNode node = new TrackNode(nodes.size(), name);
        nodes.add(node);
        namemap.put(name, node);
        return node;
    }

    public static TrackNode createTmpNode(String name) {
        return new TrackNode(-1, name);
    }

    public static void reset() {
        for (TrackNode n : nodes) {
            n.clear();
        }
    }

    public static void printlnAsTime(int ms) {
        System.out.println(ms + "ms");
    }

    public static Consumer<Integer> printByTag(String tag) {
        return t -> System.out.println(String.format("[%s] %dms", tag, t));
    }

    public static class TrackNode {
        public double totalTimeMS;
        public long trackedTimes;
        public double latestRecordTime;
        public int id;
        public String name;

        private int stack = 0;

        public TrackNode(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public void watch() {
            if (stack == 0) {
                latestRecordTime = Framework.relativePreciseTimeMillion();
            } else {
                double time = Framework.relativePreciseTimeMillion();
                totalTimeMS += time - latestRecordTime;
                latestRecordTime = time;
            }
            stack++;
        }

        public void end() {
            trackedTimes++;
            stack--;
            if (stack == 0) {
                totalTimeMS += Framework.relativePreciseTimeMillion() - latestRecordTime;
            } else {
                double time = Framework.relativePreciseTimeMillion();
                totalTimeMS += time - latestRecordTime;
                latestRecordTime = time;
            }
        }

        public void clear() {
            totalTimeMS = 0;
            trackedTimes = 0;
            latestRecordTime = 0;
            stack = 0;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ConsumerContainer<TrackNode> wrap(Runnable runnable) {
            watch();
            runnable.run();
            end();
            return new ConsumerContainer<>(this);
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("------------------------------------\n");
            sb.append("name         : " + name + " (" + id + ")\n");
            sb.append("totalTime    : " + totalTimeMS + "ms\n");
            sb.append("trackedTimes : " + trackedTimes + "\n");
            sb.append("------------------------------------");
            return sb.toString();
        }
    }
}