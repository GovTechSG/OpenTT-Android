package sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_empty_family_members.*
import sg.gov.tech.bluetrace.R

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class EmptyFamilyMembersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_empty_family_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        add_family_members_button.setOnClickListener {
            navigateToAddFamilyMembers()
        }
    }

    private fun navigateToAddFamilyMembers() {
        findNavController().navigate(
            R.id.action_fragmentEmptyFamilyMembers_to_fragmentAddFamilyMembers
        )
    }
}