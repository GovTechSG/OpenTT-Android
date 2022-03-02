package sg.gov.tech.bluetrace.revamp.safeentry

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.revamp.responseModel.CheckOutResponseModel
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

class SafeEntryCheckOutFragment : Fragment() {
    private val TAG: String = "SafeEntryCheckOutFragment"

    private val args: SafeEntryCheckOutFragmentArgs by navArgs()
    private val safeEntryCheckOutVM: SafeEntryCheckOutViewModel by viewModel()
    private val disposables = CompositeDisposable()
    private val alertBuilder: TTAlertBuilder by inject()
    private var familyMembersIDList: ArrayList<String> = ArrayList()
    private var placeName: String = ""

    //Layout & Views
    private lateinit var clSELayout: ConstraintLayout
    private lateinit var llSEHeaderLogo: LinearLayout
    private lateinit var clSECardHeader: ConstraintLayout
    private lateinit var clProgressLayout: ConstraintLayout

    private lateinit var tvMemberCount: AppCompatTextView
    private lateinit var ivCheckOut: AppCompatImageView
    private lateinit var ivCheckIn: AppCompatImageView
    private lateinit var tvDateTop: AppCompatTextView
    private lateinit var tvDateBottom: AppCompatTextView
    private lateinit var tvPlaceName: AppCompatTextView
    private lateinit var ivAddToFavImg: AppCompatImageView
    private lateinit var tvAddToFavText: AppCompatTextView
    private lateinit var btnBackToHome: AppCompatButton
    private lateinit var btnCheckOut: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_entry_check_in_out_new, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, "SECheckOutPass")
        initViewsAndListener(view)
        setUpCheckOutTheme()
        setFonts()
        setFavorite()
        callCheckOut()
    }

    private fun initViewsAndListener(view: View) {
        clSELayout = view.findViewById(R.id.safeentrycheckinout_layout)
        llSEHeaderLogo = view.findViewById(R.id.safeEntryHeaderLogo)
        clSECardHeader = view.findViewById(R.id.safeEntryCardHeader)
        clProgressLayout = view.findViewById(R.id.safe_entry_check_in_out_progress)

        tvMemberCount = view.findViewById(R.id.member_count_text)
        ivCheckOut = view.findViewById(R.id.safeEntryCheckOutIv)
        ivCheckIn = view.findViewById(R.id.safeEntryCheckInIv)
        tvDateTop = view.findViewById(R.id.safe_entry_check_in_out_date_top)
        tvDateBottom = view.findViewById(R.id.safe_entry_check_in_out_date_bottom)
        tvPlaceName = view.findViewById(R.id.safe_entry_check_in_out_place_name)
        ivAddToFavImg = view.findViewById(R.id.img_add_to_fav)
        tvAddToFavText = view.findViewById(R.id.tv_add_to_fav)
        btnBackToHome = view.findViewById(R.id.safe_entry_check_in_out_btn_next)
        btnCheckOut = view.findViewById(R.id.safe_entry_check_in_out_co_btn)

        btnBackToHome.setOnClickListener {
            (activity as SafeEntryCheckInOutActivityV2).goToHome()
        }
    }

    private fun setUpCheckOutTheme() {
        llSEHeaderLogo.setBackgroundResource(R.drawable.ic_checkout_rectangle)
        clSECardHeader.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.secondary_blue_6
            )
        )
        ivCheckOut.visibility = View.VISIBLE
        ivCheckIn.visibility = View.INVISIBLE
        placeName = if (args.venue.tenantName.isNullOrEmpty()) args.venue.venueName
            ?: "" else args.venue.tenantName ?: ""
        tvPlaceName.text = placeName

        if (args.venue.groupMembersCount == 0)
            tvMemberCount.text = 1.toString()
        else
            tvMemberCount.text = args.venue.groupMembersCount.toString()

    }

    private fun setFonts() {
        try {
            val semiBoldTypeface =
                ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
            val mediumTypeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
            tvMemberCount.typeface = semiBoldTypeface
            tvDateTop.typeface = semiBoldTypeface
            tvDateBottom.typeface = semiBoldTypeface
            tvPlaceName.typeface = mediumTypeface
            tvAddToFavText.typeface = mediumTypeface
        } catch (e: Exception) {
            CentralLog.e("SafeEntryCheckInOutFrag", "Error loading font: $e")
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                "Error loading font: $e",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    private fun setDate(date: String) {
        try {
            val splittedDate = Utils.getSafeEntryCheckInOutDate(date)
            tvDateTop.text = splittedDate[0]
            tvDateBottom.text = splittedDate[1]
        } catch (e: Exception) {
            CentralLog.e("SE_CHECK_INOUT", e.message.toString())
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                e.message.toString(),
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    private fun checkOutRecordSuccess(
        timeStamp: String
    ) {
        //  tvMemberCount.text = (familyMembersList.size + 1).toString()
        setDate(timeStamp)
        safeEntryCheckOutVM.updateSeRecordInDB(
            timeStamp, args.venue
        ) { isRecordRemoved ->
            if (isRecordRemoved) {
                btnBackToHome.isEnabled = true
                clProgressLayout.visibility = View.GONE
                btnBackToHome.visibility = View.VISIBLE
                btnCheckOut.visibility = View.GONE
            }
        }
    }

    private fun checkOutRecordFail() {
        alertBuilder.show(
            activity as SafeEntryCheckInOutActivityV2,
            AlertType.CHECK_OUT_NETWORK_ERROR_DIALOG
        ) {
            if (it) {
                btnCheckOut.visibility = View.VISIBLE
                callCheckOut()
            } else {
                activity?.finish()
            }
        }
    }

    private fun callCheckOut() {
        clProgressLayout.visibility = View.VISIBLE
        if (safeEntryCheckOutVM.checkOutResponseData.hasActiveObservers())
            safeEntryCheckOutVM.clearCheckOutResponseLiveData()
        val user = Preference.getEncryptedUserData(TracerApp.AppContext)
        var groupIds: ArrayList<String> = ArrayList()
        var ids =
            args.venue.groupMembers?.let { safeEntryCheckOutVM.getDecryptedGroupIds(requireContext(), it) }
        if (!ids.isNullOrEmpty())
            groupIds.addAll(ids)
        if (user != null) {
            safeEntryCheckOutVM.checkOutResponseData.observe(viewLifecycleOwner, Observer { response ->
                CentralLog.d(TAG, "Api successfully: $response")
                val result = response.result
                if (response.isSuccess) {
                    if (result is CheckOutResponseModel) {
                        CentralLog.d(TAG, "PostSEEntryData Success")
                        result.timeStamp?.let { checkOutRecordSuccess(it) }
                    }
                } else {
                    checkOutRecordFail()
                }
            })
            safeEntryCheckOutVM.postSEEntryCheckOut(
                user,
                groupIds,
                args.venue
            )
        }
    }

    private fun setFavorite() {

        safeEntryCheckOutVM.isFav.observe(viewLifecycleOwner, Observer {
            favUIChange(it)
        })

        safeEntryCheckOutVM.isVenueFavorite(args.venue)
        ivAddToFavImg.setOnClickListener {
            onFavClicked()
        }
        tvAddToFavText.setOnClickListener {
            if (safeEntryCheckOutVM.isFav.value == false) {
                onFavClicked()
            }
        }
    }

    private fun onFavClicked() {
        if (safeEntryCheckOutVM.isFav.value == true) {
            safeEntryCheckOutVM.deleteFavourite(args.venue) { isDeleted ->
                if (isDeleted) {
                    //display snackbar
                    showFavSnackBar(
                        requireContext(),
                        clSELayout,
                        R.string.removed_from_favourites
                    )
                }
            }
        } else {
            safeEntryCheckOutVM.insertFavourite(args.venue) { isInserted ->
                if (isInserted) {
                    showFavSnackBar(
                        requireContext(),
                        clSELayout,
                        R.string.saved_to_favourites
                    )
                }
            }
        }
    }

    private fun showFavSnackBar(context: Context, view: View, text: Int) {
        val snackBar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
        snackBar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.green_bg))
        val snackBarTextView =
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackBarTextView.setTypeface(snackBarTextView.typeface, Typeface.BOLD)
        snackBar.setTextColor(ContextCompat.getColor(context, R.color.grey_1))
        val snackBarActionView =
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        snackBarActionView.setTypeface(snackBarActionView.typeface, Typeface.BOLD)
        snackBar.setActionTextColor(ContextCompat.getColor(context, R.color.normal_text))
        snackBar.setAction(R.string.close_uppercase) {
            // Responds to click
            snackBar.dismiss()
        }.show()
    }

    private fun favUIChange(isFavAdded: Boolean) {
        ivAddToFavImg.setImageBitmap(null) //To clear the imageView before putting the background resource
        if (isFavAdded) {
            ivAddToFavImg.setBackgroundResource(R.drawable.ic_add_fav_checkin_added)
            tvAddToFavText.text = getString(R.string.check_in_fav_added)
        } else {
            ivAddToFavImg.setBackgroundResource(R.drawable.ic_add_fav_checkin)
            tvAddToFavText.text = getText(R.string.add_this_location_to_favourites)
        }
    }


}
