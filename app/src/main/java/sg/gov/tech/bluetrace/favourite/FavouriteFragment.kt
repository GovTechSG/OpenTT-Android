package sg.gov.tech.bluetrace.favourite

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.MainActivity.Companion.GO_TO_HISTORY
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.AndroidBus
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class FavouriteFragment : Fragment(), FavouriteListAdapter.Callback {

    private val viewModel: FavouriteViewModel by viewModel()
    private var clickedFavouriteRecord: FavouriteRecord? = null
    private val alertBuilder: TTAlertBuilder by inject()

    private lateinit var noFavouritesLayout: LinearLayout
    private lateinit var seeMyHistoryButton: AppCompatButton
    private lateinit var favouritesLayout: LinearLayout
    private lateinit var searchEt: AppCompatEditText
    private lateinit var termsDeclarationTv: AppCompatTextView
    private lateinit var declarationLayout: ConstraintLayout
    private lateinit var termsTv: AppCompatTextView
    private lateinit var notApplicableTv: AppCompatTextView
    private lateinit var yourFavPlacesLayout: ConstraintLayout
    private lateinit var rvFavourites: RecyclerView
    private lateinit var noResultsLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favourite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setDeclarationText()
        setObservers()
        setClickListener()
        getFavouriteRecords()
    }

    private fun initViews(view: View) {
        noFavouritesLayout = view.findViewById(R.id.no_favourites_layout)
        seeMyHistoryButton = view.findViewById(R.id.see_my_history_button)
        favouritesLayout = view.findViewById(R.id.favourites_layout)
        searchEt = view.findViewById(R.id.search_edit_text)
        termsDeclarationTv = view.findViewById(R.id.terms_declaration_text)
        declarationLayout = view.findViewById(R.id.declaration_layout)
        termsTv = view.findViewById(R.id.terms_text)
        notApplicableTv = view.findViewById(R.id.not_applicable_text)
        yourFavPlacesLayout = view.findViewById(R.id.your_favourite_places_layout)
        rvFavourites = view.findViewById(R.id.rv_favourites)
        noResultsLayout = view.findViewById(R.id.no_results_found_layout)
    }

    private fun setDeclarationText() {
        termsDeclarationTv.setOnClickListener {
            if (declarationLayout.visibility == View.GONE) {
                termsDeclarationTv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_up,
                    0
                )
                declarationLayout.visibility = View.VISIBLE
            } else {
                termsDeclarationTv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_down,
                    0
                )
                declarationLayout.visibility = View.GONE
            }
        }

        val declarationString = getString(
            R.string.combined_terms_string,
            getString(R.string.you_have_no_close_contact),
            getString(R.string.you_have_no_quarantine),
            getString(R.string.no_fever),
            getString(R.string.agree_to_the_terms_se)
        )
        val termsString = resources.getString(R.string.terms)

        termsTv.text = declarationString
        termsTv.makeLinks(
            Pair(
                termsString,
                View.OnClickListener { viewModel.getTermsFavLink(requireContext()) })
        )
        termsTv.text = viewModel.getTermsFavText(
            requireContext(),
            declarationString,
            termsString,
            termsTv.text
        )
        notApplicableTv.text = getString(
            R.string.combine_not_applicable_string,
            getString(R.string.not_applicable_if_you),
            getString(R.string.not_applicable_frontline)
        )
    }

    private fun setObservers() {
        viewModel.favouriteRecordList.observe(viewLifecycleOwner, Observer { records ->
            if (records.isNotEmpty()) {
                noFavouritesLayout.visibility = View.GONE
                seeMyHistoryButton.visibility = View.GONE
                favouritesLayout.visibility = View.VISIBLE
                setAdapter(records.sortedWith(SortFavouriteListAlphabetically))
            } else {
                noFavouritesLayout.visibility = View.VISIBLE
                seeMyHistoryButton.visibility = View.VISIBLE
                favouritesLayout.visibility = View.GONE
            }
        })

        viewModel.isDeleted.observe(viewLifecycleOwner, Observer { isDeleted ->
            if (isDeleted) {
                viewModel.showSnackBar(
                    requireContext(),
                    favouritesLayout,
                    R.string.removed_from_favourites
                )
                AnalyticsUtils().trackEvent(
                    AnalyticsKeys.SCREEN_NAME_FAVOURITE_MAIN,
                    AnalyticsKeys.SE_TAP_FAVOURITE,
                    AnalyticsKeys.FALSE
                )
            }
        })
    }

    private fun getFavouriteRecords() {
        viewModel.getFavouriteRecords(requireContext())
    }

    private fun setAdapter(records: List<FavouriteRecord>) {
        val recordsWithStatus = ArrayList<FavouritesAdapterListModel>()
        val layoutManager = LinearLayoutManager(context)
        rvFavourites.layoutManager = layoutManager
        rvFavourites.addItemDecoration(getItemDecorator())
        records.forEach {
            recordsWithStatus.add(FavouritesAdapterListModel(it))
        }
        val adapter = FavouriteListAdapter(
            requireContext(),
            recordsWithStatus
        )
        rvFavourites.adapter = adapter
        adapter.addCallback(this)
        setSearchFilter(adapter)
    }

    private fun getItemDecorator(): DividerItemDecoration {
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divier)
            ?.let { itemDecoration.setDrawable(it) }
        return itemDecoration
    }

    private fun setSearchFilter(adapter: FavouriteListAdapter) {
        searchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // filter recycler view when text is changed
                adapter.filter.filter(s?.trim()) {
                    if (adapter.itemCount > 0) {
                        yourFavPlacesLayout.visibility = View.VISIBLE
                        rvFavourites.visibility = View.VISIBLE
                        noResultsLayout.visibility = View.GONE
                    } else {
                        yourFavPlacesLayout.visibility = View.GONE
                        rvFavourites.visibility = View.GONE
                        noResultsLayout.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun setClickListener() {
        seeMyHistoryButton.setOnClickListener {
            activity?.finish()
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra(GO_TO_HISTORY, true)
            startActivity(intent)
            activity?.overridePendingTransition(0, 0)
        }
    }

    override fun onStarClicked(isChecked: Boolean, favouriteRecord: FavouriteRecord) {
        if (isChecked) {
            insertRecord(favouriteRecord)
        } else {
            viewModel.deleteFavRecord(
                requireContext(),
                favouriteRecord.venueId,
                favouriteRecord.tenantId
            )
        }
    }

    override fun onItemClick(favouriteRecord: FavouriteRecord) {
        openSafeCheckInOutActivity(
                requireContext(),
                favouriteRecord.venueName,
                favouriteRecord.venueId,
                favouriteRecord.tenantName,
                favouriteRecord.tenantId,
                favouriteRecord.postalCode,
                favouriteRecord.address
            )
    }

    private fun insertRecord(favouriteRecord: FavouriteRecord) {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.insertRecord(requireContext(), favouriteRecord)
            viewModel.showSnackBar(
                requireContext(),
                favouritesLayout,
                R.string.saved_to_favourites
            )
            AnalyticsUtils().trackEvent(
                AnalyticsKeys.SCREEN_NAME_FAVOURITE_MAIN,
                AnalyticsKeys.SE_TAP_FAVOURITE,
                AnalyticsKeys.TRUE
            )
        }
    }

    private fun openSafeCheckInOutActivity(
        context: Context,
        venueName: String,
        venueId: String,
        tenantName: String,
        tenantId: String,
        postalCode: String,
        address: String
    ) {
        val venueObject = QrResultDataModel(
            venueName,
            venueId,
            tenantName,
            tenantId,
            postalCode,
            address,
            null,
            0, 0
        )
        val venueList: ArrayList<QrResultDataModel> = arrayListOf(venueObject)
        if (!venueList.isNullOrEmpty()) {
//            AndroidBus.behaviorSubject.onNext(venueList)
            /***** old safe entry flow ******/
            //val intent = Intent(context, SafeCheckInOutActivity::class.java)
            /***** new safe entry refactored flow ******/
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
                (context as SafeEntryActivity).isFromGroupCheckIn
            )
            (context as Activity).startActivityForResult(
                intent,
                SafeEntryActivity.REQUEST_ACTION
            )
        } else {
            showErrorDialog(context)
        }
    }

    private fun showErrorDialog(context: Context) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        alertBuilder.show(context, AlertType.FAVOURITE_CHECK_IN_ERROR) {
            if (it) {
                val message = "Error occurred during Favourite Check-In"
                CentralLog.e(loggerTAG, message)
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    message,
                    null
                )
            }
        }
    }
}