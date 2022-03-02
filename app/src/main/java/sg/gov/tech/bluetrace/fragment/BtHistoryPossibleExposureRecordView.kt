package sg.gov.tech.bluetrace.fragment

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import sg.gov.tech.bluetrace.R

class BtHistoryPossibleExposureRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    var location: TextView
    var date: TextView
    var starCheckBox: AppCompatCheckBox

    init {
        val view = inflate(context, R.layout.possible_encounter_linear_layout_item, this)
        location = view.findViewById(R.id.possible_exposure_linearlayout_item_title)
        date = view.findViewById(R.id.possible_exposure_linearlayout_item_date)
        starCheckBox = view.findViewById(R.id.star_checkbox)
    }
}