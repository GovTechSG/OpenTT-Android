package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.os.Bundle

class OnboardExistingActivity : BaseActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        openFragment(
            getContainer().id,
            AppUpdatedFragment(),
            ""
        )
    }

}
