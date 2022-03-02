package sg.gov.tech.bluetrace.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_dialog_is_jailbroken.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.revamp.splash.JailbrokenDialogCallback


class JailbrokenDialogFragment(var callback: JailbrokenDialogCallback) : DialogFragment() {
    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun getFragmentTag(): String {
        return "jailbroken"
    }

    private fun handleForcedUpdate() {
        dialogJailbrokenOkButton.setOnClickListener {
            dismiss()
            callback.onJaibrokenDialogClosed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dialog_is_jailbroken, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleForcedUpdate()
    }
}

