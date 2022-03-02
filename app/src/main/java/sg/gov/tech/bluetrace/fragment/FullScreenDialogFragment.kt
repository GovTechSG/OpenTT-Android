package sg.gov.tech.bluetrace.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import sg.gov.tech.bluetrace.R


open class FullScreenDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

//        dialog?.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val window: Window? = dialog?.window
        val windowParams: WindowManager.LayoutParams? = window?.attributes
        windowParams?.dimAmount = 0.0f

        windowParams?.let {
            window.attributes = windowParams
        }

        dialog?.window?.decorView?.systemUiVisibility =
            (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.argb(216, 0, 0, 0)))
    }
}
