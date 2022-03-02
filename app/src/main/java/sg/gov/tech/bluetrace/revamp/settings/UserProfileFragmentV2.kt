package sg.gov.tech.bluetrace.revamp.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.lifecycle.Observer
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.settings.BarcodeHeaderView
import sg.gov.tech.bluetrace.settings.OnBarcodeClick
import sg.gov.tech.bluetrace.utils.VMState


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UserProfileFragmentV2.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserProfileFragmentV2 : MainActivityFragment("UserProfileFragment") {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var nameTV: AppCompatTextView
    private lateinit var mobileNumberTV: AppCompatTextView
    private lateinit var idTV: AppCompatTextView
    private lateinit var birthDateTV: AppCompatTextView
    private lateinit var birthDateHeader: AppCompatTextView
    private lateinit var birthIssueTV: AppCompatTextView
    private lateinit var nricTitle: AppCompatTextView
    private lateinit var cardSerialNumber: AppCompatTextView
    private lateinit var idType: IdentityType
    private lateinit var mContext: Context
    private lateinit var llDateOfIssue: LinearLayout
    private lateinit var loadingView: View

    private lateinit var llCardSerialNumber: LinearLayout
    private val vm: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var root = inflater.inflate(R.layout.fragment_user_profle, container, false)
        return root
    }

    override fun didProcessBack(): Boolean {
        return false
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_PROFILE_PAGE
        )
        mContext = view.context
        initView(view)

    }

    fun initView(view: View) {
        var header: BarcodeHeaderView = view.findViewById(R.id.barcode_header)
        idTV = view.findViewById(R.id.tv_nric_fin)
        nameTV = view.findViewById(R.id.tv_name)
        mobileNumberTV = view.findViewById(R.id.tv_mobile_no)
        birthDateTV = view.findViewById(R.id.tv_date_of_birth)
        birthDateHeader = view.findViewById(R.id.tv_date_of_birth_header)
        birthIssueTV = view.findViewById(R.id.tv_date_of_issue)
        nricTitle = view.findViewById(R.id.tv_nric_title)
        llDateOfIssue = view.findViewById(R.id.ll_date_of_issue)
        llCardSerialNumber = view.findViewById(R.id.ll_serial_number)
        cardSerialNumber = view.findViewById(R.id.tv_card_number)
        loadingView = view.findViewById(R.id.view_loading)
        view.context?.getString(R.string.your_profile)?.let { header.setTitle(it) }
        mobileNumberTV.text = Preference.getEncryptedPhoneNumber(mContext)
        header.showBackNavigationImage()
        header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(
                    SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,
                    SafeEntryActivity.ID_FRAGMENT
                )
                startActivity(intent)
            }

            override fun onBackPress() {
                (activity as MainActivity).onBackPressed()
            }
        })
        fetchUserData()
    }

    private fun fetchUserData() {
        setLoading(true)
        vm.getUserData()
        vm.userData.observe(viewLifecycleOwner, Observer { result ->
            when (result.state) {
                VMState.Loading -> {
                    setLoading(true)
                }
                VMState.Done -> {
                    bindUserData(result.data as RegisterUserData?)
                    setLoading(false)
                }
                VMState.Error -> {
                    setLoading(false)
                }

                else -> {

                }
            }
        })
    }


    private fun bindUserData(user: RegisterUserData?) {
        if (user != null) {
            idType = IdentityType.findByValue(user.idType)
            idTV.text = Utils.maskIdWithCross(user.id)
            nameTV.text = user.name
            birthDateTV.text = user.dateOfBirth
            birthIssueTV.text = user.idDateOfIssue
            when (idType) {
                IdentityType.NRIC ->
                    nricTitle.text = resources.getString(R.string.nric)
                IdentityType.FIN_DP, IdentityType.FIN_WP, IdentityType.FIN_LTVP, IdentityType.FIN_STP ->
                    nricTitle.text = resources.getString(R.string.fin)
                IdentityType.PASSPORT, IdentityType.PASSPORT_VERIFIED ->
                    nricTitle.text = resources.getString(R.string.passport_number)
            }
        }
    }

    private fun setLoading(show: Boolean) {
        if (show) {
            loadingView.visibility = View.VISIBLE
        } else {
            loadingView.visibility = View.INVISIBLE
        }
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UserProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UserProfileFragmentV2().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
