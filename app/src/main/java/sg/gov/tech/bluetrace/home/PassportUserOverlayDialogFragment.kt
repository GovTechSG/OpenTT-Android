package sg.gov.tech.bluetrace.home

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.dialog_passport_user.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.DialogWithCloseFragment

open class PassportUserOverlayDialogFragment : DialogWithCloseFragment() {

    override fun getLayoutId(): Int {
        return R.layout.dialog_passport_user
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        description_tv.text = HtmlCompat.fromHtml(
            getString(R.string.passport_user_overlay_detail_text),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}