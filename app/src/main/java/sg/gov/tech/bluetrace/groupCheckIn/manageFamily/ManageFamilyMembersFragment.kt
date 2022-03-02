package sg.gov.tech.bluetrace.groupCheckIn.manageFamily

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_manage_family_members.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddFamilyMembersActivity
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddMemberViewModel
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.settings.OnBarcodeClick
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord

class ManageFamilyMembersFragment : MainActivityFragment("ManageFamilyMembersFragment"),
    ManageFamilyMemberListAdapter.Callback {

    private val viewModel: AddMemberViewModel by viewModel()
    private var familyMembersRecordList = arrayListOf<FamilyMembersRecord>()
    private lateinit var familyMemberListAdapter: ManageFamilyMemberListAdapter
    private val disposables: MutableList<Disposable> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_family_members, container, false)
    }

    override fun didProcessBack(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barcode_header.setTitle(getString(R.string.manage_family_members))
        barcode_header.showBackNavigationImage()
        barcode_header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,SafeEntryActivity.ID_FRAGMENT)
                startActivity(intent)
            }

            override fun onBackPress() {
                (activity as MainActivity).onBackPressed()
            }
        })

        progress_bar_layout.visibility = View.VISIBLE
        add_another_person_text.paintFlags =
            add_another_person_text.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        add_another_person_text.setOnClickListener {
            navigateToAddFamilyMembers(true)
        }

        getFamilyMembers()
    }

    private fun getFamilyMembers() {
        disposables.add(Observable.create<List<FamilyMembersRecord>> {
            val records = viewModel.getAllRecords(requireContext())
            it.onNext(records)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { records ->
                if (records.isNotEmpty()) {
                    familyMembersRecordList = ArrayList(records.reversed())
                    setAdapter()
                } else {
                    // Navigate to EmptyAddMembers
                    navigateToAddFamilyMembers(false)
                }
            })
    }

    private fun setAdapter() {
        family_members_recycler_view.layoutManager = LinearLayoutManager(context)
        family_members_recycler_view.addItemDecoration(getItemDecorator())
        familyMemberListAdapter = ManageFamilyMemberListAdapter(
            requireContext(),
            familyMembersRecordList
        )
        family_members_recycler_view.adapter = familyMemberListAdapter
        familyMemberListAdapter.addCallback(this)
        progress_bar_layout.visibility = View.GONE
    }

    private fun getItemDecorator(): DividerItemDecoration {
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divier)
            ?.let { itemDecoration.setDrawable(it) }
        return itemDecoration
    }

    override fun onMemberRemoved(familyMembers: FamilyMembersRecord) {
        showRemoveMemberAlert(familyMembers)
    }

    private fun showRemoveMemberAlert(familyMembers: FamilyMembersRecord) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.remove_family_member_title, familyMembers.nickName))
            .setMessage(getString(R.string.remove_family_member_msg, familyMembers.nickName))
            .setPositiveButton(getString(R.string.remove)) { dialog, _ ->
                progress_bar_layout.visibility = View.VISIBLE
                deleteRecord(familyMembers)
                dialog.cancel()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun deleteRecord(familyMembers: FamilyMembersRecord) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        disposables.add(Observable.create<Boolean> {
            viewModel.deleteRecord(requireContext(), familyMembers.nric)
            it.onNext(true)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                progress_bar_layout.visibility = View.GONE
                familyMembersRecordList.remove(familyMembers)
                if (familyMembersRecordList.isEmpty())
                    navigateToAddFamilyMembers(false)
                else
                    familyMemberListAdapter.notifyDataSetChanged()
            }, {
                CentralLog.e(loggerTAG, "deleteRecord failed: ${it.message}")
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "deleteRecord failed: ${it.message}",
                    DBLogger.getStackTraceInJSONArrayString(it as Exception)
                )
            }))
    }

    private fun navigateToAddFamilyMembers(isAddFamilyMember: Boolean) {
        //Open Add Family Members screen
        val intent = Intent(requireContext(), AddFamilyMembersActivity::class.java)
        intent.putExtra(AddFamilyMembersActivity.ADD_FAMILY_MEMBERS, isAddFamilyMember)
        startActivityForResult(intent, AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE && resultCode == RESULT_OK)
            getFamilyMembers()
        else if (resultCode == RESULT_CANCELED && familyMembersRecordList.isEmpty())
            (activity as MainActivity).onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.forEach {
            it.dispose()
        }
    }
}
