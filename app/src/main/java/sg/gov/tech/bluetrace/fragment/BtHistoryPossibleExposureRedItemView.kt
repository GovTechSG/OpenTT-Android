package sg.gov.tech.bluetrace.fragment

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.safeentry.selfcheck.model.CheckoutInfo
import sg.gov.tech.safeentry.selfcheck.model.HotSpot

class BtHistoryPossibleExposureRedItemView @JvmOverloads constructor(
    context: Context,
    private val favouriteRecordsList: List<FavouriteRecord> = listOf(),
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    var location: TextView
    var date: TextView
    private var hideableView: CardView
    var redView: ConstraintLayout
    lateinit var hotSpotItemView: LinearLayout
    var redViewArrow: ImageView
    lateinit var adapter: ScrollToBottom
    var isLastItem: Boolean = false
    var visitedText: TextView

    private fun getCheckOutFormattedDate(givenDayMs: Long, checkOutDateMs: Long): String {
        return when {
            checkOutDateMs == 0L -> " - " + context.getString(R.string.no_check_out)
            Utils.compareDate(
                givenDayMs,
                checkOutDateMs
            ) == 0 -> " - ${Utils.getHourPmAm(checkOutDateMs)}"
            else -> " - ${Utils.getDayAndHourWithComaAfterDay(checkOutDateMs)}"
        }
    }

    private fun getSeRecordTime(
        checkInTimeMS: Long,
        checkOutTimeMS: Long?,
        givenDay: Long
    ): String {
        return when (checkOutTimeMS) {
            0L -> Utils.getHourPmAm(checkInTimeMS) + " - " + context.getString(R.string.no_check_out)
            else -> Utils.getHourPmAm(checkInTimeMS) + getCheckOutFormattedDate(
                givenDay,
                checkOutTimeMS ?: 0L
            )
        }
    }

    fun feedHotSpotForHistoryRecordLinearLayout(hotSpots: MutableList<HotSpotData>) {
        hotSpots.forEach { hotSpot ->
            hotSpotItemView = findViewById(R.id.possible_exposure_history_item_linear_layout)
            val view = BtHistoryPossibleExposureRecordView(context)
            view.location.text = hotSpot.locationLabel
            view.date.text = getSeRecordTime(
                hotSpot.checkInTimeMs,
                hotSpot.checkOutTimeMs,
                hotSpot.checkInTimeMs
            )
            view.starCheckBox.visibility = View.GONE
            view.starCheckBox.isChecked = false
            hotSpotItemView.addView(view)
        }
    }

    fun feedHotSpotLinearLayout(hotSpots: ArrayList<HotSpot>) {
        hotSpots.forEach { hotSpot ->
            hotSpotItemView = findViewById(R.id.possible_exposure_history_item_linear_layout)
            val view = BtHistoryPossibleExposureRecordView(context)
            view.location.text = hotSpot.location.description
            view.date.text = getSeRecordTime(
                hotSpot.timeWindow.start * 1000,
                hotSpot.timeWindow.end * 1000,
                hotSpot.timeWindow.start * 1000
            )
            view.starCheckBox.visibility = View.GONE
            hotSpotItemView.addView(view)
        }
    }

    init {
        val view = inflate(context, R.layout.bt_possible_exposure_red_item, this)
        location = view.findViewById(R.id.possible_exposure_history_item_location_text)
        date = view.findViewById(R.id.possible_exposure_history_item_time)
        hideableView = findViewById(R.id.possible_exposure_history_item_hideable_layout)
        redView = findViewById(R.id.possible_exposure_red_item_layout)
        redViewArrow = findViewById(R.id.possible_exposure_history_item_arrow)
        visitedText = findViewById(R.id.possible_exposure_history_item_bottom_text)

        redView.setOnClickListener {
            when (hideableView.visibility) {
                View.VISIBLE -> {
                    hideableView.visibility = View.GONE
                    redViewArrow.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_right_small))
                }
                View.GONE -> {
                    if (isLastItem) {
                        adapter.scrollToBottomOfList()
                    }
                    hideableView.visibility = View.VISIBLE
                    redViewArrow.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_up_small))
                }
            }
        }
    }
}
