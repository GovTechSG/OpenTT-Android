package sg.gov.tech.bluetrace.favourite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.favourite_list_item.view.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import java.util.*
import kotlin.collections.ArrayList

class FavouriteListAdapter(context: Context, val favouriteList: List<FavouritesAdapterListModel>) :
    RecyclerView.Adapter<FavouriteListAdapter.FavouriteItemViewHolder>(), Filterable {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var searchedFavouriteList: List<FavouritesAdapterListModel> = favouriteList
    private lateinit var mCallback: Callback

    class FavouriteItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: AppCompatTextView = itemView.title_text
        val addressTextView: AppCompatTextView = itemView.address_text
        val starCheckBox: AppCompatCheckBox = itemView.star_checkbox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteItemViewHolder {
        return FavouriteItemViewHolder(
            inflater.inflate(
                R.layout.favourite_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = searchedFavouriteList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: FavouriteItemViewHolder, position: Int) {
        val favouriteData = searchedFavouriteList[position].favRecords
        holder.titleTextView.text = getLocationName(favouriteData)
        holder.addressTextView.text = favouriteData.address
        holder.starCheckBox.isChecked = searchedFavouriteList[position].isChecked
        holder.starCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (searchedFavouriteList[position].isChecked !== isChecked) {
                searchedFavouriteList[position].isChecked = isChecked
                var pos = favouriteList.indexOf(searchedFavouriteList[position])
                if (pos != -1)
                    favouriteList[pos].isChecked = isChecked
                mCallback.onStarClicked(isChecked, favouriteData)
            }

        }
        holder.itemView.setOnClickListener {
            mCallback.onItemClick(favouriteData)
        }
    }

    override fun getFilter(): Filter {

        return object : Filter() {

            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                searchedFavouriteList = if (charString.isBlank()) {
                    favouriteList
                } else {
                    val filteredList: MutableList<FavouritesAdapterListModel> = ArrayList()
                    for (item in favouriteList) {
                        // tenantName or venueName match condition
                        var favourite = item.favRecords
                        if (favourite.tenantName.toLowerCase(Locale.getDefault())
                                .contains(charString.toLowerCase(Locale.getDefault()))
                            || (favourite.tenantName.isEmpty() && favourite.venueName.toLowerCase(
                                Locale.getDefault()
                            )
                                .contains(charString.toLowerCase(Locale.getDefault())))
                        ) {
                            filteredList.add(item)
                        }
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = searchedFavouriteList
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence,
                filterResults: FilterResults
            ) {
                searchedFavouriteList = filterResults.values as List<FavouritesAdapterListModel>
                notifyDataSetChanged()
            }
        }
    }

    private fun getLocationName(record: FavouriteRecord) = if (record.tenantName.isEmpty()) {
        record.venueName
    } else {
        record.tenantName
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onStarClicked(isChecked: Boolean, favouriteRecord: FavouriteRecord)
        fun onItemClick(favouriteRecord: FavouriteRecord)
    }
}
