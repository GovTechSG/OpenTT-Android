package sg.gov.tech.bluetrace.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.vision.barcode.Barcode
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.revamp.responseModel.SeVenueList
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.AndroidBus
import sg.gov.tech.bluetrace.utils.TTAlertBuilder


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [QrScannerFragmentV2.newInstance] factory method to
 * create an instance of this fragment.
 */
class QrScannerFragmentV2 : Fragment(), CameraViewHelper.QrCodeReaderListener,
    MLKitScanner.MLKitQrCodeReaderListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val TAG: String = QrScannerFragmentV2::class.java.simpleName
    private lateinit var permissionRoot: ConstraintLayout
    private lateinit var scannerRootView: ConstraintLayout
    private lateinit var mAllowButton: AppCompatButton
    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }
    private val dialog: TTAlertBuilder by inject()
    private lateinit var mContext: Context
    private lateinit var surfaceView: SurfaceView
    private lateinit var txtTerms: AppCompatTextView
    private lateinit var txtNotApplicable: AppCompatTextView
    private lateinit var mlKitCameraPreview: AutoFitSurfaceView
    private var mlKitScanner: MLKitScanner? = null
    private var barcodeReader: CameraViewHelper? = null
    val vm: QrScannerModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isTutorialCompleted = it.getBoolean(
                ARG_PARAM1
            )
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment QrScannerFragmentV2.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Boolean) =
            QrScannerFragmentV2().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PARAM1, param1)

                }
            }

        var isTutorialCompleted = false
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContext = view.context
        initViews(view)
    }

    private fun initViews(view: View) {
        txtTerms = view.findViewById(R.id.txtTerms)
        txtNotApplicable = view.findViewById(R.id.txtNotApplicable)
        permissionRoot = view.findViewById(R.id.permissionRoot)
        scannerRootView = view.findViewById(R.id.scannerRootView)
        mAllowButton = view.findViewById(R.id.allow)
        surfaceView = view.findViewById(R.id.surfaceView)
        mlKitCameraPreview = view.findViewById(R.id.mlKitCameraPreview)
        barcodeReader = activity?.let { CameraViewHelper(it as AppCompatActivity, surfaceView) }
        val declarationString = getString(
            R.string.combined_terms_string,
            getString(R.string.you_have_no_close_contact),
            getString(R.string.you_have_no_quarantine),
            getString(R.string.no_fever),
            getString(R.string.agree_to_the_terms_se)
        )
        txtTerms.text = declarationString
        val termsString = resources.getString(R.string.terms)

        txtTerms.makeLinks(
            Pair(termsString, View.OnClickListener {
                val url = BuildConfig.SE_TERMS_URL
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                try {
                    startActivity(i)
                } catch (e: Exception) {
                    //can't handle browser urls? suppress crash.
                    //todo - webview
                }
            })
        )

        val spannable = SpannableString(txtTerms.text)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(activity as Context, R.color.blue_text)),
            declarationString.indexOf(termsString),
            declarationString.indexOf(termsString) + termsString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        txtTerms.text = spannable
        txtNotApplicable.text = getString(
            R.string.combine_not_applicable_string,
            getString(R.string.not_applicable_if_you),
            getString(R.string.not_applicable_frontline)
        )

        if (barcodeReader?.isBarCodeDetectorOperational() == true) {
            barcodeReader?.let {
                barcodeReader!!.setListener(this)
            }
        } else {
            //initialize MLKit
            mlKitScanner = activity?.let { MLKitScanner(it, mlKitCameraPreview) }
            setAutoFitSurfaceViewListener(view)
            mlKitScanner?.setListener(this)
        }
        updateScanView(true)

        if (isTutorialCompleted) {
            hideShowScanner()
        }

        mAllowButton.setOnClickListener {
            if (barcodeReader?.isBarCodeDetectorOperational() == true)
                barcodeReader?.checkCameraPermission()
            else {
                mlKitScanner?.checkCameraPermission()
            }
        }
    }

    fun updateScanView(allowQRScanning: Boolean) {
        if (this::surfaceView.isInitialized && this::mlKitCameraPreview.isInitialized) {
            when {
                !allowQRScanning -> {
                    surfaceView.visibility = View.GONE
                    mlKitCameraPreview.visibility = View.GONE
                }
                barcodeReader?.isBarCodeDetectorOperational() == true -> {
                    surfaceView.visibility = View.VISIBLE
                    mlKitCameraPreview.visibility = View.GONE
                }
                else -> {
                    surfaceView.visibility = View.GONE
                    mlKitCameraPreview.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setAutoFitSurfaceViewListener(view: View) {
        mlKitCameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
                mlKitScanner?.setCameraPreviewSize()
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {

            }

            override fun surfaceCreated(p0: SurfaceHolder) {
                view.post {
                    mlKitScanner?.setCameraPreviewSize()
                    mlKitScanner?.startCameraPreview()
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()
        if(Preference.shouldBePaused(activity as Context)){
            onPause()
        }
        else{
            if (isTutorialCompleted) {
                if (barcodeReader?.isBarCodeDetectorOperational() == false) {
                    mlKitScanner?.resumeQRCodeScanning()
                } else {
                    barcodeReader?.resumeScanning()
                }
                hideShowScanner()
            }
        }
    }

    fun checkForAppPaused(){
        if(activity != null){
            if(Preference.shouldBePaused(activity as Context))
                onPause()
            else
                onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mlKitScanner?.stopCameraThread()
    }

    override fun onStop() {
        super.onStop()
        mlKitScanner?.closeCamera()
    }

    override fun onPause() {
        super.onPause()
        if (barcodeReader?.isBarCodeDetectorOperational() == true) {
            barcodeReader?.pauseScanning()
        } else {

            mlKitScanner?.pauseQRCodeScanning()
        }
    }

    private fun hideShowScanner() {
        if (isVisible)
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                scannerRootView.visibility = View.VISIBLE
                permissionRoot.visibility = View.GONE
                scannerRootView.requestFocus()
                if (barcodeReader?.isBarCodeDetectorOperational() == true) {
                    barcodeReader?.checkCameraPermission()
                } else {
                    mlKitScanner?.checkCameraPermission()
                }

            } else {
                AnalyticsUtils().screenAnalytics(activity as Activity, "SENoCameraPermission")
                permissionRoot.visibility = View.VISIBLE
                scannerRootView.visibility = View.GONE
            }
    }


    override fun onScanned(barcode: Barcode?) {
        barcodeReader?.pauseScanning()
        var pattern1 = BuildConfig.SE_VENUE_URL1.toRegex()
        var pattern2 = BuildConfig.SE_VENUE_URL2.toRegex()
        barcodeReader?.vibrate()
        barcode?.displayValue?.let {
            if (it.contains("http")) {
                requireActivity().runOnUiThread(Runnable {
                    validateQR(barcode.displayValue)
                })

            } else {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "QR code doesn't start with http: $it",
                    null
                )
                CentralLog.e(loggerTAG, "QR code doesn't start with http: $it")
                requireActivity().runOnUiThread(Runnable {
                    invalidQrCode()
                })

            }
        }
    }

    fun validateQR(url: String) {
        CentralLog.d(TAG, "Alv - Validating QR Code")
        observerResponse(url)
        errorHandler.handleSENetworkConnection {
            CentralLog.d(TAG, "Alv - Validating QR Code - handler")
            if (it) {
                (activity as SafeEntryActivity?)?.setLoadingEnable(true)
                vm.validateQrCode(url)
            } else {
                //No internet connectivity and user might press cancel, exit page
                activity?.finish()
            }
        }
    }

    override fun onScanError(errorMessage: String?) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        DBLogger.e(
            DBLogger.LogType.SAFEENTRY,
            loggerTAG,
            "onScanError: $errorMessage",
            null
        )
        CentralLog.e(loggerTAG, "onScanError: $errorMessage")
        // barcodeReader??.vibrate()
    }


    override fun onCameraPermissionDenied() {
        hideShowScanner()
    }

    override fun onCameraPermissionAllowed() {
        hideShowScanner()
    }

    private fun observerResponse(url: String) {
        if (vm.responseData.hasActiveObservers())
            vm.clearResponseLiveData()

        vm.responseData.observe(viewLifecycleOwner, androidx.lifecycle.Observer { response ->
            if (response.isSuccess) {
                val venue = response.result as SeVenueList
                (activity as SafeEntryActivity?)?.setLoadingEnable(false)
                if (venue.data.isNotEmpty()) {
                    goToNextScreen(venue.data as ArrayList<QrResultDataModel>)
                } else {
                    val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(loggerTAG, "QR code returns empty list: $url")
                    DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "QR code returns empty list: $url")
                    qrCodeUnavailable()
                }
            } else {
                var errMsg = response.result as String
                if (!errMsg.isNullOrEmpty() && (errMsg.contains(
                        "INTERNAL",
                        true
                    ) || errMsg.contains(
                        "DEADLINE_EXCEEDED",
                        true
                    ))
                ) {
                    errorHandler.showError()
                } else {
                    invalidQrCode()
                }
                (activity as SafeEntryActivity?)?.setLoadingEnable(false)
            }

        })
    }

    private fun goToNextScreen(venueList: ArrayList<QrResultDataModel>) {
        if (!venueList.isNullOrEmpty()) {
//            AndroidBus.behaviorSubject.onNext(venueList)
            val intent = Intent(activity, SafeEntryCheckInOutActivityV2::class.java)
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_FRAGMENT_VALUE,
                SafeEntryCheckInOutActivityV2.SE_CHECK_IN_VALUE
            )

            intent.putExtra(
                    SafeEntryCheckInOutActivityV2.SE_VENUE_LIST,
                    venueList
            )

            intent.putExtra("is_check_in", true)
            intent.putExtra(
                SafeEntryActivity.IS_FROM_GROUP_CHECK_IN,
                (activity as SafeEntryActivity).isFromGroupCheckIn
            )
            activity?.startActivityForResult(intent, SafeEntryActivity.REQUEST_ACTION)
            // activity?.finish()
        } else {
            invalidQrCode()
        }
    }

    private fun invalidQrCode() {
        if (activity?.isFinishing == false) {
            dialog.show(mContext, AlertType.NON_SE_OR) {
                if (it) {
                    if (barcodeReader?.isBarCodeDetectorOperational() == true) {
                        barcodeReader?.resumeScanning()
                    } else {
                        mlKitScanner?.resumeQRCodeScanning()
                    }
                } else
                    activity?.finish()
            }
        }
    }

    private fun qrCodeUnavailable() {
        if (activity?.isFinishing == false) {
            dialog.show(mContext, AlertType.SE_NOT_AVAILABLE) {
                if (it) {
                    if (barcodeReader?.isBarCodeDetectorOperational() == true) {
                        barcodeReader?.resumeScanning()
                    } else {
                        mlKitScanner?.resumeQRCodeScanning()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CameraViewHelper.PERMISSION_CALLBACK_CONSTANT) {
            barcodeReader?.permissionCallback(requestCode, permissions, grantResults)
        }
    }

    fun onPageChanged(pos: Int) {
        if (pos == 0)
            onResume()
        else onPause()
    }

    override fun onScanned(barcodeList: List<com.google.mlkit.vision.barcode.Barcode>) {
        barcodeReader?.vibrate()
        barcodeList[0].rawValue?.let {
            if (it.contains("http")) {
                requireActivity().runOnUiThread(Runnable {
                    validateQR(it)
                })

            } else {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "QR code doesn't start with http: $it",
                    null
                )
                CentralLog.e(TAG, "QR code doesn't start with http: $it")
                requireActivity().runOnUiThread(Runnable {
                    invalidQrCode()
                })
            }
        }
    }
}
