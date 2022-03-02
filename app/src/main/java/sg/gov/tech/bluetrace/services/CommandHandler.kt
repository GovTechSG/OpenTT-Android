package sg.gov.tech.bluetrace.services

import android.os.Handler
import android.os.Message
import sg.gov.tech.bluetrace.logging.CentralLog
import java.lang.ref.WeakReference

class CommandHandler(val service: WeakReference<BluetoothMonitoringService>) : Handler() {

    private val TAG = "CommandHandler"

    override fun handleMessage(msg: Message?) {
        msg?.let {
            //            val cmd = msg.arg1
            val cmd = msg.what
            service.get()?.runService(BluetoothMonitoringService.Command.findByValue(cmd), null)
        }
    }

    fun sendCommandMsg(cmd: BluetoothMonitoringService.Command, delay: Long) {
//        val msg = obtainMessage(cmd.index)
        val msg = Message.obtain(this, cmd.index)
//        msg.arg1 = cmd.index
        sendMessageDelayed(msg, delay)
    }

    fun sendCommandMsg(cmd: BluetoothMonitoringService.Command) {
        val msg = obtainMessage(cmd.index)
        msg.arg1 = cmd.index
        sendMessage(msg)
    }

    fun startBluetoothMonitoringService() {
        sendCommandMsg(BluetoothMonitoringService.Command.ACTION_START)
    }

    fun selfCheck() {
        sendCommandMsg(BluetoothMonitoringService.Command.ACTION_SELF_CHECK)
    }

    fun scheduleNextBTScan(timeInMillis: Long) {
        CentralLog.d(TAG, "scheduling BT Scan")
        cancelNextBTScan()
        sendCommandMsg(BluetoothMonitoringService.Command.ACTION_BT_SCAN, timeInMillis)
    }

    fun scheduleNextBTLScan(timeInMillis: Long) {
        CentralLog.d(TAG, "scheduling BTL scan")
        cancelNextBTLScan()
        sendCommandMsg(BluetoothMonitoringService.Command.ACTION_BTL_SCAN, timeInMillis)
    }

    fun cancelNextBTScan() {
        removeMessages(BluetoothMonitoringService.Command.ACTION_BT_SCAN.index)
    }

    fun cancelNextBTLScan() {
        removeMessages(BluetoothMonitoringService.Command.ACTION_BTL_SCAN.index)
    }

    fun hasBTScanScheduled(): Boolean {
        return hasMessages(BluetoothMonitoringService.Command.ACTION_BT_SCAN.index)
    }

    fun hasBTLScanScheduled(): Boolean {
        return hasMessages(BluetoothMonitoringService.Command.ACTION_BTL_SCAN.index)
    }

    fun scheduleNextAdvertise(timeInMillis: Long) {
        CentralLog.d(TAG, "scheduling advert")
        cancelNextAdvertise()
        sendCommandMsg(BluetoothMonitoringService.Command.ACTION_ADVERTISE, timeInMillis)
    }

    fun cancelNextAdvertise() {
        removeMessages(BluetoothMonitoringService.Command.ACTION_ADVERTISE.index)
    }

    fun hasAdvertiseScheduled(): Boolean {
        return hasMessages(BluetoothMonitoringService.Command.ACTION_ADVERTISE.index)
    }
}
