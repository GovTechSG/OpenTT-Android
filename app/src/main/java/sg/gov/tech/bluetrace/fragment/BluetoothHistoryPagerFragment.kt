package sg.gov.tech.bluetrace.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.android.synthetic.main.bt_history_view_pager.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.settings.BarcodeHeaderView
import sg.gov.tech.bluetrace.settings.OnBarcodeClick
import sg.gov.tech.safeentry.selfcheck.HealthStatusApi
import sg.gov.tech.safeentry.selfcheck.model.ConnectionState

class BluetoothHistoryPagerFragment : MainActivityFragment("BluetoothHistoryFragment") {
    override fun didProcessBack() = false
    lateinit var pagerAdapter: HistoryViewPagerAdapter
    private lateinit var header: BarcodeHeaderView
    var navigateToPETab = false

    fun getSelfCheckInData() {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"

        HealthStatusApi.healthStatusApiStatus.observe(viewLifecycleOwner,

            Observer { state ->
                when (state.state) {
                    ConnectionState.Loading -> {
                        try {
                            displayLoader(false)
                        } catch (e: Exception) {
                            CentralLog.e(loggerTAG, e.toString())
                            DBLogger.e(
                                DBLogger.LogType.BLUETRACE,
                                loggerTAG,
                                e.toString(),
                                DBLogger.getStackTraceInJSONArrayString(e)
                            )
                        }
                    }
                    ConnectionState.Done -> {
                        try {
                            hideLoader()
                        } catch (e: Exception) {
                            CentralLog.e(loggerTAG, e.toString())
                            DBLogger.e(
                                DBLogger.LogType.BLUETRACE,
                                loggerTAG,
                                e.toString(),
                                DBLogger.getStackTraceInJSONArrayString(e)
                            )
                        }
                    }

                    ConnectionState.Error -> {
                        try {
                            hideLoader()
                        } catch (e: Exception) {
                            CentralLog.e(loggerTAG, e.toString())
                            DBLogger.e(
                                DBLogger.LogType.BLUETRACE,
                                loggerTAG,
                                e.toString(),
                                DBLogger.getStackTraceInJSONArrayString(e)
                            )
                        }
                        CentralLog.e(loggerTAG, "hmm... ${state.error?.toString()}")
                        DBLogger.e(
                            DBLogger.LogType.BLUETRACE,
                            loggerTAG,
                            "hmm... ${state.error?.toString()}",
                            null
                        )
                    }

                    ConnectionState.NoNetwork -> {
                        try {
                            hideLoader()
                        } catch (e: Exception) {
                            CentralLog.e(loggerTAG, e.toString())
                            DBLogger.e(
                                DBLogger.LogType.BLUETRACE,
                                loggerTAG,
                                e.toString(),
                                DBLogger.getStackTraceInJSONArrayString(e)
                            )
                        }
                        CentralLog.e(loggerTAG, "hmm... ${state.error?.toString()}")
                        DBLogger.e(
                            DBLogger.LogType.BLUETRACE,
                            loggerTAG,
                            "hmm... ${state.error?.toString()}",
                            null
                        )
                    }

                    else -> {

                    }
                }
            }

        )
    }
    fun displayLoader(isBGTranslucent : Boolean){
        progress_bar_vp.visibility = View.VISIBLE
        setLoaderBG(isBGTranslucent)
    }
    fun hideLoader(){
        progress_bar_vp.visibility = View.GONE
    }

    private fun setLoaderBG(isBGTranslucent: Boolean) {
        if (isBGTranslucent) {
            progress_bar_vp.setBackgroundResource(R.color.black)
            progress_bar_vp.alpha = 0.5F
        } else {
            progress_bar_vp.alpha = 1F
            progress_bar_vp.setBackgroundResource(R.color.white)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        header = view.findViewById(R.id.barcode_header)
        header.setTitle(view.context.getString(R.string.title_history))
        header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,SafeEntryActivity.ID_FRAGMENT)
                startActivity(intent)
            }

            override fun onBackPress() {}
        })
        pagerAdapter = if (RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(TracerApp.AppContext))) {
            bt_history_tablayout.tabMode = TabLayout.MODE_SCROLLABLE
            HistoryViewPagerAdapter(TracerApp.AppContext, childFragmentManager, 1)
        } else {
            if (FirebaseRemoteConfig.getInstance()
                    .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
            ) {
                bt_history_tablayout.tabMode = TabLayout.MODE_FIXED
                HistoryViewPagerAdapter(TracerApp.AppContext, childFragmentManager, 2)
            } else {
                bt_history_tablayout.tabMode = TabLayout.MODE_SCROLLABLE
                HistoryViewPagerAdapter(TracerApp.AppContext, childFragmentManager, 1)
            }
        }

        bt_history_view_pager.adapter = pagerAdapter
        bt_history_tablayout.setupWithViewPager(bt_history_view_pager)
        if (navigateToPETab) {
            var handler = Handler()
            handler.postDelayed({
                bt_history_tablayout.getTabAt(1)?.select()
                bt_history_view_pager.setCurrentItem(1, true)
            }, 100)
        }

        if(!RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(TracerApp.AppContext))) {
            getSelfCheckInData()
        }
        else{
            hideLoader()
        }
    }

    override fun onResume() {
        super.onResume()
        bt_history_view_pager.adapter = pagerAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bt_history_view_pager, container, false)
    }


}
