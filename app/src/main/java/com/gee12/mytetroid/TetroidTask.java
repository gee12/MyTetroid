package com.gee12.mytetroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.WindowManager;

/**
 * Задание (параллельный поток).
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
@Deprecated
public abstract class TetroidTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    protected Activity mActivity;

    public TetroidTask(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Result result) {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        super.onCancelled();
    }

    public TetroidTask<Params, Progress, Result> run(Params... params) {
        super.execute(params);
        return this;
    }

    public boolean isRunning() {
        return getStatus() == Status.RUNNING;
    }
}
