package sg.gov.tech.bluetrace.history

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.dialog_your_data_safe.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.DialogWithCloseFragment

open class YourDataSafeDialogFragment : DialogWithCloseFragment() {

    override fun getLayoutId(): Int {
        return R.layout.dialog_your_data_safe
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dec_tv.text = HtmlCompat.fromHtml(
            getString(R.string.your_history),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

}
