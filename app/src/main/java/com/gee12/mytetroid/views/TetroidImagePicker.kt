package com.gee12.mytetroid.views

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.esafirm.imagepicker.features.*
import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig
import com.gee12.mytetroid.R

object TetroidImagePicker {

    private fun createConfig(context: Context): ImagePickerConfig {
        val returnAfterCapture = false
        val isSingleMode = false

//        ImagePickerComponentsHolder.setInternalComponent(
//            CustomImagePickerComponents(this, useCustomImageLoader)
//        )

        return ImagePickerConfig {
            mode = if (isSingleMode) ImagePickerMode.SINGLE else ImagePickerMode.MULTIPLE // multi mode (default mode)

//            language = "in" // Set image picker language
//            theme = R.style.ImagePickerTheme

            // set whether pick action or camera action should return immediate result or not. Only works in single mode for image picker
            returnMode = if (returnAfterCapture) ReturnMode.ALL else ReturnMode.NONE

            isFolderMode = true // set folder mode (false by default)
            isIncludeVideo = false // include video (false by default)
            isIncludeAnimation = true // include gif (false by default)
            isOnlyVideo = false // include video (false by default)
//            arrowColor = Color.RED // set toolbar arrow up color
            folderTitle = context.getString(R.string.title_gallery) // folder selection title
            imageTitle = null // image selection title
            doneButtonText = context.getString(R.string.title_confirm_selected_images) // done button text
            showDoneButtonAlways = false // Show done button always or not
            limit = 10 // max images can be selected (99 by default)
            isShowCamera = false // show camera or not (true by default)
//            savePath = ImagePickerSavePath("Camera") // captured image directory name ("Camera" folder by default)
//            savePath = ImagePickerSavePath(Environment.getExternalStorageDirectory().path, isRelative = false) // can be a full path

//            if (isExclude) {
//                excludedImages = images.toFiles() // don't show anything on this selected images
//            } else {
//                selectedImages = images  // original selected images, used in multi mode
//            }
            isShowSearch = true
            isShowImageNames = true
            foldersSortMode = FolderSortMode.NUMBER_DESC
            imagesSortMode = ImageSortMode.DATE_MODIFIED_DESC
        }
    }

    /**
     * StartPicker image picker activity with request code.
     * @param activity
     */
    @JvmStatic
    fun startPicker(activity: AppCompatActivity, requestCode: Int/*, callback: ImagePickerCallback*/) {
        // FIXME: Устарело, переделать на callback
        val intent = createImagePickerIntent(activity, createConfig(activity))
        activity.startActivityForResult(intent, requestCode)

//        activity.registerImagePicker { callback.invoke(it) }
//            .launch(createConfig(activity))
    }

    private fun createCameraConfig(/*newImagesFullDir: String, newImagesDir: String*/): CameraOnlyConfig {
        return CameraOnlyConfig(
            // не удалось сохранять сделанную фотографию сразу в каталог записи
            // (возникает ошибка на Android 9)
//            savePath = ImagePickerSavePath(newImagesFullDir, false),
            returnMode = ReturnMode.ALL,
            isSaveImage = true
        )
    }

    /**
     * Start capture photo from camera with request code.
     * @param activity
     */
    @JvmStatic
    fun startCamera(activity: Activity, requestCode: Int/*, newImagesFullDir: String, newImagesDir: String*/) {
        // FIXME: Устарело, переделать на callback
        val intent = createImagePickerIntent(activity, createCameraConfig(/*newImagesFullDir, newImagesDir*/))
        activity.startActivityForResult(intent, requestCode)

//        activity.registerImagePicker { callback.invoke(it) }
//            .launch(CameraOnlyConfig())
    }
}