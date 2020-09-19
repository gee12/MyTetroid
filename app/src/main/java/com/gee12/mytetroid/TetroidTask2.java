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

    public interface IAsyncTaskListener {
        void blockInterface();
        void unblockInterface();
    }

    protected Context mContext;
    protected IAsyncTaskListener mTaskListener;

    public TetroidTask2(IAsyncTaskListener listener, Context context) {
        this.mTaskListener = listener;
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mTaskListener.blockInterface();
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Result result) {
        mTaskListener.unblockInterface();
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        mTaskListener.unblockInterface();
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
