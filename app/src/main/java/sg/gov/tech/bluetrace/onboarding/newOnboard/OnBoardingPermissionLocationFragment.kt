package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import sg.gov.tech.bluetrace.R

class OnBoardingPermissionLocationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help_you_note_possible_exposure, container, false)
    }
}
