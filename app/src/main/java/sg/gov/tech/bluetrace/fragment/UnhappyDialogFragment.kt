package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_unhappy_app_not_working.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class UnhappyDialogFragment : FullScreenDialogFragment() {
    var buttonListener: (() -> Unit)? = null

    companion object {
        fun LOCATION_PERMISSION(context: Context): String {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                return context.getString(
                    R.string.location_permission_adr11,
                    context.getString(R.string.location_permission),
                    context.getString(R.string.allow_while_using_app)
                )
            }
            return context.getString(R.string.location_permission)
        }

        fun BLUETOOTH (context: Context): String{
            return context.getString(R.string.bluetooth)
        }

        fun IGNORE_BATTERY_OPTIMIZATION (context: Context): String {
            return context.getString(R.string.battery_optimiser_opt)
        }
    }

    var points: Array<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_unhappy_app_not_working, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_HOME_APP_NOT_WORKING)
        allow.setOnClickListener { buttonListener?.invoke() }

        updatePoints()
    }

    fun updatePoints() {
        points?.let { pts ->
            val setTextPointer: (TextView, TextView, String?, Int) -> Unit = { t1, t2, s, v ->
                t1.visibility = v
                t2.visibility = v
                if (s != null) t2.text = s
            }
            for (p in 0..3) {
                val visibility = if (p < pts.size) View.VISIBLE else View.GONE
                val text = if (p < pts.size) pts[p] else null
                when (p) {
                    0 -> setTextPointer(one, tFirst, text, visibility)
                    1 -> setTextPointer(two, tSecond, text, visibility)
                    2 -> setTextPointer(three, tThird, text, visibility)
                    3 -> setTextPointer(four, tFourth, text, visibility)
                }
            }
        }
    }

    fun buttonListener(f: () -> Unit) {
        buttonListener = f
    }
}
