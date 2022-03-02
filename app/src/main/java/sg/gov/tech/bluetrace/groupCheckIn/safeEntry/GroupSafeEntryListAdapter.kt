package sg.gov.tech.bluetrace.groupCheckIn.safeEntry

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.group_safe_entry_list_item.view.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import java.util.*

private const val TYPE_HEADER = 0
private const val TYPE_ITEM = 1

class GroupSafeEntryListAdapter(
    context: Context, private val familyMembersList: List<FamilyMember>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mSelectedItemsIds = SparseBooleanArray()
    private lateinit var mCallback: Callback
    private val mContext = context

    override fun getItemViewType(position: Int): Int {
        return if (position == 0)
            TYPE_HEADER
        else
            TYPE_ITEM
    }

    class HeaderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberImageView: AppCompatImageView = itemView.family_member_image
        val nameTextView: AppCompatTextView = itemView.name_text
        val nricTextView: AppCompatTextView = itemView.nric_text
        val removeMemberImageView: AppCompatCheckBox = itemView.selection_checkbox
    }

    class FamilyMemberItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
        val memberImageView: AppCompatImageView = itemView.family_member_image
        val nameTextView: AppCompatTextView = itemView.name_text
        val nricTextView: AppCompatTextView = itemView.nric_text
        val selectionCheckbox: AppCompatCheckBox = itemView.selection_checkbox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderItemViewHolder(
                inflater.inflate(
                    R.layout.group_safe_entry_list_item,
                    parent,
                    false
                )
            )
        } else {
            FamilyMemberItemViewHolder(
                inflater.inflate(
                    R.layout.group_safe_entry_list_item,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount() = familyMembersList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val familyMember = familyMembersList[position]
        if (holder is HeaderItemViewHolder) {
            holder.memberImageView.setImageResource(R.drawable.ic_pink_otter)
            holder.nameTextView.text = mContext.getString(R.string.user_you, familyMember.nickName)
                .toUpperCase(Locale.getDefault())
            holder.nricTextView.text = Utils.maskIdWithDot(familyMember.nric).toUpperCase(Locale.getDefault())
            holder.removeMemberImageView.visibility = View.GONE
        }

        if (holder is FamilyMemberItemViewHolder) {
            holder.memberImageView.setImageResource(getImageResourceId(position))
            holder.nameTextView.text = familyMember.nickName
            holder.nricTextView.text = familyMember.nric
            holder.selectionCheckbox.visibility = View.VISIBLE
            holder.selectionCheckbox.isChecked = mSelectedItemsIds.get(position)
            holder.view.setOnClickListener {
                checkCheckBox(position, !mSelectedItemsIds.get(position))
                mCallback.onMemberItemClicked()
            }
        }
    }

    private fun getImageResourceId(position: Int): Int {
        return when {
            (position + 3) % 4 == 0 -> R.drawable.ic_red_merlion
            (position + 2) % 4 == 0 -> R.drawable.ic_orange_otter
            (position + 1) % 4 == 0 -> R.drawable.ic_blue_merlion
            else -> R.drawable.ic_teal_merlion
        }
    }

    /**
     * Check the Checkbox if not checked
     */
    fun checkCheckBox(position: Int, value: Boolean) {
        if (value) mSelectedItemsIds.put(position, true) else mSelectedItemsIds.delete(position)
        notifyDataSetChanged()
    }

    /**
     * Remove all checkbox Selection
     */
    fun removeSelection() {
        mSelectedItemsIds = SparseBooleanArray()
        notifyDataSetChanged()
    }

    /**
     * Return the selected Checkbox IDs
     */
    fun getSelectedIds(): SparseBooleanArray {
        return mSelectedItemsIds
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onMemberItemClicked()
    }
}
