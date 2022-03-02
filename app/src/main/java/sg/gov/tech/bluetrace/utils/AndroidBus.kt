package sg.gov.tech.bluetrace.utils

import io.reactivex.subjects.BehaviorSubject
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.safeentry.selfcheck.model.HealthStatus

class AndroidBus {
    companion object {
        val behaviorSubject = BehaviorSubject.create<ArrayList<QrResultDataModel>>()
        val familyMembersList = BehaviorSubject.create<ArrayList<FamilyMembersRecord>>()
        val healthStatus = BehaviorSubject.create<HealthStatus>()
    }
}