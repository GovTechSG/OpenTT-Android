package sg.gov.tech.bluetrace.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.settings.BarcodeHeaderView
import sg.gov.tech.bluetrace.settings.OnBarcodeClick

private lateinit var header: BarcodeHeaderView

class UploadFlowControllerFragment : MainActivityFragment("UploadFlowControllerFragment") {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forusebymoh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        header = view.findViewById(R.id.barcode_header)
        header.setTitle(view.context.getString(R.string.upload_data))
        header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
//                val barcodeDialog = BarCodeDialogFragment()
//                activity?.supportFragmentManager?.let { barcodeDialog.show(it, "barcode_dialog") }
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,SafeEntryActivity.ID_FRAGMENT)
                startActivity(intent)
            }

            override fun onBackPress() {}
        })
        val childFragMan: FragmentManager = childFragmentManager
        val childFragTrans: FragmentTransaction = childFragMan.beginTransaction()
        val fragB = ForUseFragment()
        childFragTrans.add(R.id.fragment_placeholder, fragB)
        childFragTrans.addToBackStack(fragB.customTag)
        childFragTrans.commit()
    }

    fun goToUploadFragment() {
//        val parentActivity: MainActivity = activity as MainActivity
//        parentActivity.openFragment(
//            parentActivity.LAYOUT_MAIN_ID,
//            UploadPageFragment(),
//            UploadPageFragment::class.java.name,
//            0
//        )

        val frag = UploadPageFragment()
        val transaction =
            childFragmentManager.beginTransaction()
        transaction.addToBackStack(frag.customTag)
        transaction.replace(R.id.fragment_placeholder, frag, frag.customTag)
        transaction.commit()
    }


    override fun didProcessBack(): Boolean {
        if (isAdded) {
            //needs tweaking - child at top may  be able to process back
            return if (childFragmentManager.backStackEntryCount > 1) {
                childFragmentManager.popBackStackImmediate()
                true
            } else
                false
        }
        return false
    }
}
