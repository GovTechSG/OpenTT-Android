package sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.revamp.utils.Cause
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel
import sg.gov.tech.bluetrace.revamp.utils.NRICValidator
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager.getEncryptedFamilyMemberNRIC
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set


class AddMemberViewModel : ViewModel() {

    private var mapEnable = HashMap<String, Boolean>()
    var checksIsRegisterEnable = MutableLiveData<HashMap<String, Boolean>>()
    var myFamilyMember = ArrayList<FamilyMembersRecord>()

    fun checkNRIC(context: Context, nric: String): IDValidationModel {
        val result = isValidNRIC(context, nric)
        mapEnable["NRIC"] = result.isValid
        checksIsRegisterEnable.value = mapEnable
        return result
    }

    private fun isValidNRIC(context: Context, nric: String): IDValidationModel {
         var nricValidator =  NRICValidator()
        val result = when{
            (nric.length == 1) ->
                 nricValidator.isValidCharacterToAddMember(nric )
            (nric.length >= 9) ->
                 nricValidator.isValidHash(nric)
            else -> return IDValidationModel(
                false,
               Cause.INCOMPLETE
            )
        }
       // val result = NRICValidator().isValid(nric)
        if (result.isValid) {
            val members = myFamilyMember.filter {
                it.nric.toUpperCase(Locale.getDefault()) == nric.toUpperCase(Locale.getDefault())
            }
            if (members.isNotEmpty())
                return IDValidationModel(false, Cause.ALREADY_ADDED)
            else {
                Preference.getEncryptedUserData(context)?.let { user ->
                    if (user.id.toUpperCase(Locale.getDefault()) == nric.toUpperCase(Locale.getDefault()))
                        return IDValidationModel(false, Cause.ALREADY_ADDED)
                }
            }
        }
        return result
    }

    fun checkNickName(name: String): Boolean {
        val isValid = name.isNotEmpty()
        mapEnable["Nick"] = isValid
        checksIsRegisterEnable.value = mapEnable
        return isValid
    }

    @SuppressLint("CheckResult")
    fun getFamilyMembers(context: Context) {
        Observable.create<List<FamilyMembersRecord>> {
            val members =
                StreetPassRecordDatabase.getDatabase(context).familyMemberDao().getAllMembers()
            it.onNext(members)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { memberRecords ->
                myFamilyMember = memberRecords as ArrayList<FamilyMembersRecord>
                myFamilyMember.forEach {
                    it.nric = getDecryptedFamilyMemberNRIC(context, it.nric)?: it.nric
                }
            }
    }

    suspend fun addFamilyMember(context: Context, nric: String, nickname: String) {

        val encryptedNric = getEncryptedFamilyMemberNRIC(context, nric)
        if(encryptedNric != null) {
            val member = FamilyMembersRecord(
                nric = encryptedNric,
                nickName = nickname
            )
            StreetPassRecordDatabase.getDatabase(context).familyMemberDao().insert(member)
        }
    }

    fun getAllRecords(context: Context): List<FamilyMembersRecord> {
        return StreetPassRecordDatabase.getDatabase(context).familyMemberDao().getAllMembers()
    }

    fun deleteRecord(context: Context, nric: String) {
        return StreetPassRecordDatabase.getDatabase(context).familyMemberDao()
            .removeFamilyMember(nric)
    }
}
