package sg.gov.tech.bluetrace.fragment

import android.content.Context
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.bt_possible_exposure_rv_item.view.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.safeentry.selfcheck.model.CheckoutInfo
import sg.gov.tech.safeentry.selfcheck.model.SafeEntryMatch

class HistoryPossibleExposureListAdapter(
    val context: Context,
    val questionButtonClickListener: View.OnClickListener,
    val checkForSymptomsClickListener: View.OnClickListener,
    private val historyList: List<Pair<Int, List<SafeEntryMatch>>>
) :
    RecyclerView.Adapter<HistoryPossibleExposureListAdapter.HistoryItemViewHolder>(),
    ScrollToBottom {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var mRecyclerView: RecyclerView

    inner class HistoryItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstItemLayout: ConstraintLayout = itemView.possible_exposure_first_item_layout
        val dateSeparatorTv: TextView = itemView.possible_exposure_history_item_date
        val redItemsLinearLayout: LinearLayout = itemView.possible_exposure_red_item_linear_layout
        val questionButton: ImageView = itemView.possible_exposure_question_button
        val checkForSymptomsTv: TextView = itemView.check_for_symptoms

        init {
            checkForSymptomsTv.paintFlags = checkForSymptomsTv.paintFlags or UNDERLINE_TEXT_FLAG
            checkForSymptomsTv.text = HtmlCompat.fromHtml(
                context.getString(R.string.check_for_symptoms),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            checkForSymptomsTv.setOnClickListener(checkForSymptomsClickListener)
            questionButton.setOnClickListener(questionButtonClickListener)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun scrollToBottomOfList() {
        mRecyclerView.smoothScrollToPosition(historyList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItemViewHolder {
        return HistoryItemViewHolder(
            inflater.inflate(
                R.layout.bt_possible_exposure_rv_item,
                parent,
                false
            )
        )
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
            checkOutDate == 0L -> " - " + context.getString(R.string.no_check_out)
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
        givenDayMs: Long
    ): String {
        return when (checkOutTimeMS) {
            0L -> Utils.getHourPmAm(checkInTimeMS) + " - " + context.getString(R.string.no_check_out)
            else -> Utils.getHourPmAm(checkInTimeMS) + getCheckOutFormattedDate(
                givenDayMs,
                checkOutTimeMS
            )
        }
    }

    fun getCheckOutTimeMs(checkout: CheckoutInfo?): Long {
        if (checkout == null)
            return 0L
        return checkout.time * 1000
    }

    override fun onBindViewHolder(holder: HistoryItemViewHolder, position: Int) {
        if (position == 0) {
            holder.firstItemLayout.visibility = View.VISIBLE
        }
        holder.dateSeparatorTv.text =
            Utils.getShortDateWithComaAfterDay(historyList[position].second[0].safeentry.checkin.time * 1000)


        if (holder.redItemsLinearLayout.childCount == 0 && historyList[position].second.isNotEmpty()) {
            historyList[position].second.forEachIndexed { index, it ->
                val view = BtHistoryPossibleExposureRedItemView(context)
                view.adapter = this
                if (position == historyList.size - 1 && index == historyList[position].second.size - 1)
                    view.isLastItem = true
                view.location.text = it.safeentry.location.description
                view.date.text =
                    getSeRecordTime(
                        it.safeentry.checkin.time * 1000,
                        getCheckOutTimeMs(it.safeentry.checkout),
                        it.safeentry.checkin.time * 1000
                    )
                view.feedHotSpotLinearLayout(it.hotspots)
                holder.redItemsLinearLayout.addView(view)

                if (it.hotspots.isNotEmpty()) {
                    view.visitedText.text = context.getString(
                        R.string.a_covid_19_case_visited,
                        it.hotspots[0].location.address ?: "Unknown"
                    )
                    view.visitedText.visibility = View.VISIBLE
                } else {
                    view.visitedText.visibility = View.GONE
                }
            }
        }
    }
}
