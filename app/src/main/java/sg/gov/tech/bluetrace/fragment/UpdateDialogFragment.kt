package sg.gov.tech.bluetrace.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.update_app_forced_dialog_fragment.*
import kotlinx.android.synthetic.main.update_app_optional_dialog_fragment.*
import sg.gov.tech.bluetrace.Preference.putshouldShowOptionalUpdateDialog
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils.redirectToPlayStore
import sg.gov.tech.bluetrace.revamp.home.HomeFragmentV3

class UpdateDialogFragment(private var isForcedUpdate: Boolean = false) : DialogFragment() {
    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.setCancelable(!isForcedUpdate)
    }

    fun getFragmentTag(): String {
        return if (isForcedUpdate) "force_update" else "optional_update"
    }

    private fun handleForcedUpdate() {
        forcedUpdateDialogUpdateButton.setOnClickListener {
            redirectToPlayStore(requireContext())
        }
    }

    private fun handleOptionalUpdate() {
        optionalUpdateDialogButton.setOnClickListener {
            redirectToPlayStore(requireContext())
        }
        remindMeAgainDialogButton.setOnClickListener {
            putshouldShowOptionalUpdateDialog(requireContext(), false)
            activity?.let {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.content)
                if (fragment is SafeEntryFragment) {
                    fragment.isUpdateAppPopUpShown = false
                    fragment.updateAllowQRScanning()
                    fragment.activityFragmentManager.dismiss(getFragmentTag())
                } else if (fragment is HomeFragmentV3) {
                    fragment.activityFragmentManager.dismiss(getFragmentTag())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return when (isForcedUpdate) {
            true -> inflater.inflate(R.layout.update_app_forced_dialog_fragment, container, false)
            false -> inflater.inflate(
                R.layout.update_app_optional_dialog_fragment,
                container,
                false
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (isForcedUpdate) {
            true -> handleForcedUpdate()
            false -> handleOptionalUpdate()
        }
    }
}
