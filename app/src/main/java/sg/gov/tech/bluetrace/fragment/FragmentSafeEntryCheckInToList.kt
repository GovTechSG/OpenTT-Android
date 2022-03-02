package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_safe_entry_check_in_to_list.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeCheckInToRecyclerViewAdapter
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2

class FragmentSafeEntryCheckInToList : Fragment() {
    private var venueList: ArrayList<QrResultDataModel>? = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_SE_MULTI_TENANT)
        if (!venueList.isNullOrEmpty()) {
            val adapter = SafeCheckInToRecyclerViewAdapter(activity!!)
            adapter.setTenantList(venueList!!.toList())
            safe_check_in_to_rv.layoutManager = LinearLayoutManager(activity)
            safe_check_in_to_rv.adapter = adapter
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_safe_entry_check_in_to_list, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            venueList =
                it.getParcelableArrayList<QrResultDataModel>(SafeEntryCheckInOutActivityV2.SE_VENUE_LIST)
        }
    }
}
