package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PropertiesLibrary {
    private static final PropertiesLibrary instance = new PropertiesLibrary();
    private final String version = "properties1";
    private Map<String, BeatmapProperties> props = new HashMap<String, BeatmapProperties>();
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
        if (lib.exists() == false) {
            return;
        }

        try {
            final ObjectInputStream istream = new ObjectInputStream(
                    new FileInputStream(lib));
            Object obj = istream.readObject();
            if (obj instanceof String) {
                if (((String) obj).equals(version) == false) {
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
        } catch (final FileNotFoundException e) {
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
        } catch (final IOException e) {
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
        } catch (final ClassNotFoundException e) {
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
        }
        ToastLogger.addToLog("Cannot load properties!");
    }

    public synchronized void save(final Context activity) {
        final File lib = new File(activity.getFilesDir(), "properties");
        try {
            final ObjectOutputStream ostream = new ObjectOutputStream(
                    new FileOutputStream(lib));
            ostream.writeObject(version);
            ostream.writeObject(props);
            ostream.close();
        } catch (final FileNotFoundException e) {
            ToastLogger.showText(
                    StringTable.format(R.string.message_error, e.getMessage()),
                    false);
            Debug.e("PropertiesLibrary: " + e.getMessage(), e);
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

    public void saveAsync() {
        if (context == null) {
            return;
        }
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                save(context);
            }


            public void onComplete() {
            }
        });
    }

    public BeatmapProperties getProperties(final String path) {
        if (props.containsKey(path)) {
            return props.get(path);
        }
        return new BeatmapProperties();
    }

    public void setProperties(final String path,
                              final BeatmapProperties properties) {
        this.load((Activity) context);
        props.put(path, properties);
        if (properties.favorite == false && properties.getOffset() == 0) {
            props.remove(path);
        }
    }
}
