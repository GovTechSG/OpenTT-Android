package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.os.Bundle

class AppUpdatedV2Activity : BaseActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        openFragment(
            getContainer().id,
            AppUpdatedV2Fragment(),
            ""
        )
    }

}
