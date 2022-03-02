package sg.gov.tech.bluetrace.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MLKitScanner(var context: Context, var cameraPreview: AutoFitSurfaceView) {

    companion object {
        val PERMISSION_CALLBACK_CONSTANT_MLKIT = 201
    }

    private val TAG = "MLKitScanner"

    /**
     * boolean to send a  new frame to processor
     * only when it has finished processing last frame
     */
    private var isFrameProcessed = true

    /**
     * manages QR code detection
     */
    private var isScanningPaused = true
    var cameraDevice: CameraDevice? = null
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val qrCodeScanner = BarcodeScanning.getClient(options)
    var processorJob: Job? = null


    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread")

    /** [Handler] corresponding to [cameraThread] */
    private var cameraHandler: Handler

    private val cameraManager: CameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private val cameraId: String? by lazy {
        cameraManager.cameraIdList.find {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            return@find (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_BACK)
        }

    }
    var mListener: MLKitQrCodeReaderListener? = null

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val cameraCharacteristics: CameraCharacteristics? by lazy {
        cameraId?.let { cameraManager.getCameraCharacteristics(it) }
    }

    init {
        cameraThread.start()
        cameraHandler = Handler(cameraThread.looper)
    }

    fun setCameraPreviewSize() {
        //set the preview size
        val previewSize = cameraCharacteristics?.let {
            CameraHelper.getPreviewOutputSize(
                cameraPreview.display, it, SurfaceHolder::class.java
            )
        }
        if (previewSize != null) {
            cameraPreview.setAspectRatio(previewSize.width, previewSize.height)
        }
    }

    fun startCameraPreview() {
        val pSize = cameraCharacteristics?.let {
            CameraHelper.getPreviewOutputSize(
                cameraPreview.display,
                it,
                SurfaceHolder::class.java
            )
        }

        val cameraStateCallbacks = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {

                cameraDevice = camera
                val imgReader = if (pSize != null) {
                    ImageReader.newInstance(
                        pSize.width,
                        pSize.height,
                        ImageFormat.YUV_420_888,
                        4
                    )
                } else {
                    ImageReader.newInstance(
                        cameraPreview.width,
                        cameraPreview.height,
                        ImageFormat.YUV_420_888,
                        4
                    )
                }
                imgReader.setOnImageAvailableListener({ reader ->

                    var rotation =
                        CameraHelper.getRotationCompensation(
                            cameraId!!,
                            context,
                            false
                        )
                    val img = reader.acquireNextImage()
                    if (isFrameProcessed && !isScanningPaused) {
                        processorJob = CoroutineScope(Dispatchers.IO).launch {
                            var barcodeList = processImageFrame(img, rotation, qrCodeScanner)
                            if (barcodeList != null && barcodeList.isNotEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    pauseQRCodeScanning()
                                    mListener?.onScanned(barcodeList)
                                    processorJob?.cancel()
                                }
                            }
                        }

                    } else {
                        img.close()
                    }
                }, cameraHandler)
                val captureStateCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                        CentralLog.e(loggerTAG, "MLKit camera configuration failed")
                        DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "MLKit camera configuration failed", null)
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        val builder =
                            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        builder?.addTarget(cameraPreview.holder.surface)
                        builder?.addTarget(imgReader.surface)
                        session.setRepeatingRequest(
                            builder!!.build(),
                            null,
                            cameraHandler
                        )
                    }
                }
                cameraDevice?.createCaptureSession(
                    listOf(cameraPreview.holder.surface, imgReader.surface),
                    captureStateCallback, cameraHandler
                )
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice = camera
                closeCamera()
            }

            override fun onError(camera: CameraDevice, p1: Int) {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                CentralLog.e(loggerTAG, "MLKit camera error: $p1")
                DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "MLKit camera error: $p1", null)
                cameraDevice = camera
                closeCamera()
            }
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraId?.let { cameraManager.openCamera(it, cameraStateCallbacks, cameraHandler) }
        }
    }

    /**
     * Closes the current camera.
     */
    fun closeCamera() {
        try {
            if (cameraDevice != null) {
                cameraDevice?.close()
                cameraDevice = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        }
    }

    /**
     * function to process the image frame captured
     */
    suspend fun processImageFrame(
        image: Image,
        rotation: Int,
        mQrCodeScanner: BarcodeScanner
    ): List<Barcode>? {

        isFrameProcessed = false
        val imageNew = InputImage.fromMediaImage(image, rotation)
        return suspendCoroutine { continuation ->
            mQrCodeScanner.process(imageNew)
                .addOnSuccessListener { barcodeList ->
                    if (barcodeList.size > 0) {
                        image.close()
                        isFrameProcessed = true
                        continuation.resume(barcodeList)
                    } else {
                        image.close()
                        isFrameProcessed = true
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(loggerTAG, "MLKit failed to scan: ${it.message}")
                    DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "MLKit failed to scan: ${it.message}", null)
                    image.close()
                    isFrameProcessed = true
                    continuation.resume(null)
                }
        }
    }

    fun stopCameraThread() {
        cameraThread.quitSafely()
    }

    fun checkCameraPermission() {
        val permissions = Utils.getCameraPermissions()
        when {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.CAMERA
            ) -> {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    permissions,
                    PERMISSION_CALLBACK_CONSTANT_MLKIT
                )
            }
            else -> {
                context.let { mContext ->
                    TTAlertBuilder().show(mContext, AlertType.CAMERA_PERMISSION_DIALOG) {
                        if (it) {
                            //   sentToSettings = true
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            (context as Activity).startActivityForResult(
                                intent,
                                PERMISSION_CALLBACK_CONSTANT_MLKIT
                            )
                        }
                    }
                }
            }
        }
    }

    fun setListener(barcodeReaderListener: MLKitQrCodeReaderListener) {
        mListener = barcodeReaderListener
    }

    interface MLKitQrCodeReaderListener {
        fun onScanned(barcodeList: List<Barcode>)
    }

    fun resumeQRCodeScanning() {
        isScanningPaused = false
    }
    fun pauseQRCodeScanning(){
        isScanningPaused = true
    }
}