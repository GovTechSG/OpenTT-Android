package sg.gov.tech.bluetrace.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import kotlinx.android.synthetic.main.barcode_header_view.view.*
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData


class BarcodeHeaderView : LinearLayout, View.OnClickListener {
    private var headerTitle: AppCompatTextView
    private var barCode: AppCompatImageView
    private var mListener: OnBarcodeClick? = null

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    init {
        inflate(context, R.layout.barcode_header_view, this)
        headerTitle = findViewById(R.id.pageTitle)
        barCode = findViewById(R.id.barcodeIv)
        barCode.setOnClickListener(this)
        navigation_image.setOnClickListener(this)
        if (RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(context))) {
            barCode.visibility = View.INVISIBLE
        }
    }

    fun setTitle(title: String) {
        pageTitle.text = title
    }

    fun setBarcodeClickListener(eventListener: OnBarcodeClick) {
        mListener = eventListener
    }

    fun showBackNavigationImage() {
        navigation_image.visibility = View.VISIBLE
    }

    fun hideBackNavigationImage() {
        navigation_image.visibility = View.INVISIBLE
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    override fun onClick(v: View?) {
        if (v == barCode)
            mListener?.showBarCode()
        else if (v == navigation_image)
            mListener?.onBackPress()
    }
}

interface OnBarcodeClick {
    fun showBarCode()
    fun onBackPress()
}
