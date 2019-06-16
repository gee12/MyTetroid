package com.gee12.mytetroid.views;

import com.gee12.mytetroid.activities.FoundPageFragment;
import com.gee12.mytetroid.activities.MainPageFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private TetroidFragment[] fragments = new TetroidFragment[2];

    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
        fragments[0] = new MainPageFragment();
        fragments[1] = new FoundPageFragment();
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
