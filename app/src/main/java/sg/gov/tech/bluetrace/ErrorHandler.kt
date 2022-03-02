package sg.gov.tech.bluetrace

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

class ErrorHandler(context: Context) {
    var mContext: Context
    private var onRetryListener: ((Boolean) -> Unit)? = null
    private var dialog: TTAlertBuilder

    init {
        mContext = context
        dialog = TTAlertBuilder()
    }

    fun handleNetworkConnection(isRetry: Boolean = true, onRetry: (Boolean) -> Unit) {
        onRetryListener = onRetry
        if (isInternetAvailable(mContext)) {
            onRetryListener?.invoke(true)
        } else {
            if (isRetry)
                showError()
            //   showInternetConnectionLost()
        }
    }

    fun handleSelfCheckNetworkConnection(onRetry: (Boolean) -> Unit) {
        onRetryListener = onRetry
        if (isInternetAvailable(mContext)) {
            onRetryListener?.invoke(true)
        } else {
            onRetryListener?.invoke(false)
        }
    }

    fun handleSENetworkConnection(isCheckIn: Boolean = true, onRetry: (Boolean) -> Unit) {
        onRetryListener = onRetry
        if (isInternetAvailable(mContext)) {
            onRetryListener?.invoke(true)
        } else {
            var type =
                if (isCheckIn) AlertType.CHECK_IN_NETWORK_ERROR_DIALOG else AlertType.CHECK_IN_NETWORK_ERROR_DIALOG
            showError(type)
        }
    }

    fun showError(type: AlertType = AlertType.NETWORK_ERROR_DIALOG) {
        dialog.show(mContext, type) {
            if (it) {
                handleRational()
            } else {
                //user click retry, sees error and presses cancel
                onRetryListener?.invoke(false)
            }
        }
    }

    fun handleRational() {
        val hThread = HandlerThread("connectivity")
        hThread.start()
        Handler(hThread.looper).post {
            if (isInternetAvailable(mContext)) {
                Handler(Looper.getMainLooper()).post {
                    onRetryListener?.invoke(true)
                }
            } else {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        showError()
                    },
                    500
                )
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    fun unableToReachServer() {
        try {
            dialog.show(mContext, AlertType.UNABLE_TO_REACH_SERVER) {
                if (it) {
                    handleRational()
                }
            }
        } catch (e: Exception) {

        }
    }
}
