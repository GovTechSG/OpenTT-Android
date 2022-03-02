package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_base.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TranslatableActivity
import sg.gov.tech.bluetrace.logging.CentralLog

abstract class BaseActivity : TranslatableActivity() {
    private var TAG: String = "BaseActivity"
    private lateinit var mLoadingView: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
//        adjustFontScale(resources.configuration)
        mLoadingView = findViewById(R.id.view_loading)
        mLoadingView.setOnClickListener(View.OnClickListener {

        })
    }

    fun setProgressBar(progress: Int) {
        progress_bar.progress = progress
    }

    fun showIndicator(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    fun getContainer(): View {
        return main_container
    }

    private val mOnClickListener =
        View.OnClickListener { v: View ->
            when (v.id) {

            }
        }

    fun onClose() {
        finish()
    }

    fun setLoading(show: Boolean) {
        if (show) {
            mLoadingView.visibility = View.VISIBLE
        } else {
            mLoadingView.visibility = View.INVISIBLE
        }
    }

    fun setLoadingBackgroundWhite() {
        mLoadingView.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
    }

    fun setLoadingBackgroundBlack() {
        mLoadingView.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
    }

    fun openFragment(
        containerViewId: Int,
        fragment: Fragment,
        tag: String?
    ) {
        try {

            //pop all fragment
            popBackStackImmediate(true)

            val fragmentTransaction: FragmentTransaction =
                supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.animator.window_fadein, R.animator.window_fadeout,
                R.animator.window_fadein, R.animator.window_fadeout
            )
            fragmentTransaction.replace(containerViewId, fragment, tag)
            fragmentTransaction.commit()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushFragment(
        containerViewId: Int,
        fragment: Fragment,
        tag: String
    ) {
        try {
            val fragmentTransaction: FragmentTransaction =
                supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.animator.window_slideleft_enter, R.animator.window_slideleft_exit,
                R.animator.window_slideright_enter, R.animator.window_slideright_exit
            )
            fragmentTransaction.replace(containerViewId, fragment, tag)
            fragmentTransaction.addToBackStack(tag)
            fragmentTransaction.commit()
//            fragmentTransaction.commitAllowingStateLoss()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stackFragment(
        containerViewId: Int,
        fragment: Fragment,
        tag: String
    ) {
        try {
            val fragmentTransaction: FragmentTransaction =
                supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.animator.window_slideleft_enter, R.animator.window_slideleft_exit,
                R.animator.window_slideright_enter, R.animator.window_slideright_exit
            )
            fragmentTransaction.add(containerViewId, fragment, tag)
            fragmentTransaction.addToBackStack(tag)
            fragmentTransaction.commit()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun popBackStackImmediate(isPopAll: Boolean): Boolean {

        if (supportFragmentManager.backStackEntryCount <= 0) {
            return false
        }

        return if (isPopAll) {
            val firstFragNameTag = supportFragmentManager.getBackStackEntryAt(0).name
            supportFragmentManager.popBackStackImmediate(
                firstFragNameTag,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        } else {
            val entryName =
                supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1)
                    .name
            supportFragmentManager.popBackStackImmediate(
                entryName,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )

        }
    }

    open fun adjustFontScale(configuration: Configuration) {
        if (configuration.fontScale > 1.50) {
            CentralLog.d(
                "base",
                "fontScale=" + configuration.fontScale
            ) //Custom Log class, you can use Log.w
            CentralLog.d(
                "base",
                "font too big. scale down..."
            ) //Custom Log class, you can use Log.w
            configuration.fontScale = 1.50.toFloat()
            val metrics = resources.displayMetrics
            val wm =
                getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(metrics)
            metrics.scaledDensity = configuration.fontScale * metrics.density
            baseContext.resources.updateConfiguration(configuration, metrics)
        }
    }

    fun goBack(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 1) {
            return popBackStackImmediate(false)
        }
        return false
    }

}
