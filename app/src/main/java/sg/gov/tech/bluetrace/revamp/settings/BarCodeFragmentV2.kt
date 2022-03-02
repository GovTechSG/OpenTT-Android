package sg.gov.tech.bluetrace.revamp.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData

class BarCodeFragmentV2 : Fragment() {
    private lateinit var idNumberTV: AppCompatTextView
    private lateinit var idNumberView: AppCompatImageView
    private lateinit var imageView: AppCompatImageView

    private val vm: BarCodeViewModel by viewModel()

    private var userType: Int = 0

    private var userID: String = "" //User original ID
    private var qrInput: String = "" //ID for generating barcode

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bar_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        imageView = view.findViewById(R.id.barcode_iv)
        idNumberTV = view.findViewById(R.id.id_number)
        idNumberView = view.findViewById(R.id.view_cardno_iv)

        userID = vm.getUserID()

        userType = if(RegisterUserData.isValidPassportUser(Preference.getUserIdentityType(view.context)))
            BarCodeViewModel.PASSPORT_USER
        else
            BarCodeViewModel.OTHER_USER

        generateBarcode()
    }

    private fun generateBarcode() {
        if(userType == BarCodeViewModel.OTHER_USER)
            qrInput = userID
        else if (userType == BarCodeViewModel.PASSPORT_USER)
            qrInput = vm.getPassportNumberWithCheckSum(userID)

        idNumberTV.text = Utils.maskIdWithDot(userID)

        idNumberView.setOnClickListener {
            populateID()
        }

        vm.getBarcodeBitmap(qrInput) {
            imageView.setImageBitmap(it)
        }
    }

    private fun populateID() {
        vm.isIDMasked {
            if (it) {
                idNumberTV.text = userID
                idNumberView.setImageResource(R.drawable.hide)
            } else {
                idNumberTV.text = HtmlCompat.fromHtml(Utils.maskIdWithDot(userID), HtmlCompat.FROM_HTML_MODE_LEGACY)
                idNumberView.setImageResource(R.drawable.eye)
            }
        }
    }
}
