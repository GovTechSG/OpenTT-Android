package sg.gov.tech.bluetrace.streetpass.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordRepository

class RecordViewModel(app: Application) : AndroidViewModel(app) {

    private var repo: StreetPassRecordRepository

    var allRecords: LiveData<List<StreetPassRecord>>

    var streetPassLiteRecords: LiveData<List<StreetPassLiteRecord>>

    init {
        val recordDao = StreetPassRecordDatabase.getDatabase(app).recordDao()
        repo = StreetPassRecordRepository(recordDao)
        allRecords = repo.allRecords
        streetPassLiteRecords =
            StreetPassRecordDatabase.getDatabase(app).bleRecordDao().getRecords()

    }


}
