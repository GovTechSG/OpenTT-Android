package sg.gov.tech.bluetrace.revamp.safeentry

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.navigation.fragment.NavHostFragment
import kotlinx.android.synthetic.main.activity_safe_check_in_out_v2.*
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.utils.AndroidBus

class SafeEntryCheckInOutActivityV2 : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var safeEntryCheckInOutBackButton: AppCompatImageButton
    private var seFragmentValue = SE_CHECK_IN_VALUE
    private var venue: QrResultDataModel? = null
    private var venueList = ArrayList<QrResultDataModel>()
    internal var familyMembersList = ArrayList<FamilyMembersRecord>()

    companion object {
        const val SE_CHECK_IN_VALUE = 0
        const val SE_VIEW_PASS_VALUE = 1
        const val SE_CHECK_OUT_VALUE = 2
        const val SE_FRAGMENT_VALUE = "fragmentValue"
        const val SE_VENUE = "venue"
        const val SE_VENUE_LIST = "venue_list"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_check_in_out_v2)

        initViews()
        getData()
        setNavigationDestination()
    }

    private fun initViews() {
        safeEntryCheckInOutBackButton = findViewById(R.id.safe_entry_back_button)
        safeEntryCheckInOutBackButton.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (navHostFragment.navController.currentDestination?.id != R.id.fragmentSafeEntryCheckInList)
            goToHome()
        else
            super.onBackPressed()
    }

    private fun getData() {
        intent.extras?.let {
            seFragmentValue = it.getInt(SE_FRAGMENT_VALUE)
            venue = it.getParcelable(SE_VENUE)

            if (seFragmentValue == SE_CHECK_IN_VALUE) {
//                AndroidBus.behaviorSubject.subscribe { list ->
//                    venueList = list
//                }.dispose()
                venueList = it.getParcelableArrayList<QrResultDataModel>(SE_VENUE_LIST) as ArrayList<QrResultDataModel>
            }

            if (intent.hasExtra(SafeEntryActivity.IS_FROM_GROUP_CHECK_IN)) {
                if (it.getBoolean(SafeEntryActivity.IS_FROM_GROUP_CHECK_IN, false)) {
                    AndroidBus.familyMembersList.subscribe { membersList ->
                        familyMembersList = membersList
                    }.dispose()
                }
            }
        }
    }

    private fun setNavigationDestination() {
        navHostFragment = safe_entry_navigation_host as NavHostFragment
        val graph =
            navHostFragment.navController.navInflater.inflate(R.navigation.safe_entry_check_in_out_navigation_v2)
        val bundle = Bundle()
        when (seFragmentValue) {
            SE_CHECK_IN_VALUE -> {
                /*
                There is a chance that venueList is still size = 0 at this point when it should not be empty.
                Try to retrieve the value again
                 */
                if (venueList.size == 0) {
                    getData()
                }

                if (venueList.size > 1) {
                    graph.startDestination = R.id.fragmentSafeEntryCheckInList
                    bundle.putParcelableArrayList(SE_VENUE_LIST, venueList)
                } else {
                    graph.startDestination = R.id.fragmentSafeEntryCheckIn
                    bundle.putParcelable(SE_VENUE, venueList[0])
                }
            }
            SE_VIEW_PASS_VALUE -> {
                graph.startDestination = R.id.fragmentSafeEntryViewPass
                bundle.putParcelable(SE_VENUE, venue)
            }
            SE_CHECK_OUT_VALUE -> {
                graph.startDestination = R.id.fragmentSafeEntryCheckOut
                bundle.putParcelable(SE_VENUE, venue)
            }
            else -> {
                graph.startDestination = R.id.fragmentSafeEntryViewPass
                bundle.putParcelable(SE_VENUE, venue)
            }
        }
        navHostFragment.navController.setGraph(graph, bundle)
    }

    fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.GO_TO_HOME, true)
        setResult(SafeEntryActivity.ACTION_FINISH)
        startActivity(intent)
        finish()
    }
}