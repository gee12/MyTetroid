package com.gee12.mytetroid;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Задание (параллельный поток).
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public abstract class TetroidTask2<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    public interface IAsyncTaskCallback {
        void blockInterface();
        void unblockInterface();
    }

    protected Context mContext;
    protected IAsyncTaskCallback mTaskCallback;

    public TetroidTask2(IAsyncTaskCallback callback, Context context) {
        this.mTaskCallback = callback;
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mTaskCallback.blockInterface();
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Result result) {
        mTaskCallback.unblockInterface();
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        mTaskCallback.unblockInterface();
        super.onCancelled();
    }

    public TetroidTask2<Params, Progress, Result> run(Params... params) {
        super.execute(params);
        return this;
    }

    public boolean isRunning() {
        return getStatus() == Status.RUNNING;
    }
}
