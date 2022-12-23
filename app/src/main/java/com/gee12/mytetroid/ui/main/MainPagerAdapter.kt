package com.gee12.mytetroid.ui.main

import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.gee12.mytetroid.ui.main.found.FoundPageFragment

// TODO: переписать на новый лад
class MainPagerAdapter(
    viewModel: MainViewModel,
    fragmentManager: FragmentManager,
    detector: GestureDetectorCompat,
) : FragmentPagerAdapter(
    fragmentManager
) {

    val mainFragment = MainPageFragment(viewModel, detector)
    val foundFragment = FoundPageFragment(viewModel, detector)

    private val fragments = arrayOf(
        mainFragment,
        foundFragment,
    )

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (val fragment = getItem(position)) {
            is MainPageFragment -> fragment.getTitle()
            is FoundPageFragment -> fragment.getTitle()
            else -> null
        }
    }

}