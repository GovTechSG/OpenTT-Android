package sg.gov.tech.bluetrace.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.bt_history_list_item.view.*
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import java.util.*

class HistoryListAdapter(
    val context: Context,
    val questionButtonClickListener: View.OnClickListener,
    private val historyList: List<DayHistoryRecord>,
    private val favouriteRecordsList: List<FavouriteRecord>
) :
    RecyclerView.Adapter<HistoryListAdapter.HistoryItemViewHolder>(), ScrollToBottom {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var mCallback: Callback
    private lateinit var mRecyclerView: RecyclerView

    inner class HistoryItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView = itemView.history_date_separator
        val exchangesCount = itemView.bt_history_exchange_count_text
        val noOfDaysView = itemView.noOfDaysView
        val noOfDays = itemView.noOfDays
        val exchangesCountView = itemView.bt_history_exchanges_count_view
        val seRecordsLinearLayout = itemView.bt_history_se_records_vertical_layout

        init {
            itemView.bt_history_list_question_button.setOnClickListener(questionButtonClickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItemViewHolder {
        return HistoryItemViewHolder(
            inflater.inflate(
                R.layout.bt_history_list_item,
                parent,
                false
            )
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun getItemCount() = historyList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun getCheckOutFormattedDate(givenDay: Long, checkOutDate: Long): String {
        return when {
            checkOutDate == 0L -> ""
            Utils.compareDate(
                givenDay,
                checkOutDate
            ) == 0 -> " - ${Utils.getHourPmAm(checkOutDate)}"
            else -> " - ${Utils.getDayAndHourWithComaAfterDay(checkOutDate)}"
        }
    }

    private fun getSeRecordTime(
        checkInTimeMS: Long,
        checkOutTimeMS: Long,
        givenDay: Long,
        isRedItem: Boolean = false
    ): String {
        val noCheckOut = if (isRedItem) " - ${context.getString(R.string.no_check_out)}" else ""
        return when (checkOutTimeMS) {
            0L -> "${Utils.getHourPmAm(checkInTimeMS)}${noCheckOut}"
            else -> Utils.getHourPmAm(checkInTimeMS) + getCheckOutFormattedDate(
                givenDay,
                checkOutTimeMS
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HistoryItemViewHolder, position: Int) {

//        holder.noOfDays.text =
//            "${context.getString(R.string.past)} ${BuildConfig.NO_OF_DAYS_FOR_HISTORY} ${context.getString(
//                R.string.days
//            )}"
        holder.dayTextView.text = Utils.getShortDateWithComaAfterDay(historyList[position].dayInMs)

        //hide the Exchanges text for the entries before registration
        holder.exchangesCount.text = historyList[position].btRecordsCount.toString()
        if(Date(historyList[position].dayInMs).before(Date(Preference.getUserRegistrationDate(context)))){
            if(historyList[position].btRecordsCount<=0){
                holder.exchangesCountView.visibility =
                    View.GONE
            }
        }
        when {
            position >= 1 -> holder.noOfDaysView.visibility = View.GONE
            historyList[position].btRecordsCount == -1 -> holder.exchangesCountView.visibility =
                View.GONE
        }

        if (holder.seRecordsLinearLayout.childCount == 0 && historyList[position].historyRecords.isNotEmpty()) {
            historyList[position].historyRecords.forEachIndexed { index, safeEntryRecord ->
                val isRedItem = historyList[position].historyRecords[index].hotSpots.isNotEmpty()
                when (isRedItem) {
                    true -> {
                        val possibleExposureRedItemView =
                        BtHistoryPossibleExposureRedItemView(context)
                        possibleExposureRedItemView.adapter = this
                        if (position == historyList.size - 1 && index == historyList[position].historyRecords.size - 1)
                            possibleExposureRedItemView.isLastItem = true
                        possibleExposureRedItemView.feedHotSpotForHistoryRecordLinearLayout(
                            historyList[position].historyRecords[index].hotSpots
                        )
                        possibleExposureRedItemView.location.text =
                            historyList[position].historyRecords[index].locationLabel; possibleExposureRedItemView.date.text =
                            getSeRecordTime(
                                historyList[position].historyRecords[index].checkInTimeMs,
                                historyList[position].historyRecords[index].checkOutTimeMs ?: 0L,
                                historyList[position].dayInMs,
                                isRedItem
                            )

                        if (historyList[position].historyRecords[index].hotSpots.isNotEmpty()) {
                            possibleExposureRedItemView.visitedText.text = context.getString(
                                R.string.a_covid_19_case_visited,
                                historyList[position].historyRecords[index].hotSpots[0].address
                            )
                            possibleExposureRedItemView.visitedText.visibility = View.VISIBLE
                        } else {
                            possibleExposureRedItemView.visitedText.visibility = View.GONE
                        }
                        holder.seRecordsLinearLayout.addView(possibleExposureRedItemView)
            }
            false -> {
                val btHistorySeRecordView = BtHistorySeRecordView(context)
                btHistorySeRecordView.starCheckBox.isChecked =
                    isFavourite(historyList[position].historyRecords[index])
                btHistorySeRecordView.starCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    mCallback.onStarClicked(isChecked, safeEntryRecord)
                }
                if (index == 0)
                    when (historyList[position].btRecordsCount) {
                        -1 -> btHistorySeRecordView.separator.visibility =
                            View.GONE
                    }
                btHistorySeRecordView.location.text =
                    historyList[position].historyRecords[index].locationLabel
                if (historyList[position].historyRecords[index].checkOutTimeMs == null
                    && isDisplayCheckOutNow(historyList[position].historyRecords[index].checkInTimeMs)
                ) {
                    btHistorySeRecordView.checkOutNow.visibility = View.VISIBLE
                } else {
                    btHistorySeRecordView.checkOutNow.visibility = View.GONE
                }
                btHistorySeRecordView.checkOutNow.setOnClickListener {
                    mCallback.onCheckoutNowClicked(safeEntryRecord, position, index)
                }
                btHistorySeRecordView.time.text =
                    getSeRecordTime(
                        historyList[position].historyRecords[index].checkInTimeMs,
                        historyList[position].historyRecords[index].checkOutTimeMs ?: 0L,
                        historyList[position].dayInMs
                    )
                holder.seRecordsLinearLayout.addView(btHistorySeRecordView)
            }
                }
            }
        }
    }

    private fun isDisplayCheckOutNow(checkInTimeMs: Long): Boolean {
        var time24HourAfterCheckin = Calendar.getInstance()
        time24HourAfterCheckin.timeInMillis = DateTools.getTimeHoursAfter((24 * 60), checkInTimeMs)
        return (Calendar.getInstance().before(time24HourAfterCheckin))
    }

    private fun isFavourite(safeEntryRecord: HistoryRecord): Boolean {
        for (favourite in favouriteRecordsList) {
            if (safeEntryRecord.venueId == favourite.venueId && safeEntryRecord.tenantId == favourite.tenantId)
                return true
        }
        return false
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onStarClicked(
            isChecked: Boolean,
            safeEntryRecord: HistoryRecord
        )

        fun onCheckoutNowClicked(
            safeEntryRecord: HistoryRecord,
            position: Int,
            historyRecordPosition: Int
        )
    }

    fun updateCheckOutTime(checkoutTime: Long, position: Int, historyRecordPosition: Int) {
        historyList[position].historyRecords[historyRecordPosition].checkOutTimeMs = checkoutTime
    }

    override fun scrollToBottomOfList() {
        mRecyclerView.smoothScrollToPosition(historyList.size)
    }
}
