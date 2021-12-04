package com.gee12.mytetroid.views;

import android.app.Activity;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.cameraonly.ImagePickerCameraOnly;
import com.gee12.mytetroid.R;

public class ImgPicker {

    /**
     * StartPicker image picker activity with request code.
     * @param activity
     */
    public static void startPicker(Activity activity) {
        ImagePicker.create(activity)
                .limit(10)
                .toolbarFolderTitle(activity.getString(R.string.title_gallery))
                .toolbarDoneButtonText(activity.getString(R.string.title_confirm_selected_images))
                .showCamera(false)
                .folderMode(true)
                .includeVideo(false)
                .start();
    }

    /**
     * Start capture photo from camera with request code.
     * @param activity
     * @param newImagesFullDir
     */
    public static void startCamera(Activity activity, String newImagesFullDir, String newImagesDir) {
        createCamera(newImagesFullDir, newImagesDir).start(activity);
    }

    public static ImagePickerCameraOnly createCamera(String newImagesFullDir, String newImagesDir) {
        return ImagePicker.cameraOnly()
                .imageDirectory(newImagesDir)
                .imageFullDirectory(newImagesFullDir);
    }
}
