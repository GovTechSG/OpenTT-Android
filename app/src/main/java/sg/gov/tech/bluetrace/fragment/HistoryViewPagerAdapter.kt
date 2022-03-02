package sg.gov.tech.bluetrace.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.appbar.AppBarLayout
import sg.gov.tech.bluetrace.R

class HistoryViewPagerAdapter(
    private val myContext: Context,
    fm: FragmentManager,
    private var totalTabs: Int
) : FragmentStatePagerAdapter(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val tabTitles = arrayOf(
        myContext.getString(R.string.all_records),
        myContext.getString(R.string.possible_exposure)
    )

    private val pages = if (totalTabs == 1) {
        arrayOf(BluetoothHistoryFragment())
    } else {
        arrayOf(BluetoothHistoryFragment(), BluetoothHistoryPossibleExposureFragment())
    }

    override fun getItem(position: Int) = pages[position]

    override fun getPageTitle(position: Int) = tabTitles[position]

    override fun getCount() = totalTabs
}
