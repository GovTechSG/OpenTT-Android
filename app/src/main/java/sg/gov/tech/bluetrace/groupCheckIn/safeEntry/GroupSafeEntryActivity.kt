package sg.gov.tech.bluetrace.groupCheckIn.safeEntry

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_group_safe_entry.*
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.SafeEntryActivity.Companion.IS_FROM_GROUP_CHECK_IN
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddFamilyMembersActivity
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddMemberViewModel
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.utils.AndroidBus
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager
import java.util.*
import kotlin.collections.ArrayList

class FamilyMember(
    val nric: String,
    val nickName: String
)

class GroupSafeEntryActivity : AppCompatActivity(), GroupSafeEntryListAdapter.Callback {

    private val viewModel: AddMemberViewModel by viewModel()
    private var disposable: Disposable? = null
    private var familyMembersRecordList = arrayListOf<FamilyMembersRecord>()
    private lateinit var groupSafeEntryAdapter: GroupSafeEntryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_group_safe_entry)

        add_another_person_text.paintFlags =
            add_another_person_text.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        select_all_text.paintFlags =
            select_all_text.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        getFamilyMembers()
        setOnClickListeners()
    }

    private fun getFamilyMembers() {
        familyMembersRecordList.clear()
        select_all_text.text = getString(R.string.select_all)
        disposable = Observable.create<List<FamilyMembersRecord>> {
            val records = viewModel.getAllRecords(this)
            it.onNext(records)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { records ->
                if (records.isNotEmpty()) {
                    val familyMemberList = arrayListOf<FamilyMember>()
                    Preference.getEncryptedUserData(this)?.let { user ->
                        familyMembersRecordList.add(FamilyMembersRecord(user.id, user.name))
                        familyMemberList.add(FamilyMember(user.id, user.name))
                    }
                    val reversedRecords = ArrayList(records.reversed())
                    familyMembersRecordList.addAll(reversedRecords)
                    for (familyMembersRecord in reversedRecords) {
                        val familyMember =
                            FamilyMember(
                                getMaskedNRIC(familyMembersRecord.nric),
                                familyMembersRecord.nickName
                            )
                        familyMemberList.add(familyMember)
                    }
                    setAdapter(familyMemberList)
                } else {
                    // Navigate to EmptyFamilyMembers screen
                    navigateToEmptyFamilyMembers()
                }
            }
    }

    private fun setAdapter(familyMemberList: ArrayList<FamilyMember>) {
        family_members_recycler_view.layoutManager = LinearLayoutManager(this)
        family_members_recycler_view.addItemDecoration(getItemDecorator())
        groupSafeEntryAdapter =
            GroupSafeEntryListAdapter(this, familyMemberList)
        family_members_recycler_view.adapter = groupSafeEntryAdapter
        groupSafeEntryAdapter.addCallback(this)
        progress_bar_layout.visibility = View.GONE
    }

    private fun getMaskedNRIC(nric: String): String {
        val decryptedNRIC = TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC(this, nric)
        return if (decryptedNRIC != null)
            Utils.maskIdWithDot(decryptedNRIC).toUpperCase(Locale.getDefault())
        else
            ""
    }

    private fun getItemDecorator(): DividerItemDecoration {
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.divier)
            ?.let { itemDecoration.setDrawable(it) }
        return itemDecoration
    }

    private fun navigateToEmptyFamilyMembers() {
        val intent = Intent(this, AddFamilyMembersActivity::class.java)
        intent.putExtra(AddFamilyMembersActivity.ADD_FAMILY_MEMBERS, false)
        startActivityForResult(
            intent,
            AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE
        )
        overridePendingTransition(0, 0)
    }

    private fun setOnClickListeners() {

        back_btn.setOnClickListener {
            onBackPressed()
        }

        add_another_person_text.setOnClickListener {
            // Navigate to AddFamilyMembers screen
            navigateToAddFamilyMembers()
        }

        select_all_text.setOnClickListener {
            if (select_all_text.text.toString() == getString(R.string.select_all)) {
                //If Text is Select All then loop to all array List items and check all of them
                for (i in 1 until familyMembersRecordList.size)
                    groupSafeEntryAdapter.checkCheckBox(i, true)
                //After checking all items change button text
                select_all_text.text = getString(R.string.unselect_all)
            } else {
                //If button text is Deselect All remove check from all items
                groupSafeEntryAdapter.removeSelection()
                //After checking all items change button text
                select_all_text.text = getString(R.string.select_all)
            }
        }

        next_button.setOnClickListener {
            // Navigate to SafeEntry screen
            navigateToSafeEntry()
        }
    }

    private fun navigateToAddFamilyMembers() {
        val intent = Intent(this, AddFamilyMembersActivity::class.java)
        intent.putExtra(AddFamilyMembersActivity.ADD_FAMILY_MEMBERS, true)
        startActivityForResult(intent, AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE)
    }

    private fun navigateToSafeEntry() {
        val selectedFamilyMembersRecordList = arrayListOf<FamilyMembersRecord>()
        val selectedItems = groupSafeEntryAdapter.getSelectedIds()
        if (selectedItems.isNotEmpty()) {
            for (i in 0 until selectedItems.size()) {
                if (selectedItems.valueAt(i))
                    selectedFamilyMembersRecordList.add(
                        familyMembersRecordList[selectedItems.keyAt(i)]
                    )
            }
        }
       // if (selectedFamilyMembersRecordList.isNotEmpty())
            AndroidBus.familyMembersList.onNext(selectedFamilyMembersRecordList)
        val intent = Intent(this, SafeEntryActivity::class.java)
        intent.putExtra(IS_FROM_GROUP_CHECK_IN, true)
        startActivity(intent)
    }

    private fun toggleSelectAllText() {
        val selectedItems = groupSafeEntryAdapter.getSelectedIds()
        if (selectedItems.size() == familyMembersRecordList.size - 1)
            select_all_text.text = getString(R.string.unselect_all)
        else
            select_all_text.text = getString(R.string.select_all)
    }

    override fun onMemberItemClicked() {
        toggleSelectAllText()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddFamilyMembersActivity.ADD_FAMILY_MEMBERS_RESULT_CODE) {
            if (resultCode == RESULT_OK)
                getFamilyMembers()
            else if (familyMembersRecordList.size == 0)
                finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}
