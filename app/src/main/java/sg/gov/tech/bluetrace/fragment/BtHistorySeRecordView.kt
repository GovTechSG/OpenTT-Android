package sg.gov.tech.bluetrace.fragment

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import sg.gov.tech.bluetrace.R

class BtHistorySeRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    var location: TextView
    var time: TextView
    var separator: View
    var starCheckBox: AppCompatCheckBox
    var checkOutNow: TextView

    init {
        val view = inflate(context, R.layout.bt_history_item_list_item_location_and_time, this)
        location = view.findViewById(R.id.bt_history_item_location)
        time = view.findViewById(R.id.bt_history_list_item_time)
        separator = view.findViewById(R.id.bt_history_list_item_separator)
        starCheckBox = view.findViewById(R.id.star_checkbox)
        checkOutNow = view.findViewById(R.id.tv_check_out_now)
    }
}