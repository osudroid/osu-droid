package com.reco1l.framework.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.reco1l.framework.drawing.Dimension;
import com.reco1l.framework.execution.Async;

import java.util.LinkedList;
import java.util.Queue;

public class BitmapQueue {

    private final Queue<QueueItem> mQueue;

    private boolean mIsQuerying = false;

    //--------------------------------------------------------------------------------------------//

    public BitmapQueue() {
        mQueue = new LinkedList<>();
    }

    //--------------------------------------------------------------------------------------------//

    public void query() {
        if (mIsQuerying) {
            return;
        }

        Async.run(() -> {
            mIsQuerying = true;

            while (!mQueue.isEmpty()) {
                QueueItem item = mQueue.poll();

                if (item == null || item.path == null) {
                    continue;
                }
                Bitmap bm = loadBitmap(item.path, item.dimension);

                if (item.callback != null) {
                    item.callback.onQueuePoll(bm);
                }
            }

            mIsQuerying = false;
        });
    }

    private Bitmap loadBitmap(String path, Dimension dimen) {

        Bitmap bm = BitmapFactory.decodeFile(path);
        if (bm == null) {
            // For some reason in some devices decodeFile() returns null so i've to put this little workaround.
            return null;
        }
        bm = bm.copy(Config.ARGB_8888, true);

        float scale = (float) dimen.width / bm.getWidth();

        bm = BitmapHelper.resize(bm, bm.getWidth() * scale, bm.getHeight() * scale);
        bm = BitmapHelper.cropInCenter(bm, dimen.width, dimen.height);

        return bm;
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface BitmapCallback {
        void onQueuePoll(Bitmap bitmap);
    }

    //--------------------------------------------------------------------------------------------//

    public static class QueueItem {

        private final BitmapCallback callback;
        private final Dimension dimension;
        private final String path;

        public QueueItem(BitmapCallback callback, Dimension dimension, String path) {
            this.dimension = dimension;
            this.callback = callback;
            this.path = path;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private boolean isValidDimensions(Dimension d) {
        if (d == null) {
            return false;
        }
        return d.height > 0 && d.width > 0;
    }

    public QueueItem queue(BitmapCallback callback, Dimension dimension, String path) {
        if (!isValidDimensions(dimension) || path == null) {
            return null;
        }
        QueueItem item = new QueueItem(callback, dimension, path);
        mQueue.add(item);
        query();
        return item;
    }

    public void remove(QueueItem item) {
        mQueue.remove(item);
    }
}
