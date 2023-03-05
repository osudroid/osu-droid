package main.osu.async;

import android.os.AsyncTask;

public class AsyncTaskLoader extends
        AsyncTask<OsuAsyncCallback, Integer, Boolean> {

    private OsuAsyncCallback[] mparams;


    @Override
    protected Boolean doInBackground(final OsuAsyncCallback... params) {
        this.mparams = params;
        final int count = params.length;
        for (int i = 0; i < count; i++) {
            params[i].run();
        }
        return true;
    }


    @Override
    protected void onPostExecute(final Boolean result) {
        final int count = this.mparams.length;
        for (int i = 0; i < count; i++) {
            this.mparams[i].onComplete();
        }
    }

}
