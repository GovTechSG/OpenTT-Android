package sg.gov.tech.bluetrace.revamp.safeentry

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import kotlinx.android.synthetic.main.group_check_in_error_dialog.view.*
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.revamp.responseModel.CheckInResponseModel
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager

class SafeEntryCheckInFragmentV2 : Fragment() {
    private val viewModel: SafeEntryCheckInViewModel by viewModel()
    private val args: SafeEntryCheckInFragmentV2Args by navArgs()
    private lateinit var rootView: ConstraintLayout
    private lateinit var llSafeEntryHeaderLogo: LinearLayout
    private lateinit var clSafeEntryHeader: ConstraintLayout
    private lateinit var ivSafeEntryCheckInIv: AppCompatImageView
    private lateinit var ivSafeEntryCheckOutIv: AppCompatImageView
    private lateinit var btGoToHome: AppCompatButton
    private lateinit var clSafeEntryCheckInProgress: ConstraintLayout
    private lateinit var tvSafeEntryPlaceName: AppCompatTextView
    private lateinit var tvSafeEntryCheckInDateTop: AppCompatTextView
    private lateinit var tvSafeEntryCheckInDateBottom: AppCompatTextView
    private lateinit var tvMemberCount: AppCompatTextView
    private lateinit var tvAddToFav: AppCompatTextView
    private lateinit var ivAddToFav: ImageView
    private lateinit var llAddToFav: LinearLayout
    private lateinit var familyMembersList: List<FamilyMembersRecord>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, "SECheckInPass")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_entry_check_in_out_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        familyMembersList = (activity as SafeEntryCheckInOutActivityV2).familyMembersList
        setViews(view)
        setFonts()
        setObservers()
        setFavorite()
        callCheckIn()
        btGoToHome.setOnClickListener {
            (activity as SafeEntryCheckInOutActivityV2).goToHome()
        }
    }

    private fun setViews(view: View) {
        rootView = view.findViewById(R.id.safeentrycheckinout_layout)
        llSafeEntryHeaderLogo = view.findViewById(R.id.safeEntryHeaderLogo)
        clSafeEntryHeader = view.findViewById(R.id.safeEntryCardHeader)
        ivSafeEntryCheckInIv = view.findViewById(R.id.safeEntryCheckInIv)
        ivSafeEntryCheckOutIv = view.findViewById(R.id.safeEntryCheckOutIv)
        btGoToHome = view.findViewById(R.id.safe_entry_check_in_out_btn_next)
        clSafeEntryCheckInProgress = view.findViewById(R.id.safe_entry_check_in_out_progress)
        tvSafeEntryPlaceName = view.findViewById(R.id.safe_entry_check_in_out_place_name)
        tvSafeEntryCheckInDateTop = view.findViewById(R.id.safe_entry_check_in_out_date_top)
        tvSafeEntryCheckInDateBottom = view.findViewById(R.id.safe_entry_check_in_out_date_bottom)
        tvMemberCount = view.findViewById(R.id.member_count_text)
        tvAddToFav = view.findViewById(R.id.tv_add_to_fav)
        ivAddToFav = view.findViewById(R.id.img_add_to_fav)
        llAddToFav = view.findViewById(R.id.ll_add_to_fav)

        llSafeEntryHeaderLogo.setBackgroundResource(R.drawable.ic_checkin_rectangle)
        clSafeEntryHeader.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green_checkin
            )
        )
        ivSafeEntryCheckInIv.visibility = View.VISIBLE
        ivSafeEntryCheckOutIv.visibility = View.INVISIBLE
        btGoToHome.isEnabled = true
        clSafeEntryCheckInProgress.visibility = View.GONE
        tvSafeEntryPlaceName.text =
            if (args.venue.tenantName.isNullOrEmpty()) args.venue.venueName else args.venue.tenantName
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
            tvSafeEntryCheckInDateTop.typeface = semiBoldTypeface
            tvSafeEntryCheckInDateBottom.typeface = semiBoldTypeface
            tvSafeEntryPlaceName.typeface = mediumTypeface
            tvAddToFav.typeface = mediumTypeface
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "Error loading font: $e")
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                "Error loading font: $e",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    private fun setObservers() {
        viewModel.isFav.observe(viewLifecycleOwner, Observer {
            favUIChange(it)
        })

        viewModel.checkInApiResponse.observe(viewLifecycleOwner, Observer { response ->
            val result = response.result
            if (response.isSuccess) {
                if (result is CheckInResponseModel) {
                    result.timeStamp?.let { checkInSuccessful(result, it) }
                }
            } else {
                checkInFail()
            }
        })
    }

    private fun setFavorite() {
        viewModel.isVenueFavorite(args.venue)
        ivAddToFav.setOnClickListener {
            onFavClicked()
        }
        llAddToFav.setOnClickListener {
            if (viewModel.isFav.value == false) {
                onFavClicked()
            }
        }
    }

    private fun onFavClicked() {
        if (viewModel.isFav.value == true) {
            viewModel.deleteFavourite(args.venue) { isDeleted ->
                if (isDeleted) {
                    //display snackbar
                    showFavSnackBar(
                        requireContext(),
                        rootView,
                        R.string.removed_from_favourites
                    )
                }
            }
        } else {
            viewModel.insertFavourite(args.venue) { isInserted ->
                if (isInserted) {
                    showFavSnackBar(
                        requireContext(),
                        rootView,
                        R.string.saved_to_favourites
                    )
                }
            }
        }
    }

    private fun callCheckIn() {
        //create requestModel
        clSafeEntryCheckInProgress.visibility = View.VISIBLE
        val user = Preference.getEncryptedUserData(TracerApp.AppContext)
        val groupIds =
            familyMembersList.map {
                TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC(requireContext(), it.nric)
            } as ArrayList<String>
        if (user != null) {
            viewModel.callUserCheckIn(
                user,
                groupIds,
                args.venue
            )
        }
    }

    private fun checkInSuccessful(
        result: CheckInResponseModel,
        timeStamp: String
    ) {
        tvMemberCount.text = (familyMembersList.size + 1).toString()
        result.timeStamp?.let { it ->
            Utils.getSafeEntryCheckInOutDate(it)
            setDateCheckIn(it)
            viewModel.insertSeRecordToDB(
                timeStamp, args.venue,
                familyMembersList
            ) { isRecordInserted ->
                if (isRecordInserted) {
                    clSafeEntryCheckInProgress.visibility = View.GONE

                }
            }
        }
    }

    private fun checkInFail() {
        val alertType = AlertType.CHECK_IN_NETWORK_ERROR_DIALOG
        TTAlertBuilder().show(activity as SafeEntryCheckInOutActivityV2, alertType) {
            if (it) {
                callCheckIn()
            } else {
                activity?.finish()
            }
        }
    }

    private fun setDateCheckIn(date: String) {
        try {
            var splittedDate = Utils.getSafeEntryCheckInOutDate(date)
            tvSafeEntryCheckInDateTop.text = splittedDate[0]
            tvSafeEntryCheckInDateBottom.text = splittedDate[1]
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                e.message.toString(),
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(loggerTAG, e.message.toString())
        }
    }


    private fun favUIChange(isFavAdded: Boolean) {
        ivAddToFav.setImageBitmap(null) //To clear the imageView before putting the background resource
        if (isFavAdded) {
            ivAddToFav.setBackgroundResource(R.drawable.ic_add_fav_checkin_added)
            tvAddToFav.text = getString(R.string.check_in_fav_added)
        } else {
            ivAddToFav.setBackgroundResource(R.drawable.ic_add_fav_checkin)
            tvAddToFav.text = getText(R.string.add_this_location_to_favourites)
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
}