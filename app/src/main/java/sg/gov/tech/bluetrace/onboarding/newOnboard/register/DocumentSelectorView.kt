package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.view_document.view.*
import sg.gov.tech.bluetrace.R

class DocumentSelectorView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private var textNeutral: String = ""
    private var textSelected: String = ""
    private var textUnselected: String = ""

    private var subTextNeutral: String = ""
    private var subTextSelected: String = ""
    private var subTextUnSelected: String = ""

    private var drawableNeutral: Int = 0
    private var drawableSelected: Int = 0
    private var drawableUnselected: Int = 0

    private var textColorNeutral: Int = 0
    private var textColorSelected: Int = 0
    private var textColorUnselected: Int = 0

    private var backgroundNeutral: Int = 0
    private var backgroundSelected: Int = 0
    private var backgroundUnselected: Int = 0

    private var prepareImage: Int = 0
    private var prepareText: String = ""
    private var subTextVisibility = false

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_document, this, false)
        addView(view)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DocumentSelectorView,
            0, 0
        ).apply {

            try {

                textNeutral = getString(R.styleable.DocumentSelectorView_stringMsgNeutral) ?: ""

                textSelected = getString(R.styleable.DocumentSelectorView_stringMsgSelected) ?: ""

                textUnselected =
                    getString(R.styleable.DocumentSelectorView_stringMsgUnselected) ?: ""

                subTextNeutral = getString(R.styleable.DocumentSelectorView_stringSubMsgNeutral) ?: ""

                subTextSelected = getString(R.styleable.DocumentSelectorView_stringSubMsgSelected) ?: ""

                subTextUnSelected =
                    getString(R.styleable.DocumentSelectorView_stringSubMsgUnselected) ?: ""

                drawableNeutral = getResourceId(R.styleable.DocumentSelectorView_imageNeutral, 0)

                drawableSelected = getResourceId(R.styleable.DocumentSelectorView_imageSelected, 0)

                drawableUnselected =
                    getResourceId(R.styleable.DocumentSelectorView_imageUnselected, 0)

                textColorNeutral = getColor(R.styleable.DocumentSelectorView_textColorNeutral, 0)

                textColorSelected = getColor(R.styleable.DocumentSelectorView_textColorSelected, 0)

                textColorUnselected =
                    getColor(R.styleable.DocumentSelectorView_textColorUnselected, 0)

                backgroundNeutral = getColor(R.styleable.DocumentSelectorView_backgroundNeutral, 0)

                backgroundSelected =
                    getColor(R.styleable.DocumentSelectorView_backgroundSelected, 0)

                backgroundUnselected =
                    getColor(R.styleable.DocumentSelectorView_backgroundUnselected, 0)

                prepareImage = getResourceId(R.styleable.DocumentSelectorView_prepareImage, 0)

                prepareText = getString(R.styleable.DocumentSelectorView_prepareText) ?: ""

                subTextVisibility = getBoolean(R.styleable.DocumentSelectorView_subTextVisibility, false)
                if(subTextVisibility){
                    subText.visibility = View.VISIBLE
                }
                else{
                    subText.visibility = View.GONE
                }

                setNeutral()

                img_id_card.setImageResource(prepareImage)
                prepare_your_id.text = prepareText
            } finally {
                recycle()
            }
        }
    }

    fun setNeutral() {
        isSelected = false
        text.text = HtmlCompat.fromHtml(
            textNeutral,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        text.setTextColor(textColorNeutral)
        subText.text = HtmlCompat.fromHtml(
            subTextNeutral,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        subText.setTextColor(textColorNeutral)
        card.setCardBackgroundColor(backgroundNeutral)
        img.setImageResource(drawableNeutral)
        prepare_document.visibility = View.GONE
    }

    fun setSelected() {
        isSelected = true
        text.text = HtmlCompat.fromHtml(
            textSelected,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        text.setTextColor(textColorSelected)
        subText.text = HtmlCompat.fromHtml(
            subTextSelected,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        subText.setTextColor(textColorSelected)
        card.setCardBackgroundColor(backgroundSelected)
        img.setImageResource(drawableSelected)

        prepare_document.visibility = View.VISIBLE
    }

    fun setUnselected() {
        isSelected = false
        text.text = HtmlCompat.fromHtml(
            textUnselected,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        subText.text = HtmlCompat.fromHtml(
            subTextUnSelected,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        subText.setTextColor(textColorUnselected)
        text.setTextColor(textColorUnselected)
        card.setCardBackgroundColor(backgroundUnselected)
        img.setImageResource(drawableUnselected)

        prepare_document.visibility = View.GONE
    }

    override fun setOnClickListener(l: OnClickListener?) {
        card.setOnClickListener(l)
    }
}
