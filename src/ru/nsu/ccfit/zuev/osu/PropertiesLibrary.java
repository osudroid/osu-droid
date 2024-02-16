package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;

import org.andengine.util.debug.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PropertiesLibrary {
    private static final PropertiesLibrary instance = new PropertiesLibrary();
    private final String version = "properties1";
    private Map<String, BeatmapProperties> props = new HashMap<>();
    private Context context = null;
    private PropertiesLibrary() {
    }

    public static PropertiesLibrary getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public void load(final Activity activity) {
        context = activity;
        final File lib = new File(activity.getFilesDir(), "properties");
        if (!lib.exists()) {
            return;
        }

        try {
            final ObjectInputStream istream = new ObjectInputStream(
                    new FileInputStream(lib));
            Object obj = istream.readObject();
            if (obj instanceof String) {
                if (!obj.equals(version)) {
                    istream.close();
                    return;
                }
            } else {
                istream.close();
                return;
            }
            obj = istream.readObject();
            if (obj instanceof Map<?, ?>) {
                props = (Map<String, BeatmapProperties>) obj;
                istream.close();
                Debug.i("Properties loaded");
            }
            istream.close();
        } catch (final IOException | ClassNotFoundException e) {
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
        }
    }

    public synchronized void save(final Context activity) {
        final File lib = new File(activity.getFilesDir(), "properties");
        try {
            final ObjectOutputStream ostream = new ObjectOutputStream(
                    new FileOutputStream(lib));
            ostream.writeObject(version);
            ostream.writeObject(props);
            ostream.close();
        } catch (final IOException e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
        }
    }

    public synchronized void clear(final Context activity) {
        final File lib = new File(activity.getFilesDir(), "properties");
        lib.delete();
        props.clear();
    }

    public void save() {
        if (context == null) {
            return;
        }
        save(context);
    }

    public BeatmapProperties getProperties(final String path) {
        if (props.containsKey(path)) {
            return props.get(path);
        }
        return null;
    }

    public void setProperties(final String path,
                              final BeatmapProperties properties) {
        this.load((Activity) context);
        props.put(path, properties);
        if (!properties.favorite && properties.getOffset() == 0) {
            props.remove(path);
        }
    }
}
