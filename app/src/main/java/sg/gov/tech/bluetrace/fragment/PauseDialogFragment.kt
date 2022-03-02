package sg.gov.tech.bluetrace.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.paused_overlay.*
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils
import java.util.concurrent.TimeUnit

class PauseDialogFragment : FullScreenDialogFragment() {

    private var countDownTimer: CountDownTimer? = null
    private var pauseDialogDismissListener: PauseDialogDismissListener? = null
    var activityFragmentManager: ActivityFragmentManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.paused_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_resume.setOnClickListener {
            Utils.pauseBluetoothMonitoringService(TracerApp.AppContext, -1)
        }
    }
    fun setDialogDismissListener(listener : PauseDialogDismissListener){
        pauseDialogDismissListener = listener
    }
    private var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Preference.PAUSE_UNTIL -> {
                    if (!Preference.shouldBePaused(TracerApp.AppContext) && activityFragmentManager != null) {
                        activityFragmentManager?.dismiss("PDF")
                    }
                }
            }
        }

    override fun onResume() {
        super.onResume()
        Preference.registerListener(TracerApp.AppContext, preferenceListener)
        startCountdownTimer()
    }

    private fun startCountdownTimer() {
        val timeToStop = Preference.getPauseUntil(TracerApp.AppContext)
        val millisLeft = timeToStop - System.currentTimeMillis()

        countDownTimer = object : CountDownTimer(millisLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                displayTimeLeft(millisUntilFinished)
            }

            override fun onFinish() {
                dialog?.cancel()
            }
        }

        countDownTimer?.start()
    }

    private fun displayTimeLeft(timeLeftInMillis: Long) {

        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) % 60

        if (hours != 0L) {
            paused_for.text = getString(R.string.paused_hms, hours, minutes, seconds)
        } else if (hours == 0L && minutes != 0L) {
            paused_for.text = getString(R.string.paused_ms, minutes, seconds)
        } else {
            paused_for.text = getString(R.string.paused_s, seconds)
        }

    }

    override fun onPause() {
        super.onPause()
        Preference.unregisterListener(TracerApp.AppContext, preferenceListener)
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
        pauseDialogDismissListener?.onDialogDismiss()
    }
}
interface PauseDialogDismissListener{
    fun onDialogDismiss()
}
