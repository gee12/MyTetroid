package com.gee12.mytetroid;

import android.app.Activity;

import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.TetroidLog;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;
import com.google.android.play.core.tasks.Task;

public class TetroidReview {

    public static void showInAppReview(Activity activity) {

        TetroidLog.log(activity, activity.getString(R.string.log_start_review), ILogger.Types.INFO, -1);
        boolean test = false;   //BuildConfig.DEBUG
        ReviewManager manager = (test)
                ? new FakeReviewManager(activity) : ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();

        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();

                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.

                });

            } else {
                TetroidLog.log(activity, activity.getString(R.string.log_review_request_not_succ),
                        ILogger.Types.WARNING, -1);
            }
        });

    }

}
