package sg.gov.tech.bluetrace.fragment

import android.content.Intent
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_bluetooth_history.rv_possible_exposure
import kotlinx.android.synthetic.main.fragment_bluetooth_possible_exposure_history.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.safeentry.selfcheck.HealthStatusApi
import sg.gov.tech.safeentry.selfcheck.model.ConnectionState
import sg.gov.tech.safeentry.selfcheck.model.SafeEntrySelfCheck
import java.util.*

class BluetoothHistoryPossibleExposureFragment :
    MainActivityFragment("BluetoothHistoryFragment") {
    override fun didProcessBack(): Boolean {
        return false
    }

    private fun populateRvList(safeEntryMatches: SafeEntrySelfCheck) {
        CentralLog.i(
            "PossibleExposureFragment",
            "Fetched safeEntryMatches: ${safeEntryMatches.count}"
        )
        safeEntryMatches.data.toTypedArray().sortByDescending { it.safeentry.checkin.time * 1000 }
        val data = safeEntryMatches.data.toTypedArray().groupBy {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.safeentry.checkin.time * 1000
            calendar.get(Calendar.DATE)
        }.toList()
        rv_possible_exposure.layoutManager = LinearLayoutManager(activity)
        rv_possible_exposure.adapter = HistoryPossibleExposureListAdapter(
            activity!!,
            View.OnClickListener {
                val url = BuildConfig.YOUR_POSSIBLE_EXPOSURE_URL
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                try {
                    startActivity(i)
                } catch (e: Exception) {
                    //can't handle browser urls? suppress crash.
                    //todo
                }
            },
            View.OnClickListener {
                val url = BuildConfig.CHECK_FOR_SYMPTOMS_URL
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                try {
                    startActivity(i)
                } catch (e: Exception) {
                    //can't handle browser urls? suppress crash.
                    //todo
                }
            },
            data
        )
        try {
            when (data.isEmpty()) {
                true -> no_contact_possible_exposure_view.visibility = View.VISIBLE
                false -> no_contact_possible_exposure_view.visibility = View.GONE
            }
            progress_bar_possible_exposure.visibility = View.GONE
        } catch (e: java.lang.Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                e.message.toString(),
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("BT_HISTORY", e.message.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        possible_exposure_determined.paintFlags =
            possible_exposure_determined.paintFlags or UNDERLINE_TEXT_FLAG
        possible_exposure_determined.text = HtmlCompat.fromHtml(
            getString(R.string.how_are_my_possible_exposures_determined),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        possible_exposure_determined.setOnClickListener {
            val url = BuildConfig.HOW_POSSIBLE_EXPOSURE_DETERMINED_URL
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            try {
                startActivity(i)
            } catch (e: Exception) {
                //can't handle browser urls? suppress crash.
                //todo
            }
        }

        if (!RegisterUserData.isInvalidPassportOrInvalidUser(
                Preference.getUserIdentityType(
                    TracerApp.AppContext
                )
            )
        ) {
            HealthStatusApi.healthStatusApiStatus.observe(
                viewLifecycleOwner,
                Observer { state ->
                    when (state.state) {
                        ConnectionState.Loading -> {
                            progress_bar_possible_exposure.visibility = View.VISIBLE
                        }

                        ConnectionState.Done -> {
                            state.data?.let {
                                progress_bar_possible_exposure.visibility = View.GONE
                                pe_not_available_view.visibility = View.GONE
                                populateRvList(it.selfCheck)
                                rv_possible_exposure?.adapter?.notifyDataSetChanged()
                            }
                        }

                        ConnectionState.Error -> {
                            try {
                                pe_not_available_view.visibility = View.VISIBLE
                                no_contact_possible_exposure_view.visibility = View.GONE
                                progress_bar_possible_exposure.visibility = View.GONE
                            } catch (e: java.lang.Exception) {
                                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                                CentralLog.e("BT_HISTORY", "error: ${e.message}")
                                DBLogger.e(
                                    DBLogger.LogType.BLUETRACE,
                                    loggerTAG,
                                    "error: ${e.message}",
                                    DBLogger.getStackTraceInJSONArrayString(e)
                                )
                            }
                        }

                        ConnectionState.NoNetwork -> {
                            try {
                                pe_not_available_view.visibility = View.VISIBLE
                                no_contact_possible_exposure_view.visibility = View.GONE
                                progress_bar_possible_exposure.visibility = View.GONE
                            } catch (e: java.lang.Exception) {
                                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                                DBLogger.e(
                                    DBLogger.LogType.BLUETRACE,
                                    loggerTAG,
                                    e.message.toString(),
                                    DBLogger.getStackTraceInJSONArrayString(e)
                                )
                                CentralLog.e("BT_HISTORY", e.message.toString())
                            }
                        }

                        else -> {

                        }
                    }
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_bluetooth_possible_exposure_history,
            container,
            false
        )
    }

}
