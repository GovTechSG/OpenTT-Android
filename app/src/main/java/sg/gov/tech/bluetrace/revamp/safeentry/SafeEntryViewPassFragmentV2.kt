package sg.gov.tech.bluetrace.revamp.safeentry

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.favourite.FavouriteViewModel
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger

class SafeEntryViewPassFragmentV2 : Fragment() {

    private val favouriteViewModel: FavouriteViewModel by viewModel()
    private val args: SafeEntryViewPassFragmentV2Args by navArgs()
    private var isFavAdded: Boolean = false

    private lateinit var safeEntryMainLayout: ConstraintLayout
    private lateinit var safeEntryProgressLayout: ConstraintLayout
    private lateinit var safeEntryCardHeader: ConstraintLayout
    private lateinit var safeEntryHeaderLogo: LinearLayout
    private lateinit var safeEntryCheckOutIv: AppCompatImageView
    private lateinit var safeEntryCheckInIv: AppCompatImageView
    private lateinit var safeEntryCheckOutButton: AppCompatButton
    private lateinit var safeEntryBackToHomeButton: AppCompatButton
    private lateinit var safeEntryPlaceNameTv: AppCompatTextView
    private lateinit var safeEntryMemberCountTv: AppCompatTextView
    private lateinit var safeEntryDateTop: AppCompatTextView
    private lateinit var safeEntryDateBottom: AppCompatTextView
    private lateinit var addToFavIv: AppCompatImageView
    private lateinit var addToFavTv: AppCompatTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safe_entry_check_in_out_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, "SEViewPass")
        initViews(view)
        setClickListeners()
        setFonts()
        setViewPassTheme()
        setObservers()
        getFavourite()
    }

    private fun initViews(view: View) {
        safeEntryMainLayout = view.findViewById(R.id.safeentrycheckinout_layout)
        safeEntryProgressLayout = view.findViewById(R.id.safe_entry_check_in_out_progress)
        safeEntryCardHeader = view.findViewById(R.id.safeEntryCardHeader)
        safeEntryHeaderLogo = view.findViewById(R.id.safeEntryHeaderLogo)
        safeEntryCheckOutIv = view.findViewById(R.id.safeEntryCheckOutIv)
        safeEntryCheckInIv = view.findViewById(R.id.safeEntryCheckInIv)
        safeEntryCheckOutButton = view.findViewById(R.id.safe_entry_check_in_out_co_btn)
        safeEntryBackToHomeButton = view.findViewById(R.id.safe_entry_check_in_out_btn_next)
        safeEntryPlaceNameTv = view.findViewById(R.id.safe_entry_check_in_out_place_name)
        safeEntryMemberCountTv = view.findViewById(R.id.member_count_text)
        safeEntryDateTop = view.findViewById(R.id.safe_entry_check_in_out_date_top)
        safeEntryDateBottom = view.findViewById(R.id.safe_entry_check_in_out_date_bottom)
        addToFavIv = view.findViewById(R.id.img_add_to_fav)
        addToFavTv = view.findViewById(R.id.tv_add_to_fav)
    }

    private fun setClickListeners() {
        safeEntryCheckOutButton.setOnClickListener {
            navigateToCheckOutScreen()
        }
        addToFavIv.setOnClickListener {
            onFavClicked()
        }
        addToFavTv.setOnClickListener {
            if (!isFavAdded) {
                onFavClicked()
            }
        }
    }

    private fun setFonts() {
        try {
            val semiBoldTypeface =
                ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
            val mediumTypeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
            safeEntryMemberCountTv.typeface = semiBoldTypeface
            safeEntryDateTop.typeface = semiBoldTypeface
            safeEntryDateBottom.typeface = semiBoldTypeface
            safeEntryPlaceNameTv.typeface = mediumTypeface
            addToFavTv.typeface = mediumTypeface
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

    private fun setViewPassTheme() {
        safeEntryCardHeader.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.green_checkin
            )
        )
        safeEntryHeaderLogo.setBackgroundResource(R.drawable.ic_checkin_rectangle)

        safeEntryCheckOutIv.visibility = View.VISIBLE
        safeEntryCheckInIv.visibility = View.GONE
        safeEntryCheckOutButton.visibility = View.VISIBLE
        safeEntryBackToHomeButton.visibility = View.GONE
        safeEntryProgressLayout.visibility = View.GONE

        safeEntryPlaceNameTv.text =
            if (args.venue.tenantName.isNullOrEmpty()) args.venue.venueName
                ?: "" else args.venue.tenantName ?: ""
        safeEntryMemberCountTv.text =
            if (args.venue.groupMembersCount == null || args.venue.groupMembersCount == 0) 1.toString() else args.venue.groupMembersCount.toString()
        args.venue.checkInTimeMS?.let {
            setDate(Utils.getSafeEntryCheckInOutDateFromMs(it))
        }
    }

    private fun setDate(splitDate: List<String>) {
        try {
            safeEntryDateTop.text = splitDate[0]
            safeEntryDateBottom.text = splitDate[1]
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, e.message.toString())
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                e.message.toString(),
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    private fun setObservers() {
        favouriteViewModel.isAdded.observe(viewLifecycleOwner, Observer { isAdded ->
            isFavAdded = isAdded
            favUIChange()
        })

        favouriteViewModel.isDeleted.observe(viewLifecycleOwner, Observer { isDeleted ->
            if (isDeleted) {
                favouriteViewModel.showSnackBar(
                    requireContext(),
                    safeEntryMainLayout,
                    R.string.removed_from_favourites
                )
                AnalyticsUtils().trackEvent(
                    AnalyticsKeys.SCREEN_NAME_CHECK_IN_CONFIRMATION,
                    AnalyticsKeys.SE_TAP_FAVOURITE,
                    AnalyticsKeys.FALSE
                )
            }
        })
    }

    private fun getFavourite() {
        favouriteViewModel.isFavAdded(requireContext(), args.venue)
    }

    private fun favUIChange() {
        addToFavIv.setImageBitmap(null) //To clear the imageView before putting the background resource
        if (isFavAdded) {
            addToFavIv.setBackgroundResource(R.drawable.ic_add_fav_checkin_added)
            addToFavTv.text = getString(R.string.check_in_fav_added)
        } else {
            addToFavIv.setBackgroundResource(R.drawable.ic_add_fav_checkin)
            addToFavTv.text = getText(R.string.add_this_location_to_favourites)
        }
    }

    private fun onFavClicked() {
        if (!isFavAdded)
            favouriteViewModel.insertFavRecord(requireContext(), args.venue, safeEntryMainLayout)
        else
            favouriteViewModel.deleteFavRecord(
                requireContext(),
                args.venue.venueId,
                args.venue.tenantId
            )

        isFavAdded = !isFavAdded
        favUIChange()
    }

    private fun navigateToCheckOutScreen() {
        val bundle = bundleOf(SafeEntryCheckInOutActivityV2.SE_VENUE to args.venue)
        findNavController().navigate(
            R.id.action_safeEntryViewPassFragment_to_safeEntryCheckOutFragment,
            bundle
        )
    }
}
