package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_safe_entry.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.favourite.FavouriteFragment
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.qrscanner.QrScannerFragmentV2
import sg.gov.tech.bluetrace.revamp.popUp.PopUpFragment
import sg.gov.tech.bluetrace.revamp.settings.BarCodeFragmentV2
import sg.gov.tech.bluetrace.view.CustomViewPager
import java.util.*


class SafeEntryFragment : PopUpFragment(), SafeEntryShowcaseDialogFragment.Callback {
    private val TAG = "SafeEntryFragment"
    var brightness: Float = -1.0f
    lateinit var mContext: Context
    private var mShowCaseView: ShowcaseView? = null
    var mViewPager: CustomViewPager? = null
    private lateinit var mShowcaseDialogFragment: SafeEntryShowcaseDialogFragment
    var onStop = false
    var hasSeenHowToUse = true
    private val imageResId = intArrayOf(
        R.drawable.icon_qr,
        R.drawable.icon_favourite,
        R.drawable.icon_barcode
    )
    private lateinit var tabAt: TabLayout.Tab
    private var mHandler: Handler = Handler()

    companion object {
        const val ARG_PAGE_NUM = "pageNum"

        fun newInstance(page: Int): SafeEntryFragment {
            val fragment = SafeEntryFragment()

            val bundle = Bundle().apply {
                putInt(ARG_PAGE_NUM, page)
            }

            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContext = view.context
        hasSeenHowToUse = Preference.hasSeenHowToUse(activity as Context)
        setupViewPager()
        prepareShowcase()

        imv_back.setOnClickListener {
            (activity as SafeEntryActivity).finish()
        }
        tv_how_to_use.setOnClickListener {
            startShowcase()
        }

        val pageNum: Int = arguments?.getInt(ARG_PAGE_NUM) ?: -1
        if (pageNum != -1)
            mViewPager?.setCurrentItem(pageNum, false)
    }

    override fun didProcessBackFragment(): Boolean {
        return false
    }

    private fun prepareShowcase() {
        if (!hasSeenHowToUse) {
            mHandler.postDelayed({
                startShowcase()
            }, 500)
            imv_back.visibility = View.INVISIBLE
            tv_how_to_use.visibility = View.INVISIBLE
        }
    }

    private fun startShowcase() {
        createShowcase(SafeEntryActivity.QR_FRAGMENT)
        showDialog(SafeEntryActivity.QR_FRAGMENT)
    }

    override fun onResume() {
        super.onResume()
        if (onStop) {
            resetShowcase()
            hasSeenHowToUse = Preference.hasSeenHowToUse(activity as Context)
            prepareShowcase()
            onStop = false
        }
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        onStop = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_entry, container, false)
    }

    override fun onDismissed() {
        resetShowcase()
    }

    override fun onButtonClicked(index: Int) {

        when (index) {
            SafeEntryActivity.QR_FRAGMENT -> {
                resetShowcase()
                createShowcase(SafeEntryActivity.FAV_FRAGMENT)
                showDialog(SafeEntryActivity.FAV_FRAGMENT)
            }
            SafeEntryActivity.FAV_FRAGMENT -> {
                if (!(activity as SafeEntryActivity).isFromGroupCheckIn) {
                    resetShowcase()
                    createShowcase(SafeEntryActivity.ID_FRAGMENT)
                    showDialog(SafeEntryActivity.ID_FRAGMENT)
                } else {
                    Preference.setHasSeenHowToUse(activity as Context, true)
                    checkCameraPermission()
                }
            }
            SafeEntryActivity.ID_FRAGMENT -> {
                Preference.setHasSeenHowToUse(activity as Context, true)
                checkCameraPermission()
            }
        }
    }

    private fun createShowcase(index: Int) {
        val button = Button(context)
        button.text = ""
        button.isEnabled = false
        button.visibility = View.GONE

        tabAt = tab_layout?.getTabAt(index)!!
        mShowCaseView = ShowcaseView.Builder(activity)
            .setTarget(ViewTarget(tabAt.view))
            .withMaterialShowcase()
            .setContentTitle("")
            .replaceEndButton(button)
            .setContentText("")
            .setStyle(R.style.CustomShowcaseTheme3)
            .hideOnTouchOutside()
            .build()

        mShowCaseView?.show()
    }

    private fun resetShowcase() {
        mShowCaseView?.let {
            mShowCaseView?.hide()
            activityFragmentManager.dismiss("showcase_SE")
        }
    }

    private fun showDialog(index: Int) {
        mShowcaseDialogFragment = SafeEntryShowcaseDialogFragment(index, tabAt.view)
        activityFragmentManager.show("showcase_SE", mShowcaseDialogFragment)
        mShowcaseDialogFragment.addCallback(this)
    }

    private fun setupViewPager() {
        mViewPager = view?.findViewById(R.id.view_pager)

        val mViewPagerAdapter = ViewPagerAdapter(activity?.supportFragmentManager)
        val scannerFragment = QrScannerFragmentV2.newInstance(true)
        val qrScannerFragment = fragmentModel()
        qrScannerFragment.fragment = scannerFragment
        mViewPagerAdapter.addFrag(qrScannerFragment)

        val favouriteFragment = fragmentModel()
        favouriteFragment.fragment = FavouriteFragment()
        mViewPagerAdapter.addFrag(favouriteFragment)

        if (!(activity as SafeEntryActivity).isFromGroupCheckIn) {
            val barCodeFragment = fragmentModel()
            barCodeFragment.fragment = BarCodeFragmentV2()
            mViewPagerAdapter.addFrag(barCodeFragment)
        }

        mViewPager?.adapter = mViewPagerAdapter
        mViewPagerAdapter.notifyDataSetChanged()
        tab_layout.setupWithViewPager(mViewPager)
        tab_layout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_TOP)
        tab_layout.tabMode = TabLayout.MODE_FIXED
        for (i in 0 until tab_layout.tabCount) {
            tab_layout.getTabAt(i)?.customView = setTabView(i)
        }
        mViewPager?.setPagingEnabled(false) // disabled swipe
        mViewPager?.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                // Check if this is the page you want.
                scannerFragment.onPageChanged(position)
                manageTint(position)
                if (position == SafeEntryActivity.ID_FRAGMENT) {
                    setBrightness()
                    AnalyticsUtils().screenAnalytics(
                        activity as Activity,
                        AnalyticsKeys.SCREEN_NAME_SE_DISPLAY_BARCODE
                    )
                } else {
                    AnalyticsUtils().screenAnalytics(
                        activity as Activity,
                        AnalyticsKeys.SCREEN_NAME_SE_SCAN_QR
                    )
                    resetBrightness()
                }
                if (position == SafeEntryActivity.FAV_FRAGMENT && Preference.isFavNew(requireContext())) {
                    Preference.putIsFavNew(requireContext(), false)
                    hideNewImgInTab(position)
                }
            }
        })
        manageTint(SafeEntryActivity.QR_FRAGMENT)
    }

    fun manageTint(int: Int) {
        when (int) {

            SafeEntryActivity.QR_FRAGMENT -> {
                imv_back.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_back_white
                    )
                )
                imv_back.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                tv_how_to_use.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                setTextViewDrawableColor(R.color.white)

                tv_how_to_use.visibility = View.VISIBLE

            }

            SafeEntryActivity.FAV_FRAGMENT, SafeEntryActivity.ID_FRAGMENT -> {
                imv_back.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_back
                    )
                )
                tv_how_to_use.visibility = View.VISIBLE
                tv_how_to_use.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.menu_text
                    )
                )
                setTextViewDrawableColor(R.color.menu_text)


            }

        }
    }

    private fun setTextViewDrawableColor(color: Int) {
        for (drawable in tv_how_to_use.compoundDrawables) {
            if (drawable != null) {
                drawable.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(
                        tv_how_to_use.context,
                        color
                    ), PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    private fun setTabView(pos: Int): View {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater!!.inflate(R.layout.safe_entry_custom_tab_layout, null)
        val item: ImageView = view.findViewById(R.id.iv_item)
        item.setImageResource(imageResId[pos])

        if (Preference.isFavNew(requireContext()) && imageResId[pos] == R.drawable.icon_favourite) {
            val newImg: ImageView = view.findViewById(R.id.iv_new)
            newImg.visibility = View.VISIBLE
        }

        return view
    }

    private fun hideNewImgInTab(pos: Int) {
        val view = tab_layout.getTabAt(pos)?.customView
        if (view != null) {
            val newImg: ImageView? = view.findViewById(R.id.iv_new)
            if (newImg != null) {
                newImg.visibility = View.GONE
            }
        }
    }

    class ViewPagerAdapter internal constructor(manager: FragmentManager?) :
        FragmentPagerAdapter(manager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val mFragmentList: MutableList<fragmentModel> =
            ArrayList()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position].fragment
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(frag: fragmentModel) {
            mFragmentList.add(frag)
        }
    }

    inner class fragmentModel {
        lateinit var fragment: Fragment
        lateinit var title: String
    }


    private fun checkCameraPermission() {
        // if(mViewPager?.currentItem == SafeEntryActivity.QR_FRAGMENT)
        showScanner()
    }

    private fun showScanner() {
       // manageTint(SafeEntryActivity.QR_FRAGMENT)
        imv_back.visibility = View.VISIBLE
        tv_how_to_use.visibility = View.VISIBLE
        resetShowcase()
       // mViewPager?.currentItem = SafeEntryActivity.QR_FRAGMENT
        QrScannerFragmentV2.isTutorialCompleted = true
        // scannerFragment.onResumeCamera()
    }

    fun setBrightness() {
        val layout: WindowManager.LayoutParams? = activity?.window?.attributes
        brightness = layout?.screenBrightness!!
        CentralLog.d(TAG, "brightness set:$brightness")
        layout.screenBrightness = 1f
        activity?.window?.attributes = layout
    }

    fun resetBrightness() {
        CentralLog.d(TAG, "brightness reset:$brightness")
        val layout: WindowManager.LayoutParams? = activity?.window?.attributes
        layout?.screenBrightness = brightness
        activity?.window?.attributes = layout

    }
}
