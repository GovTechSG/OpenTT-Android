package sg.gov.tech.bluetrace.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.se_showcase_dialog_fragment.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.Utils.dpToPx


class SafeEntryShowcaseDialogFragment(private var mIndex: Int, private var mTarget: View) :
    DialogFragment() {
    private lateinit var mCallback: Callback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.se_showcase_dialog_fragment, container, false)
        setDialogPosition()
        return view
    }

    private fun setDialogPosition() {
        val location = IntArray(2)
        mTarget.getLocationOnScreen(location)
        val sourceX = location[0]
        val sourceY = location[1]
        val window = dialog!!.window
        window!!.setGravity(Gravity.TOP or Gravity.RIGHT)
        val params = window.attributes

        params.x = sourceX - dpToPx(
            activity as SafeEntryActivity,
            10f
        ) // about half of confirm button size left of source view
        params.y = sourceY - dpToPx(activity as SafeEntryActivity, 530f) // above source view
        window.attributes = params
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.setCancelable(false)
        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog!!.setOnDismissListener {
            mCallback.onDismissed()
        }

        btn_cool.setOnClickListener({
            mCallback.onButtonClicked(mIndex)
            dismiss()
        })

        when (mIndex) {
            0 -> {
                img_scan_qr.setImageDrawable(resources.getDrawable(R.drawable.img_scan_qr))
                tv_title.text = getString(R.string.scan_and_go)
                tv_desc.text = getString(R.string.no_more_form_filling)
                btn_cool.text = getString(R.string.cool)
            }
            1 -> {
                img_scan_qr.setImageDrawable(resources.getDrawable(R.drawable.img_favourite))
                tv_title.text = getString(R.string.visit_place_often)
                tv_desc.text = getString(R.string.tap_on_star_text)
                btn_cool.text = getString(R.string.awesome)
            }
            2 -> {
                img_scan_qr.setImageDrawable(resources.getDrawable(R.drawable.img_scan_nric))
                tv_title.text = getString(R.string.need_identity)
                tv_desc.text = getString(R.string.use_digital_barcode)
                btn_cool.text = getString(R.string.got_it_exclamation_mark)
            }
        }

    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onDismissed()
        fun onButtonClicked(index: Int)
    }

}
