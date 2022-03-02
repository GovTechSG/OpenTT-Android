package sg.gov.tech.bluetrace.debugger

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.log_item.view.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.logging.persistence.LogRecord
import java.text.SimpleDateFormat
import java.util.*

class LogListAdapter(context: Context, private val logRecordList: List<LogRecord>) :
    RecyclerView.Adapter<LogListAdapter.LogRecordViewHolder>(), Filterable {

    private var searchedLogRecordList: List<LogRecord> = logRecordList
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss:mmm", Locale.ENGLISH)

    class LogRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: AppCompatTextView = itemView.time_text
        val levelTextView: AppCompatTextView = itemView.level_text
        val typeTextView: AppCompatTextView = itemView.type_text
        val tagTextView: AppCompatTextView = itemView.tag_text
        val messageTextView: AppCompatTextView = itemView.message_text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogRecordViewHolder {
        return LogRecordViewHolder(
            inflater.inflate(R.layout.log_item, parent, false)
        )
    }

    override fun getItemCount() = searchedLogRecordList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: LogRecordViewHolder, position: Int) {
        val logRecord = searchedLogRecordList[position]
        holder.timeTextView.text = dateFormat.format(logRecord.time)
        holder.levelTextView.text = logRecord.level
        holder.typeTextView.text = logRecord.type
        holder.tagTextView.text = logRecord.tag
        holder.messageTextView.text = logRecord.message
    }

    override fun getFilter(): Filter {

        return object : Filter() {

            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                searchedLogRecordList = if (charString.isBlank()) {
                    logRecordList
                } else {
                    val filteredList: MutableList<LogRecord> = ArrayList()
                    for (logRecord in logRecordList) {
                        if (logRecord.message.contains(charString, false))
                            filteredList.add(logRecord)
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = searchedLogRecordList
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence,
                filterResults: FilterResults
            ) {
                searchedLogRecordList = filterResults.values as List<LogRecord>
                notifyDataSetChanged()
            }
        }
    }
}