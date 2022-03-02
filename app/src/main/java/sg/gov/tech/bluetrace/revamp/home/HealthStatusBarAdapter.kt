package sg.gov.tech.bluetrace.revamp.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils

class HealthStatusBarAdapter(val itemList: ArrayList<HealthStatusModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var context: Context
    private lateinit var mCallback: Callback

    override fun getItemViewType(position: Int): Int {
        return itemList[position].state
    }

    private val LAYOUT_ACTIVE = 1
    private val LAYOUT_DISABLED = 0

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon = itemView.findViewById<AppCompatImageView>(R.id.iv_icon)
        var rootCl = itemView.findViewById<ConstraintLayout>(R.id.root_cl)
        var title = itemView.findViewById<AppCompatTextView>(R.id.title_txt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.health_status_bar_item, parent, false)
        return ItemViewHolder(view)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder as ItemViewHolder
        if (position == (itemList.size - 1)) {
            val layoutParams: ViewGroup.MarginLayoutParams =
                view.rootCl.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 0, 0)
            view.rootCl.requestLayout()

        } else {
            val layoutParams: ViewGroup.MarginLayoutParams =
                view.rootCl.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, Utils.dpToPx(context, 8f), 0)
            view.rootCl.requestLayout()
        }
        view.icon.setImageDrawable(ContextCompat.getDrawable(context, itemList[position].resId))
        view.title.text = itemList[position].title

        when (holder.itemViewType) {
            LAYOUT_ACTIVE -> {
                view.rootCl.background =
                    ContextCompat.getDrawable(context, R.drawable.health_status_box)
            }
            LAYOUT_DISABLED -> {
                view.rootCl.background =
                    ContextCompat.getDrawable(context, R.drawable.dotted_border_line_status)
            }
        }

        view.rootCl.setOnClickListener {
            if (position != 2)
                mCallback.onHealthStatusItemClick(position)
        }
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onHealthStatusItemClick(position: Int)
    }
}