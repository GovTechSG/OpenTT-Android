package sg.gov.tech.bluetrace.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import sg.gov.tech.bluetrace.ErrorHandler


class ConnectionStateMonitor(private val mContext: Context) :
    LiveData<Boolean?>() {
    private lateinit var networkCallback: NetworkCallback
    private lateinit var networkReceiver: NetworkReceiver
    private var connectivityManager: ConnectivityManager =
        mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    private lateinit var errorHandler: ErrorHandler

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = NetworkCallback(this)
        } else {
            networkReceiver = NetworkReceiver()
        }
        errorHandler = ErrorHandler(mContext)
    }

    override fun onActive() {
        super.onActive()
        updateConnection()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } else {
            mContext.registerReceiver(
                networkReceiver,
                IntentFilter(CONNECTIVITY_ACTION)
            )
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } else {
            mContext.unregisterReceiver(networkReceiver)
        }
    }

    internal inner class NetworkCallback(private val mConnectionStateMonitor: ConnectionStateMonitor) :
        ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (network != null) {
                mConnectionStateMonitor.postValue(true)
            }
        }

        override fun onLost(network: Network) {
            mConnectionStateMonitor.postValue(false)
        }

    }

    private fun updateConnection() {
        errorHandler.handleSelfCheckNetworkConnection {
            postValue(it)
        }
    }

    internal inner class NetworkReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action == CONNECTIVITY_ACTION) {
                updateConnection()
            }
        }
    }

}