package sg.gov.tech.bluetrace.healthStatus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.settings.BarcodeHeaderView
import sg.gov.tech.bluetrace.settings.OnBarcodeClick
import sg.gov.tech.bluetrace.utils.AndroidBus
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment
import sg.gov.tech.safeentry.selfcheck.model.HealthStatus

class CovidHealthStatusFragment : MainActivityFragment("CovidHealthStatusFragment"),
    HealthStatusListAdapter.Callback {

    private lateinit var barcodeHeader: BarcodeHeaderView
    private lateinit var rvHealthStatus: RecyclerView
    private lateinit var svHealthStatus: ScrollView
    private var healthStatusPosition = 0

    companion object {
        const val HEALTH_STATUS_POSITION = "HEALTH_STATUS_POSITION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            healthStatusPosition = bundle.getInt(HEALTH_STATUS_POSITION, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_covid_health_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        getData()
    }

    private fun initViews(view: View) {
        barcodeHeader = view.findViewById(R.id.barcode_header)
        rvHealthStatus = view.findViewById(R.id.rv_health_status)
        svHealthStatus = view.findViewById(R.id.sv_health_status)
        barcodeHeader.setTitle(getString(R.string.covid_health_status))
        barcodeHeader.showBackNavigationImage()
        barcodeHeader.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(
                    SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,
                    SafeEntryActivity.ID_FRAGMENT
                )
                startActivity(intent)
            }

            override fun onBackPress() {
                (activity as MainActivity).onBackPressed()
            }
        })
    }

    private fun getData() {
        AndroidBus.healthStatus.subscribe { healthStatus ->
            setAdapter(healthStatus)
        }.dispose()
    }

    private fun setAdapter(healthStatus: HealthStatus) {
        rvHealthStatus.layoutManager = LinearLayoutManager(context)
        val adapter = HealthStatusListAdapter(requireContext(), healthStatus)
        adapter.addCallback(this)
        rvHealthStatus.adapter = adapter
        // Disabled nested scrolling since Parent scrollview will scroll the content.
        rvHealthStatus.isNestedScrollingEnabled = false
        // Scroll to a particular position
        svHealthStatus.post {
            rvHealthStatus.adapter?.let {
                if (healthStatusPosition < it.itemCount) {
                    svHealthStatus.smoothScrollTo(0, rvHealthStatus[healthStatusPosition].top)
                } else {
                    svHealthStatus.smoothScrollTo(0, svHealthStatus.bottom)
                }
            }
        }
    }

    override fun onItemClick(urlLink: String, isPossibleExposure: Boolean) {
        if (!isPossibleExposure) {
            if (urlLink.contains("https://support.tracetogether.gov.sg/")) {
                // Open link in in-app browser
                val fragment = WebViewZendeskSupportFragment()
                fragment.setUrl(urlLink)
                childFragmentManager.beginTransaction()
                    .addToBackStack(fragment.customTag)
                    .replace(R.id.fragment_child_content, fragment)
                    .commit()
            } else {
                // Open link in external browser
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(urlLink)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    //can't handle browser urls? suppress crash.
                    //todo
                }
            }
        } else {
            // Goto possible exposure screen
            (activity as MainActivity?)?.goToPossibleExposure()
        }
    }

    override fun didProcessBack(): Boolean {
        if (isAdded) {
            return if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
                true
            } else false
        }
        return false
    }
}
