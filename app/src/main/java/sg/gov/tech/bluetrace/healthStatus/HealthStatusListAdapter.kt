package sg.gov.tech.bluetrace.healthStatus

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.health_status_list_item.view.*
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.safeentry.selfcheck.model.HealthStatus
import sg.gov.tech.safeentry.selfcheck.model.SafeEntrySelfCheck
import sg.gov.tech.safeentry.selfcheck.model.VaccinationInfo

class HealthStatusListAdapter(
    context: Context,
    private val healthStatus: HealthStatus
) : RecyclerView.Adapter<HealthStatusListAdapter.HealthStatusItemViewHolder>() {

    private val mContext = context
    private val inflater: LayoutInflater = LayoutInflater.from(mContext)
    private lateinit var mCallback: Callback

    class HealthStatusItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.cardView
        val titleTextView: AppCompatTextView = itemView.title_text
        val subTitleTextView: AppCompatTextView = itemView.subtitle_text
        val detailsTextView: AppCompatTextView = itemView.details_text
        val urlTextView: AppCompatTextView = itemView.url_text
        val iconImageView: AppCompatImageView = itemView.icon_image_view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthStatusItemViewHolder {
        return HealthStatusItemViewHolder(
            inflater.inflate(
                R.layout.health_status_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = 2

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: HealthStatusItemViewHolder, position: Int) {
        holder.urlTextView.paintFlags = holder.urlTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        if (position == 0) {
            setVaccinationData(holder, healthStatus.vaccination)
            setVaccinationStyling(holder, healthStatus.vaccination)
        } else if (position == 1) {
            setSelfCheckData(holder, healthStatus.selfCheck)
            setSelfCheckStyling(holder, healthStatus.selfCheck)
        }
    }

    private fun setVaccinationData(
        holder: HealthStatusItemViewHolder,
        vaccination: VaccinationInfo
    ) {
        holder.titleTextView.text = mContext.getString(R.string.vaccination_status)
        holder.subTitleTextView.text = vaccination.header
        holder.detailsTextView.text = vaccination.subtext
        holder.urlTextView.text = vaccination.urlText
        holder.urlTextView.setOnClickListener {
            mCallback.onItemClick(healthStatus.vaccination.urlLink, false)
        }
    }

    private fun setSelfCheckData(
        holder: HealthStatusItemViewHolder,
        selfCheck: SafeEntrySelfCheck
    ) {
        holder.titleTextView.text = mContext.getString(R.string.possible_ex)
        if (selfCheck.count == 0) {
            holder.subTitleTextView.text = mContext.getString(R.string.no_exposure_alerts)
            holder.detailsTextView.text = mContext.getString(R.string.records_from_the_last_14_days)
            holder.urlTextView.text =
                mContext.getString(R.string.how_are_my_possible_exposures_determined)
            holder.urlTextView.setOnClickListener {
                mCallback.onItemClick(BuildConfig.HOW_POSSIBLE_EXPOSURE_DETERMINED_URL, false)
            }
        } else {
            holder.subTitleTextView.text = if (selfCheck.count == 1)
                mContext.getString(R.string.count_possible_exposure, selfCheck.count.toString())
            else
                mContext.getString(
                    R.string.count_possible_exposures,
                    selfCheck.count.toString()
                )
            holder.detailsTextView.text = mContext.getString(R.string.possible_exposure_details)
            holder.urlTextView.text = mContext.getString(R.string.see_details)
            holder.cardView.setOnClickListener {
                mCallback.onItemClick("", true)
            }
        }
    }

    private fun setVaccinationStyling(
        holder: HealthStatusItemViewHolder,
        vaccination: VaccinationInfo
    ) {
        holder.urlTextView.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            R.drawable.ic_open_in_new,
            0
        )
        if (vaccination.isVaccinated) {
            holder.iconImageView.setImageResource(R.drawable.ic_details_vaccinated)
            setGreenCard(holder)
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_details_not_vaccinated)
            setGreyCard(holder)
        }
    }

    private fun setSelfCheckStyling(
        holder: HealthStatusItemViewHolder,
        selfCheck: SafeEntrySelfCheck
    ) {
        if (selfCheck.count > 0) {
            holder.iconImageView.setImageResource(R.drawable.ic_details_possible_exposure)
            setPinkCard(holder)
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_details_no_exposure)
            setGreenCard(holder)
        }
    }

    private fun setGreenCard(holder: HealthStatusItemViewHolder) {
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                mContext,
                R.color.close_box_bg
            )
        )
        holder.subTitleTextView.setTextColor(
            ContextCompat.getColor(
                mContext,
                R.color.green_text
            )
        )
    }

    private fun setGreyCard(holder: HealthStatusItemViewHolder) {
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                mContext,
                R.color.unselected_accent
            )
        )
        holder.subTitleTextView.setTextColor(
            ContextCompat.getColor(
                mContext,
                R.color.bt_text
            )
        )
    }

    private fun setPinkCard(holder: HealthStatusItemViewHolder) {
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                mContext,
                R.color.pink
            )
        )
        holder.subTitleTextView.setTextColor(
            ContextCompat.getColor(
                mContext,
                R.color.colorAccent
            )
        )
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onItemClick(urlLink: String, isPossibleExposure: Boolean)
    }
}