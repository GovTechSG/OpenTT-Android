package sg.gov.tech.bluetrace.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.language_item.view.*
import sg.gov.tech.bluetrace.R

class LanguageAdapter(val languages: List<String>, val languageCodes: List<String>) :
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var listener: OnItemClickListener? = null
    var prevPressed: View? = null

    class LanguageViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView) {
        val textView = rootView.text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val textView =
            LayoutInflater.from(parent.context).inflate(R.layout.language_item, parent, false)
        return LanguageViewHolder(textView)
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.textView.text = languages[position]
        holder.rootView.setOnClickListener {
            listener?.onItemClick(prevPressed, it, position)
            prevPressed = it
        }
        holder.setIsRecyclable(false)
        holder.rootView.tag = holder
    }
}

interface OnItemClickListener {
    fun onItemClick(prevClickedView: View?, view: View, position: Int)
}
