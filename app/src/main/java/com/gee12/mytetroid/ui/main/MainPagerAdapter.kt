package com.gee12.mytetroid.ui.main

import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gee12.mytetroid.ui.main.found.FoundPageFragment

class MainPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    detector: GestureDetectorCompat,
) : FragmentStateAdapter(
    fragmentManager,
    lifecycle
) {

    val mainFragment = MainPageFragment(detector)
    val foundFragment = FoundPageFragment(detector)

    private val fragments = arrayOf(
        mainFragment,
        foundFragment,
    )

    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun createFragment(position: Int): Fragment {
        return getFragment(position)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    fun getPageTitle(position: Int): CharSequence? {
        return when (val fragment = getFragment(position)) {
            is MainPageFragment -> fragment.getTitle()
            is FoundPageFragment -> fragment.getTitle()
            else -> null
        }
    }

}