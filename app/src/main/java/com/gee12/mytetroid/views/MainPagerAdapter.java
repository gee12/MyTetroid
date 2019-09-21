package com.gee12.mytetroid.views;

import android.os.Bundle;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.activities.FoundPageFragment;
import com.gee12.mytetroid.activities.IMainView;
import com.gee12.mytetroid.activities.MainPageFragment;

import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    public static final String KEY_MAIN_VIEW = "KEY_MAIN_VIEW";

    private TetroidFragment[] fragments = new TetroidFragment[2];
    Bundle arguments;

    public MainPagerAdapter(FragmentManager fm, IMainView mainView, GestureDetectorCompat detector) {
        super(fm);
        fragments[0] = new MainPageFragment(detector);
        fragments[1] = new FoundPageFragment(detector);

        this.arguments = new Bundle();
        arguments.putParcelable(KEY_MAIN_VIEW, mainView);
    }

    @Override
    public Fragment getItem(int position) {
        fragments[position].setArguments(arguments);
        return fragments[position];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fragments[position].getTitle();
    }

    public MainPageFragment getMainFragment() {
        return (MainPageFragment)fragments[0];
    }

    public FoundPageFragment getFoundFragment() {
        return (FoundPageFragment)fragments[1];
    }
}
