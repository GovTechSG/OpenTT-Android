package sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers

import android.app.Activity.RESULT_OK
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.android.synthetic.main.fragment_add_family_members.*
import kotlinx.android.synthetic.main.language_item.view.*
import kotlinx.coroutines.*
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.extentions.afterTextChangedListener
import sg.gov.tech.bluetrace.revamp.utils.Cause
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel



/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class AddFamilyMembersFragment : Fragment() {

    private val viewModel: AddMemberViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_family_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getFamilyMembers(requireContext())
        viewModel.checksIsRegisterEnable.observe(viewLifecycleOwner, Observer { hash ->
            activity?.let { act ->
                if (hash.size == 2 && !hash.containsValue(false)) {
                    btn_add.isEnabled = true
                    btn_add.setTextColor(Color.WHITE)
                } else {
                    btn_add.isEnabled = false
                    btn_add.setTextColor(ContextCompat.getColor(act, R.color.unselected_text))
                }
            }
        })

        members_nric.afterTextChangedListener {
            showHideFinError(
                viewModel.checkNRIC(
                    requireContext(),
                    it
                )
            )
        }

        members_nick_name.afterTextChangedListener {
            viewModel.checkNickName(it)
        }

        btn_add.setOnClickListener {
            addMember()
        }

        if (BuildConfig.DEBUG) {
            members_nric.setText("G5996561")
        }
    }

    private fun addMember() {
        (activity as AddFamilyMembersActivity?)?.setLoadingEnable(true)
        viewModel.viewModelScope.launch (Dispatchers.IO) {
            viewModel.addFamilyMember(
                requireContext(),
                getNRICString(),
                getNickNameString()
            )
            activity?.setResult(RESULT_OK)
            activity?.finish()
        }
    }

    private fun getNRICString(): String {
        return members_nric.text?.trim().toString()
    }

    private fun getNickNameString(): String {
        return members_nick_name.text?.trim().toString()
    }

    private fun showHideFinError(model: IDValidationModel) {
        when {
            (model.cause == Cause.INCOMPLETE) ->{
                members_nric.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                nick_name_root.visibility = View.INVISIBLE
                members_nick_name.setText("")
            }

            (model.cause == Cause.PARTIAL_VALID) ->{
                members_nric.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                fin_error.visibility = View.GONE
                nick_name_root.visibility = View.INVISIBLE
                members_nick_name.setText("")
            }
            (model.isValid) -> {
                fin_error.visibility = View.GONE
                val img = ContextCompat.getDrawable(requireContext(), R.drawable.correct_nric)
                members_nric.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null)
                nick_name_root.visibility = View.VISIBLE
            }
            else -> {
                nick_name_root.visibility = View.INVISIBLE
                members_nick_name.setText("")
                fin_error.visibility = View.VISIBLE
                members_nric.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                when (model.cause) {
                    Cause.INVALID_FIN -> fin_error.text =
                        getString(R.string.invalid_nric_fin)
                    Cause.INVALID_FORMAT -> fin_error.text =
                        getString(R.string.invalid_nric_format)
                    Cause.ALREADY_ADDED -> fin_error.text =
                        getString(R.string.already_exists)
                    Cause.UNHANDLED_ERROR -> fin_error.text =
                        getString(R.string.invalid_fin)
                }
            }
        }
    }
}
