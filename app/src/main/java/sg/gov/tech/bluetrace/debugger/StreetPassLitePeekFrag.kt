package sg.gov.tech.bluetrace.debugger

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.database_peek.*
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.streetpass.view.RecordViewModel

class StreetPassLitePeekFrag : Fragment() {

    private lateinit var viewModel: RecordViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.database_peek, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newPeek()

        deviceId.text = "Device id: ${Settings.Secure.getString(
            requireActivity().contentResolver,
            Settings.Secure.ANDROID_ID
        )}"
    }

    private fun newPeek() {
        val context = requireContext()
        val adapter = RecordListAdapter(context)
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        viewModel = ViewModelProvider(this).get(RecordViewModel::class.java)
        viewModel.streetPassLiteRecords.observe(viewLifecycleOwner, Observer { records ->
            adapter.setSourceData(records.map {

                val decoded = Base64.decode(it.msg, Base64.DEFAULT)
                var modelP = decoded[17].toString() + decoded[16].toString()

                var spr = StreetPassRecord(
                    decoded[19].toInt(),
                    it.msg,
                    "GovTech",
                    modelP,
                    TracerApp.asCentralDevice().modelC,
                    it.rssi,
                    it.txPower
                )
                spr.timestamp = it.timestamp
                spr
            })
        })

        expand.setOnClickListener {
            viewModel.allRecords.value?.let {
                adapter.setMode(RecordListAdapter.MODE.ALL)
            }
        }

        collapse.setOnClickListener {
            viewModel.allRecords.value?.let {
                adapter.setMode(RecordListAdapter.MODE.COLLAPSE)
            }
        }

        start.setOnClickListener {
            startService()
        }

        stop.setOnClickListener {
            stopService()
        }

        delete.setOnClickListener { view ->
            view.isEnabled = false

            val builder = AlertDialog.Builder(requireContext())
            builder
                .setTitle("Are you sure?")
                .setCancelable(false)
                .setMessage("Deleting the DB records is irreversible")
                .setPositiveButton("DELETE") { dialog, which ->
                    Observable.create<Boolean> {
                        StreetPassRecordDatabase.getDatabase(requireContext())
                            .bleRecordDao().nukeDb()
                        it.onNext(true)
                    }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe { result ->
                            Toast.makeText(
                                requireContext(),
                                "Database nuked: $result",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            view.isEnabled = true
                            dialog.cancel()
                        }
                }

                .setNegativeButton("DON'T DELETE") { dialog, which ->
                    view.isEnabled = true
                    dialog.cancel()
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()

        }

        plot.setOnClickListener { view ->
            val intent = Intent(requireContext(), PlotActivity::class.java)
            intent.putExtra("time_period", nextTimePeriod())
            intent.putExtra("type","btlite")
            startActivity(intent)
        }

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val serviceUUID = BuildConfig.BLE_SSID
        info.text =
            "UID: ${uid.substring(uid.length - 4)}   SSID: ${BuildConfig.BT_LITE_SSID.substring(0,8 )}"

        if (!BuildConfig.DEBUG) {
            start.visibility = View.GONE
            stop.visibility = View.GONE
            delete.visibility = View.GONE
        }

    }

    private var timePeriod: Int = 0

    private fun nextTimePeriod(): Int {
        timePeriod = when (timePeriod) {
            1 -> 3
            3 -> 6
            6 -> 12
            12 -> 24
            else -> 1
        }

        return timePeriod
    }

    private fun startService() {
        Utils.startBluetoothMonitoringService(requireContext())
    }

    private fun stopService() {
        Utils.stopBluetoothMonitoringService(requireContext())
    }

}
