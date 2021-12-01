package com.gee12.mytetroid

import android.app.Activity
import com.gee12.mytetroid.logs.ITetroidLogger
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.tasks.Task


class ReviewInteractor(
    private val logger: ITetroidLogger
) {

    fun showInAppReview(activity: Activity) {
        logger.log(activity.getString(R.string.log_start_review), false)
        val test = false //BuildConfig.DEBUG
        val manager = if (test) FakeReviewManager(activity) else ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task: Task<ReviewInfo?> ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { }
            } else {
                logger.logWarning(activity.getString(R.string.log_review_request_not_succ), false)
            }
        }
    }

}