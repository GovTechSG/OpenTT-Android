package sg.gov.tech.bluetrace.utils

import android.app.AlertDialog
import android.content.Context
import sg.gov.tech.bluetrace.R

enum class AlertType {
    NETWORK_ERROR_DIALOG,
    CAMERA_PERMISSION_DIALOG,
    CHECK_IN_NETWORK_ERROR_DIALOG,
    CHECK_OUT_NETWORK_ERROR_DIALOG,
    NON_SE_OR,
    SE_NOT_AVAILABLE,
    UNABLE_TO_REACH_SERVER,
    FAVOURITE_CHECK_IN_ERROR,
    UNABLE_TO_SCAN_QR,
    TOO_MANY_TRIES_ERROR,
    UNABLE_TO_CONNECT_TO_CAMERA

}

class TTAlertBuilder {
    lateinit var builder: AlertDialog.Builder
    lateinit var dialog: AlertDialog

    fun show(
        mContext: Context,
        type: AlertType,
        isCancel: Boolean = false,
        onClick: ((Boolean) -> Unit)? = null
    ) {
        builder = AlertDialog.Builder(mContext)
        var title = ""
        var msg = ""
        var negButton = ""
        var posButton = ""

        when (type) {
            AlertType.NETWORK_ERROR_DIALOG -> {
                title = mContext.resources.getString(R.string.check_your_connection)
                msg = mContext.resources.getString(R.string.there_seems)
                posButton = mContext.resources.getString(R.string.retry)
                negButton = mContext.resources.getString(R.string.cancel)
            }
            AlertType.CAMERA_PERMISSION_DIALOG -> {
                msg = mContext.resources.getString(R.string.app_needs_camera)
                posButton = mContext.resources.getString(R.string.go_to_settings)
                negButton = mContext.resources.getString(R.string.cancel)
            }
            AlertType.CHECK_IN_NETWORK_ERROR_DIALOG -> {
                title = mContext.resources.getString(R.string.unable_to_in)
                msg = mContext.resources.getString(R.string.network_issue_text)
                posButton = mContext.resources.getString(R.string.retry)
                negButton = mContext.resources.getString(R.string.cancel)
            }
            AlertType.CHECK_OUT_NETWORK_ERROR_DIALOG -> {
                title = mContext.resources.getString(R.string.unable_to_out)
                msg = mContext.resources.getString(R.string.network_issue_text)
                posButton = mContext.resources.getString(R.string.retry)
                negButton = mContext.resources.getString(R.string.cancel)
            }
            AlertType.NON_SE_OR -> {
                title = mContext.resources.getString(R.string.non_se_qr)
                msg = mContext.resources.getString(R.string.ensure_you)
                posButton = mContext.resources.getString(R.string.scan_again)
                negButton = mContext.resources.getString(R.string.cancel)
            }
            AlertType.SE_NOT_AVAILABLE -> {
                title = mContext.resources.getString(R.string.se_unavailable)
                msg = mContext.resources.getString(R.string.consider_us)
                posButton = mContext.resources.getString(R.string.scan_again)
            }
            AlertType.UNABLE_TO_REACH_SERVER -> {
                title = mContext.resources.getString(R.string.temporarily)
                msg = mContext.resources.getString(R.string.we_re_reall)
                posButton = mContext.resources.getString(R.string.ok)
            }
            AlertType.FAVOURITE_CHECK_IN_ERROR -> {
                title = mContext.resources.getString(R.string.favourite_check_in_error_title)
                msg = mContext.resources.getString(R.string.favourite_check_in_error_message)
                posButton = mContext.resources.getString(R.string.ok)
            }
            AlertType.UNABLE_TO_SCAN_QR -> {
                title = mContext.getString(R.string.safe_entry_unable_to_scan_qr)
                msg = mContext.getString(R.string.check_google_play_services_is_updated)
                posButton = mContext.resources.getString(R.string.ok)
            }
            AlertType.UNABLE_TO_CONNECT_TO_CAMERA -> {
                title = mContext.getString(R.string.unable_to_connect_to_camera)
                msg = mContext.getString(R.string.consider_us)
                posButton = mContext.resources.getString(R.string.ok)
            }
            AlertType.TOO_MANY_TRIES_ERROR -> {
                title = mContext.getString(R.string.please_try_again_later)
                msg = mContext.getString(R.string.error_tries)
                posButton = mContext.resources.getString(R.string.ok)
            }
        }


        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setCancelable(isCancel)
        if (negButton.isNotEmpty())
            builder.setNegativeButton(negButton) { dialog, which ->
                dialog.dismiss()
                onClick?.invoke(false)

            }
        builder.setPositiveButton(posButton) { dialog, which ->
            dialog.dismiss()
            onClick?.invoke(true)
        }
        dialog = builder.create()
        dialog.show()
    }
}