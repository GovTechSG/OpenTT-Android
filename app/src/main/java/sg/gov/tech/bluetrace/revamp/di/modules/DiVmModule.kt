package sg.gov.tech.bluetrace.revamp.di.modules


import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import sg.gov.tech.bluetrace.favourite.FavouriteViewModel
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyDialogViewModel
import sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers.AddMemberViewModel
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.*
import sg.gov.tech.bluetrace.passport.PassportStatusViewModel
import sg.gov.tech.bluetrace.qrscanner.QrScannerModel
import sg.gov.tech.bluetrace.revamp.home.HomeViewModel
import sg.gov.tech.bluetrace.revamp.onboarding.OnboardingOTPViewModel
import sg.gov.tech.bluetrace.revamp.otp.OTPViewModel
import sg.gov.tech.bluetrace.revamp.register.OnboardingVerifyNumberViewModel
import sg.gov.tech.bluetrace.revamp.register.ProfileHoldingViewModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckOutViewModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInViewModel
import sg.gov.tech.bluetrace.revamp.settings.BarCodeViewModel
import sg.gov.tech.bluetrace.revamp.settings.PermissionViewModel
import sg.gov.tech.bluetrace.revamp.settings.ProfileViewModel
import sg.gov.tech.bluetrace.revamp.settings.SettingsViewModel
import sg.gov.tech.bluetrace.revamp.splash.SplashViewModel
import sg.gov.tech.bluetrace.settings.SubmitLogViewModel


val viewModelModule = module {
    viewModel { NricViewModel(get(), get()) }
    viewModel { WpViewModel(get(), get()) }
    viewModel { DpViewModel(get(), get()) }
    viewModel { LtvpViewModel(get(), get()) }
    viewModel { StpViewModel(get(), get()) }
    viewModel { PassportViewModel(get(), get()) }
    viewModel { OnboardingOTPViewModel() }
    viewModel { OnboardingVerifyNumberViewModel(get()) }
    viewModel { OTPViewModel(get()) }
    //viewModel { GetTempIDModel(get()) }
    viewModel { SafeEntryCheckInViewModel(get(), get(), get()) }
    viewModel { SubmitLogViewModel(get()) }
    viewModel { SafeEntryCheckOutViewModel(get(), get(), get()) }
    viewModel { FavouriteViewModel() }
    viewModel { QrScannerModel(get()) }
    viewModel { PassportStatusViewModel(get()) }
    viewModel { ProfileHoldingViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
	viewModel { PrivacyPolicyDialogViewModel(get()) }

    viewModel { PermissionViewModel() }
    viewModel { AddMemberViewModel() }
    viewModel { BarCodeViewModel() }
    viewModel { SplashViewModel() }
    viewModel { ProfileViewModel() }
    viewModel { SettingsViewModel() }


}
