package sg.gov.tech.bluetrace.revamp.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.Disposable
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddFamilyMembersActivity
import sg.gov.tech.bluetrace.groupCheckIn.manageFamily.ManageFamilyMembersFragment
import sg.gov.tech.bluetrace.settings.BarcodeHeaderView
import sg.gov.tech.bluetrace.settings.OnBarcodeClick
import sg.gov.tech.bluetrace.settings.SettingMenuAdapter
import sg.gov.tech.bluetrace.settings.SubmitLogsFragment
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragmentV2.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragmentV2 : MainActivityFragment("SettingsFragmentV2") {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: SettingMenuAdapter
    private lateinit var header: BarcodeHeaderView
    private lateinit var mContext: Context
    private lateinit var contentFrame: FrameLayout
    private val vm: SettingsViewModel by viewModel()

    //The text for each of the setting
    private var yourProfile: String? = null
    private var manageFamilyMember: String? = null
    private var changeLanguage: String? = null
    private var help: String? = null
    private var reportVul: String? = null
    private var submitErrorLogs: String? = null
    private var privacySafeguards: String? = null
    private var termsOfUse: String? = null
    private var acknowledge: String? = null

    private var disposable: Disposable? = null

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
        // Inflate the manu_list_item_layout for this fragment
//        return inflater.inflate(R.manu_list_item_layout.fragment_setting_menu, container, false)
        contentFrame = FrameLayout(requireContext())
        return inflateContent(contentFrame)
    }

    private fun inflateContent(root: ViewGroup): View {
        yourProfile = resources.getString(R.string.your_profile)
        manageFamilyMember = resources.getString(R.string.manage_family_members)
        changeLanguage = resources.getString(R.string.change_language)
        help = resources.getString(R.string.help)
        reportVul = resources.getString(R.string.report_vulnerability)
        submitErrorLogs = resources.getString(R.string.submit_error_logs)
        privacySafeguards = resources.getString(R.string.privacy_safeguards)
        termsOfUse = resources.getString(R.string.terms_of_Use)
        acknowledge = resources.getString(R.string.acknowledge)

        return LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_setting_menu, root, false)
    }

    override fun onResume() {
        super.onResume()
        mAdapter.resetItemClickFlag()
        (activity as MainActivity).isSettings = true
    }


    /**
     * Initialize the contents of the Fragment host's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called [.setHasOptionsMenu].  See
     * [Activity.onCreateOptionsMenu]
     * for more information.
     *
     * @param menu The options menu in which you place your items.
     *
     * @see .setHasOptionsMenu
     *
     * @see .onPrepareOptionsMenu
     *
     * @see .onOptionsItemSelected
     */


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingMenuFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = SettingsFragmentV2()

    }

    override fun didProcessBack(): Boolean {
        mAdapter.resetItemClickFlag()
        if (isAdded) {
            return if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
                true
            } else
                false
        }
        return false
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_SETTINGS_PAGE)
        createView(view)
    }

    private fun createView(view: View) {
        mContext = view.context
        view.findViewById<AppCompatTextView>(R.id.appCompatTextView3)
            ?.setText(R.string.setting_hello)

        header = view.findViewById(R.id.barcode_header)
        header.setTitle(mContext.getString(R.string.title_more))
        mAdapter = SettingMenuAdapter(
            mContext,
            this.childFragmentManager,
            vm.getVersionName(mContext),
            vm.getUserIdType()
        )
        initView()
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView2).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(view.context)
            adapter = mAdapter
        }
    }


    private fun initView() {
        header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,SafeEntryActivity.ID_FRAGMENT)
                startActivity(intent)
            }

            override fun onBackPress() {}
        })

        mAdapter.setCallBackListener(object : SettingMenuAdapter.FragmentCallBack {
            override fun onNextClicked(position: Int, sectionLabel: String) {
                (activity as MainActivity).isSettings = false
                when (sectionLabel) {
                    yourProfile -> {
                        val userProfileFragment = UserProfileFragmentV2.newInstance("", "")
                        childFragmentManager.beginTransaction()
                            .addToBackStack(userProfileFragment.customTag)
                            .replace(R.id.child_content, userProfileFragment)
                            .commit()
                    }
                    manageFamilyMember -> {
                        mAdapter.notifyItemChanged(position)
                        //Put method to link to Manage Family Members page
                        vm.navigateToManageFamily(mContext) {
                            if (it) openManageFamily()
                            else openAddFamilyMembers()
                        }
                    }
                    changeLanguage -> {
                        showPopup()
                        mAdapter.resetItemClickFlag()
                    }
                    help -> {
                        openWebView(BuildConfig.ZENDESK_URL,true)
                    }
                    reportVul -> {
                        openWebView(BuildConfig.REPORT_VULNERABILITY_URL,false)
                    }
                    submitErrorLogs -> {
                        if (Preference.isSubmitErrorLogsNew(mContext))
                            Preference.putIsSubmitErrorLogNew(mContext, false)
                        mAdapter.notifyItemChanged(position)
                        var fragment = SubmitLogsFragment()
                        childFragmentManager.beginTransaction()
                            .addToBackStack(fragment.customTag)
                            .replace(R.id.child_content, fragment)
                            .commit()
                    }
                    privacySafeguards ->{
                        openWebView(BuildConfig.PRIVACY_SAFEGUARDS_URL,false)
                    }
                    termsOfUse ->{
                        openWebView(BuildConfig.TERMS_OF_USE_URL,false)
                    }
                    acknowledge ->{
                        openWebView(BuildConfig.ACKNOWLEDGEMENT_URL,false)
                    }
                    else -> openWebView(BuildConfig.GDS_LOGO_URL ,false)
                }
            }
        })
    }

    fun showPopup() {
        vm.showPopup(mContext) {
            vm.setLocale(mContext, it)
            contentFrame.removeAllViews()
            inflateContent(contentFrame)
            view?.let {
                createView(it)
            }
            mAdapter.notifyDataSetChanged()
            (activity as MainActivity).refreshBottomNav()
        }
    }


    private fun openWebView(url: String, isFabVisible: Boolean) {
        var fragment = WebViewZendeskSupportFragment()
        fragment.setUrl(url)
        if(!isFabVisible)
            fragment.setFabInvisible()
        childFragmentManager.beginTransaction()
            .addToBackStack(fragment.customTag)
            .replace(R.id.child_content, fragment)
            .commit()
    }


    private fun openManageFamily() {
        val manageFamilyMembersFragment = ManageFamilyMembersFragment()
        childFragmentManager.beginTransaction()
            .addToBackStack(manageFamilyMembersFragment.customTag)
            .replace(R.id.child_content, manageFamilyMembersFragment)
            .commit()
    }

    private fun openAddFamilyMembers() {
        val intent = Intent(mContext, AddFamilyMembersActivity::class.java)
        intent.putExtra(AddFamilyMembersActivity.ADD_FAMILY_MEMBERS, false)
        startActivityForResult(
            intent,
            AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            openManageFamily()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}
