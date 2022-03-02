package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import sg.gov.tech.bluetrace.R

abstract class DialogWithCloseFragment : DialogFragment() {

    var dismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.dialog_with_close, container, false)
//        content.addView()
        val content = rootView.findViewById<ViewGroup>(R.id.content)
        val inflated = inflater.inflate(getLayoutId(), null, false)
        content.addView(inflated)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCloseBtn(view)
    }

    open fun setupCloseBtn(rootView: View) {
        rootView.findViewById<View>(R.id.close).setOnClickListener {
            dialog?.dismiss()
        }

        val btnOk = rootView.findViewById<Button>(R.id.btn_ok)
        btnOk?.setOnClickListener {
            dialog?.dismiss()
        }
    }

    abstract fun getLayoutId(): Int

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.onDismiss(dialog)
    }
}
