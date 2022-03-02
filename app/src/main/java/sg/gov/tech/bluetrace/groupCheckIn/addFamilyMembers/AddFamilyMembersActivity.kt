package sg.gov.tech.bluetrace.groupCheckIn.addFamilyMembers

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_add_family_members.*
import sg.gov.tech.bluetrace.R

class AddFamilyMembersActivity : AppCompatActivity() {

    private var isAddFamilyMember = false
    private lateinit var navHostFragment: NavHostFragment
    private var disposable: Disposable? = null
    var misBackEnable: Boolean = true

    companion object {
        const val ADD_FAMILY_MEMBERS = "ADD_FAMILY_MEMBERS"
        const val ADD_FAMILY_MEMBERS_RESULT_CODE = 50505
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_family_members)

        intent.extras?.let {
            isAddFamilyMember = it.getBoolean(ADD_FAMILY_MEMBERS)
            setNavigationDestination()
        }

        back_btn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setNavigationDestination() {
        navHostFragment = add_family_members_navigation_host as NavHostFragment
        val graph =
            navHostFragment.navController.navInflater.inflate(R.navigation.add_family_members_navigation)
        val bundle = Bundle()
        when {
            isAddFamilyMember -> {
                graph.startDestination = R.id.addFamilyMembersFragment
            }
            else -> {
                graph.startDestination = R.id.emptyFamilyMembersFragment
            }
        }
        navHostFragment.navController.setGraph(graph, bundle)
    }

    fun setLoadingEnable(isLoading: Boolean) {
        setLoading(isLoading)
        misBackEnable = !isLoading
    }

    fun setLoading(show: Boolean) {
        if (show) {
            view_loading.visibility = View.VISIBLE
        } else {
            view_loading.visibility = View.INVISIBLE
        }
    }

    override fun onBackPressed() {
        if (!misBackEnable)
            return
        else
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}