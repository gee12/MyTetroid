package com.gee12.mytetroid.ui.record

import android.app.Activity
import android.os.Bundle
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildBundle
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.ui.main.MainActivity

class RecordActivityComponent(
    private val activity: RecordActivity,
    private val viewModel: RecordViewModel,
) {

    /**
     * Формирование результата активити и ее закрытие.
     * При необходимости запуск главной активности, если текущая была запущена самостоятельно.
     */
    fun finishWithResult(
        resultActionType: Int?,
        bundle: Bundle?,
        isOpenMainActivity: Boolean = true,
    ) {
        val resultBundle = (bundle ?: Bundle()).apply {
            resultActionType?.also {
                putInt(Constants.EXTRA_RESULT_ACTION_TYPE, it)
            }
            with(viewModel.recordState) {
                if (!containsKey(Constants.EXTRA_RECORD_ID)) {
                    putString(Constants.EXTRA_RECORD_ID, recordId)
                }
                putBoolean(Constants.EXTRA_IS_SAVED_IN_TREE, isSavedFromTemporary)
                putBoolean(Constants.EXTRA_IS_FIELDS_EDITED, isFieldsEdited)
                putBoolean(Constants.EXTRA_IS_TEXT_EDITED, isTextEdited)
            }
        }
        val intent = buildIntent {
            action = Constants.ACTION_RECORD
            putExtras(resultBundle)
        }
        // если возвращаться некуда, то можем просто закрывать приложение.
        if (activity.callingActivity != null) {
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        } else {
            activity.finish()
            if (isOpenMainActivity) {
                MainActivity.start(activity, intent)
            }
        }
    }

    fun openAnotherRecord(recordId: String) {
        val bundle = buildBundle {
            putString(Constants.EXTRA_RECORD_ID, recordId)
        }
        finishWithResult(
            resultActionType = Constants.RESULT_OPEN_RECORD,
            bundle = bundle,
        )
    }

    fun openNode(nodeId: String) {
        val bundle = buildBundle {
            putString(Constants.EXTRA_NODE_ID, nodeId)
        }
        finishWithResult(
            resultActionType = Constants.RESULT_OPEN_NODE,
            bundle = bundle,
        )
    }

    fun openTag(tagName: String) {
        val bundle = buildBundle {
            putString(Constants.EXTRA_TAG_NAME, tagName)
        }
        finishWithResult(
            resultActionType = Constants.RESULT_SHOW_TAG,
            bundle = bundle,
        )
    }

    fun openRecordAttaches(recordId: String) {
        val bundle = buildBundle {
            putString(Constants.EXTRA_RECORD_ID, recordId)
        }
        finishWithResult(
            resultActionType = Constants.RESULT_SHOW_ATTACHES,
            bundle = bundle,
        )
    }

    fun deleteRecord(recordId: String) {
        val bundle = buildBundle {
            putString(Constants.EXTRA_RECORD_ID, recordId)
        }
        finishWithResult(
            resultActionType = Constants.RESULT_DELETE_RECORD,
            bundle = bundle,
        )
    }

}