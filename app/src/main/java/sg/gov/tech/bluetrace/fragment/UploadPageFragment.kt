package sg.gov.tech.bluetrace.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_upload_page.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord


data class ExportData(
    val recordList: List<StreetPassRecord>,
    val recordLiteList: List<StreetPassLiteRecord>,
    val statusList: List<StatusRecord>,
    val strings: List<String>
)

class UploadPageFragment : MainActivityFragment("UploadMain") {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var uuidString = BuildConfig.BLE_SSID

        fragment_buildno.text = "${BuildConfig.GITHASH}-${uuidString}"

        uuidString = uuidString.substring(uuidString.length - 4)
        activity?.let {
            val ttId: String = Preference.getTtID(it)
            fragment_buildno.text = "${BuildConfig.GITHASH}-${uuidString}@${ttId}"
        }

        val childFragMan: FragmentManager = childFragmentManager
        val childFragTrans: FragmentTransaction = childFragMan.beginTransaction()
        val fragB = EnterPinFragment()
        childFragTrans.add(R.id.fragment_placeholder, fragB)
        childFragTrans.addToBackStack(fragB.customTag)
        childFragTrans.commit()
    }

    fun turnOnLoadingProgress() {
        uploadPageFragmentLoadingProgressBarFrame.visibility = View.VISIBLE
    }

    fun turnOffLoadingProgress() {
        uploadPageFragmentLoadingProgressBarFrame.visibility = View.INVISIBLE
    }

    fun navigateToUploadPin() {
        val parentFragMan: FragmentManager = parentFragmentManager
        val parentFragTrans: FragmentTransaction = parentFragMan.beginTransaction()
        val fragB = ForUseFragment()
        parentFragTrans.add(R.id.fragment_placeholder, fragB)
        parentFragTrans.addToBackStack(fragB.customTag)
        parentFragTrans.commit()
    }

    fun goBackToHome() {
        var parentActivity = activity as MainActivity
        parentActivity.goToHome()
    }

    fun navigateToUploadComplete(isForCPC: Boolean) {
        val parentFragMan: FragmentManager = parentFragmentManager
        parentFragmentManager.popBackStackImmediate()

        val parentFragTrans: FragmentTransaction = parentFragMan.beginTransaction()
        val fragB = UploadCompleteFragment()

        val bundleArgs = Bundle()
        bundleArgs.putBoolean(UploadCompleteFragment.ARG_IS_FOR_CPC, isForCPC)
        fragB.arguments = bundleArgs
        parentFragTrans.add(R.id.fragment_placeholder, fragB)
        parentFragTrans.addToBackStack("UploadComplete")
//        childFragTrans.replace(R.id.fragment_placeholder, fragB)
        parentFragTrans.commit()
    }

    override fun didProcessBack(): Boolean {
        if (isAdded) {
            return if (childFragmentManager.backStackEntryCount > 1) {
                childFragmentManager.popBackStackImmediate()
                true
            } else
                false
        }
        return false
    }
}
