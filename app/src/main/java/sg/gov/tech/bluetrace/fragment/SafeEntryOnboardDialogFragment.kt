package sg.gov.tech.bluetrace.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils.dpToPx


class SafeEntryOnboardDialogFragment(private var target: View) : DialogFragment() {
    private lateinit var mCallback: Callback
    override fun onResume() {
        super.onResume()
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.setCancelable(true)
        dialog!!.setCanceledOnTouchOutside(true)
        dialog!!.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog!!.setOnDismissListener { }
        dialog!!.window!!.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
        dialog!!.setOnDismissListener {
            mCallback.onDismissed()
        }
    }

    private fun setDialogPosition() {
        val location = IntArray(2)
        target.getLocationOnScreen(location)
        val sourceX = location[0]
        val sourceY = location[1]
        val window = dialog!!.window
        window!!.setGravity(Gravity.TOP or Gravity.RIGHT)
        val params = window.attributes

        params.x = sourceX - dpToPx(
            activity as MainActivity,
            10f
        ) // about half of confirm button size left of source view
        params.y = sourceY + dpToPx(activity as MainActivity, 60f) // above source view
        window.attributes = params
    }

    fun getFragmentTag(): String {
        return "showcase_SE"
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.se_onboard_dialog_fragment, container, false)
        setDialogPosition()
        return view
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onDismissed()
    }

}
