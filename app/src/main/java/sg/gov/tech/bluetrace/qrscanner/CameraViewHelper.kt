package sg.gov.tech.bluetrace.qrscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

class CameraViewHelper(var mActivity: AppCompatActivity, val mSurfaceView: SurfaceView) {
    private val TAG: String = CameraViewHelper::class.java.simpleName
    private var barcodeDetector: BarcodeDetector
    private var cameraSource: CameraSource
    var mListener: QrCodeReaderListener? = null


    var isPaused = true

    companion object {
        val PERMISSION_CALLBACK_CONSTANT = 101
        val REQUEST_PERMISSION_SETTING = 102
    }

    init {
        barcodeDetector = BarcodeDetector.Builder(mActivity)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(mActivity, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

    }

    fun isBarCodeDetectorOperational(): Boolean {
        return barcodeDetector.isOperational
    }

    fun setListener(barcodeReaderListener: QrCodeReaderListener?) {
        mListener = barcodeReaderListener
    }


    private fun initialiseDetectorsAndSources() {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (ActivityCompat.checkSelfPermission(
                        mActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        cameraSource.start(mSurfaceView.holder)
                    }
                    catch (e: Exception){
                        TTAlertBuilder().show(mActivity,AlertType.UNABLE_TO_CONNECT_TO_CAMERA)
                        DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "onCameraError: ${e.message}", null)
                        CentralLog.e(loggerTAG, "onCameraError: ${e.message}")
                    }
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })
        barcodeDetector.setProcessor(object :
            Detector.Processor<Barcode> {
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                try {
                    if (barcodes.size() > 0 && barcodes.valueAt(0).displayValue.isNotBlank() && !isPaused) {
                        mListener?.onScanned(barcodes.valueAt(0))
                    }
                } catch (e: Exception) {
                    CentralLog.e(loggerTAG, "GMS scanner failed to scan: ${e.message}")
                    DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "GMS scanner failed to scan: ${e.message}", null)
                    e.printStackTrace()
                }

            }
        })
    }

    protected fun onPause() {
        cameraSource.release()
    }

    protected fun onResume() {
        initialiseDetectorsAndSources()
    }

    fun pauseScanning() {
        isPaused = true
    }

    fun resumeScanning() {
        isPaused = false
    }

    fun checkCameraPermission() {
        val permissions = Utils.getCameraPermissions()
        when {
            ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initialiseDetectorsAndSources()
            }

//            ActivityCompat.shouldShowRequestPermissionRationale(
//                mActivity,
//                Manifest.permission.CAMERA
//            ) -> {
//
//
//                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
//                    mActivity,
//                    Manifest.permission.CAMERA
//                )
//
//                val permGranted = ActivityCompat.checkSelfPermission(
//                    mActivity,
//                    Manifest.permission.CAMERA
//                )
//                CentralLog.i(TAG, "Perm ${permGranted}, should show rationale?: $shouldShowRationale")
//
//            }
            else -> {


                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,
                    Manifest.permission.CAMERA
                )

                CentralLog.i(TAG, "No perm, should show rationale?: $shouldShowRationale")

                if (shouldShowRationale) {

                    ActivityCompat.requestPermissions(
                        mActivity,
                        permissions,
                        PERMISSION_CALLBACK_CONSTANT
                    )
                }
                else{

                    val userAskedPermissionBefore = Preference.getCameraPermRequestedFlag(mActivity)

                    if(userAskedPermissionBefore) {
                        mActivity.let {
                            TTAlertBuilder().show(it, AlertType.CAMERA_PERMISSION_DIALOG) {
                                if (it) {
                                    //   sentToSettings = true
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", mActivity.packageName, null)
                                    intent.data = uri
                                    mActivity.startActivityForResult(
                                        intent,
                                        PERMISSION_CALLBACK_CONSTANT
                                    )
                                } else {
                                    // mListener!!.onCameraPermissionDenied()
                                }
                            }
                        }
                    }
                    else{
                        ActivityCompat.requestPermissions(
                            mActivity,
                            permissions,
                            PERMISSION_CALLBACK_CONSTANT
                        )
                        Preference.putCameraPermRequestedFlag(mActivity, true)
                    }
                }

            }

        }
    }

    fun permissionCallback(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CALLBACK_CONSTANT) {
            if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                //   mListener?.onCameraPermissionAllowed()
            } else {
                //mListener?.onCameraPermissionDenied()
            }
        }
    }

    fun vibrate() {
        val v = mActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(500)
        }
    }

    interface QrCodeReaderListener {
        fun onScanned(barcode: Barcode?)
        fun onScanError(errorMessage: String?)
        fun onCameraPermissionDenied()
        fun onCameraPermissionAllowed()
    }
}
