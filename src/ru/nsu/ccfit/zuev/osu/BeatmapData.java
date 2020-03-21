package ru.nsu.ccfit.zuev.osu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BeatmapData {
    private final Map<String, Map<String, String>> sections = new HashMap<String, Map<String, String>>();
    private final Map<String, ArrayList<String>> dataSections = new HashMap<String, ArrayList<String>>();
    private String folder;

    public void addSection(final String name, final Map<String, String> data) {
        sections.put(name, data);
    }

    public void addSection(final String name, final ArrayList<String> data) {
        dataSections.put(name, data);
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(final String path) {
        folder = path;
    }

    public String getData(final String section, final String name) {
        if (sections.containsKey(section)) {
            if (sections.get(section).containsKey(name)) {
                return sections.get(section).get(name);
            }
        }
        return "";
    }

    public ArrayList<String> getData(final String section) {
        if (dataSections.containsKey(section)) {
            return dataSections.get(section);
        }
        return new ArrayList<String>();
    }
}
