package sg.gov.tech.bluetrace

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.safe_layout_check_in_to_rv_item.view.*
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2

class SafeCheckInToRecyclerViewAdapter internal constructor(context: Context) :
    RecyclerView.Adapter<SafeCheckInToRecyclerViewAdapter.TenantViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var tenantList: List<QrResultDataModel>

    class TenantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameTv: TextView = itemView.safe_check_in_to_item_title
        val addressTv: TextView = itemView.safe_check_in_to_item_address
        val itemContainer: ConstraintLayout = itemView.safe_check_in_to_rv_view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TenantViewHolder {
        val itemView = inflater.inflate(R.layout.safe_layout_check_in_to_rv_item, parent, false)
        return TenantViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return tenantList.size
    }

    fun setTenantList(list: List<QrResultDataModel>) {
        tenantList = list
    }

    override fun onBindViewHolder(holder: TenantViewHolder, position: Int) {
        val current = tenantList[position]
        holder.placeNameTv.text =
            if (current.tenantName.isNullOrEmpty()) current.venueName else current.tenantName
        holder.addressTv.text = current.address
        holder.itemContainer.setOnClickListener { view ->
            val bundle = bundleOf(SafeEntryCheckInOutActivityV2.SE_VENUE to current)
            /***** old safe entry flow ******/
            /*view.findNavController().navigate(
                R.id.action_fragmentSafeEntryCheckInToConfirmation_to_safeEntryCheckInOutFragment,
                bundle*/
            /***** new safe entry refactored flow ******/
            view.findNavController().navigate(
                R.id.action_safeEntryVenueListFragment_to_safeEntryCheckInFragment,
                bundle
            )
            /*val bundle =
                bundleOf("venue" to current)
            view.findNavController().navigate(
                R.id.action_fragmentSafeEntryCheckInToList_to_fragmentSafeEntryCheckInToConfirmation,
                bundle
            )*/
        }
    }
}
