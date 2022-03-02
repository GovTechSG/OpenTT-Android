package sg.gov.tech.bluetrace.revamp.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.utils.State

class ProfileViewModel : ViewModel() {

    var userData: MutableLiveData<State> = MutableLiveData<State>()

    fun getUserData() {
        userData.postValue(State.loading())
        try {
            var user = Preference.getEncryptedUserData(TracerApp.AppContext)
            if (user != null) {
                userData.postValue(State.done(user))
            } else
                userData.postValue(State.error(Throwable("Undefined Result")))
        } catch (e: Exception) {
            userData.postValue(State.error(e))
        }
    }
}