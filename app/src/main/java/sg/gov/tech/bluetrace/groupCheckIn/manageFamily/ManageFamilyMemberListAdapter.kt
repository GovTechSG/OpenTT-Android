package sg.gov.tech.bluetrace.groupCheckIn.manageFamily

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.family_member_list_item.view.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC
import java.util.*

class ManageFamilyMemberListAdapter(
    context: Context, private val familyMembersList: List<FamilyMembersRecord>
) : RecyclerView.Adapter<ManageFamilyMemberListAdapter.FamilyMemberItemViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var mCallback: Callback
    private val mContext = context

    class FamilyMemberItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberImageView: AppCompatImageView = itemView.family_member_image
        val nameTextView: AppCompatTextView = itemView.name_text
        val nricTextView: AppCompatTextView = itemView.nric_text
        val removeMemberImageView: AppCompatImageView = itemView.remove_family_member_image
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyMemberItemViewHolder {
        return FamilyMemberItemViewHolder(
            inflater.inflate(
                R.layout.family_member_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = familyMembersList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: FamilyMemberItemViewHolder, position: Int) {
        val familyMember = familyMembersList[position]
        holder.memberImageView.setImageResource(getImageResourceId(position))
        holder.nameTextView.text = familyMember.nickName
        holder.nricTextView.text = getMaskedNRIC(familyMember.nric)
        holder.removeMemberImageView.setOnClickListener {
            mCallback.onMemberRemoved(familyMember)
        }
    }

    private fun getMaskedNRIC(nric: String): String {
        val decryptedNRIC = getDecryptedFamilyMemberNRIC(mContext, nric)
        return if (decryptedNRIC != null)
            Utils.maskIdWithDot(decryptedNRIC).toUpperCase(Locale.getDefault())
        else
            ""
    }

    private fun getImageResourceId(position: Int): Int {
        return when {
            (position + 3) % 4 == 0 -> R.drawable.ic_orange_otter
            (position + 2) % 4 == 0 -> R.drawable.ic_blue_merlion
            (position + 1) % 4 == 0 -> R.drawable.ic_teal_merlion
            else -> R.drawable.ic_red_merlion
        }
    }

    fun addCallback(callback: Callback) {
        mCallback = callback
    }

    interface Callback {
        fun onMemberRemoved(familyMembers: FamilyMembersRecord)
    }
}
