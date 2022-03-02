package sg.gov.tech.bluetrace.revamp.di.modules


import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import sg.gov.tech.bluetrace.ErrorHandler
import sg.gov.tech.bluetrace.api.ApiResponseHandler
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.api.ApiRepo
import sg.gov.tech.bluetrace.revamp.api.IApiRepository
import sg.gov.tech.bluetrace.revamp.utils.NRICValidator
import sg.gov.tech.bluetrace.revamp.utils.PassportNumberValidator
import sg.gov.tech.bluetrace.revamp.utils.PhoneNumberValidator
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.revamp.utils.FieldValidationsV2


val apiModule = module {
    single<IApiRepository> { return@single ApiRepo() }
    single { ApiHandler(get(), get()) }
    single { ApiResponseHandler() }
}

val utilsModule = module {
    single { FieldValidationsV2() }
    single { NRICValidator() }
    single { PassportNumberValidator() }
    single { PhoneNumberValidator() }
    factory { (context: Context) -> ErrorHandler(context) }
    factory { TTAlertBuilder() }
}


val dbModule = module {
    single { StreetPassRecordDatabase.getDatabase(androidContext()) }
    factory { get<StreetPassRecordDatabase>().favouriteDao() }
    factory { get<StreetPassRecordDatabase>().safeEntryDao() }
    factory { get<StreetPassRecordDatabase>().recordDao() }
}
