package com.gee12.mytetroid.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gee12.mytetroid.activities.IMainView;

import androidx.fragment.app.Fragment;

public abstract class TetroidFragment extends Fragment {

    protected IMainView mainView;
    protected String titleMask;

//    public TetroidFragment(IMainView mainView) {
//        this.mainView = mainView;
//    }

//    public TetroidFragment() {
//        this.mainView = mainView;
//    }

    public void setTitleMask(String titleMask) {
        this.titleMask = titleMask;
    }

    public abstract String getTitle();

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        Bundle arguments = getArguments();
//        if (arguments != null) {
//            this.mainView = arguments.getParcelable("KEY");
//        }
//    }

    public void setMainView(IMainView main) {
        this.mainView = main;
    }

    public void setMainView(Bundle arguments) {
        if (arguments != null) {
            this.mainView = arguments.getParcelable(MainPagerAdapter.KEY_MAIN_VIEW);
        }
    }

}
