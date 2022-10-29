package com.gee12.mytetroid.views.adapters;

import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.gee12.mytetroid.viewmodels.MainViewModel;
import com.gee12.mytetroid.views.fragments.FoundPageFragment;
import com.gee12.mytetroid.views.fragments.MainPageFragment;
import com.gee12.mytetroid.views.fragments.TetroidFragment;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private TetroidFragment[] fragments = new TetroidFragment[2];

    public MainPagerAdapter(MainViewModel viewModel, FragmentManager fm, GestureDetectorCompat detector) {
        super(fm);
        fragments[0] = new MainPageFragment(viewModel, detector);
        fragments[1] = new FoundPageFragment(viewModel, detector);
    }

    @Override
    public Fragment getItem(int position) {
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
