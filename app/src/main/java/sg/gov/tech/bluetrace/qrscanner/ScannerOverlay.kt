package sg.gov.tech.bluetrace.qrscanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import sg.gov.tech.bluetrace.R

class ScannerOverlay : ViewGroup {
    private var left = 0f
    private var top = 0f
    private var endY = 0f
    private var rectWidth = 0
    private var rectHeight = 0
    private var frames = 0
    private var revAnimation = false
    private var lineColor = 0
    private var lineWidth = 0
    private var mContext: Context? = null

    constructor(context: Context?) : super(context)

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int = 0
    ) : super(context, attrs, defStyle) {
        mContext = context
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ScannerOverlay,
            0, 0
        )
        rectWidth = a.getInteger(
            R.styleable.ScannerOverlay_square_width,
            resources.getInteger(R.integer.scanner_rect_width)
        )
        rectHeight = a.getInteger(
            R.styleable.ScannerOverlay_square_height,
            resources.getInteger(R.integer.scanner_rect_height)
        )
        lineColor = a.getColor(
            R.styleable.ScannerOverlay_line_color,
            ContextCompat.getColor(context, R.color.scanner_line)
        )
        lineWidth = a.getInteger(
            R.styleable.ScannerOverlay_line_width,
            resources.getInteger(R.integer.line_width)
        )
        frames = a.getInteger(
            R.styleable.ScannerOverlay_line_speed,
            resources.getInteger(R.integer.line_width)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    public override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        left = (w - dpToPx(rectWidth)) / 2.toFloat()
        top = (h - dpToPx(rectHeight)) / 2.toFloat()
        endY = top
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
        /*DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));*/
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw transparent rect
        val cornerRadius = 5
        val eraser = Paint()
        eraser.isAntiAlias = true
        eraser.style = Paint.Style.FILL
        eraser.color = ContextCompat.getColor(mContext!!, R.color.scanner_white)
        eraser.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        val border = Paint()
        border.isAntiAlias = true
        border.style = Paint.Style.STROKE
        border.color = Color.WHITE
        border.strokeWidth = 5f
        border.pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
        val rect = RectF(left, top, dpToPx(rectWidth) + left, dpToPx(rectHeight) + top)
        canvas.drawRoundRect(
            rect,
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            eraser
        )
        canvas.drawRoundRect(
            rect,
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            border
        )

        // draw horizontal line
        val line = Paint()
        line.color = lineColor
        line.strokeWidth = java.lang.Float.valueOf(lineWidth.toFloat())

        // draw the line to product animation
        if (endY >= top + dpToPx(rectHeight) + frames) {
            revAnimation = true
        } else if (endY == top + frames) {
            revAnimation = false
        }

        // check if the line has reached to bottom
        if (revAnimation) {
            endY -= frames.toFloat()
        } else {
            endY += frames.toFloat()
        }
        canvas.drawLine(left, endY, left + dpToPx(rectWidth), endY, line)
        invalidate()
    }
}
