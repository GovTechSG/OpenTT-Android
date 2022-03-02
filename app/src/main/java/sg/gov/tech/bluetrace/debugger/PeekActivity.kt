package sg.gov.tech.bluetrace.debugger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_pager.*
import sg.gov.tech.bluetrace.R

class PeekActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager)

        nav_view.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bluetrace -> {
                    openFragment(StreetPassPeekFrag())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.bluetrace_lite -> {
                    openFragment(StreetPassLitePeekFrag())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.logs -> {
                    openFragment(CentralProdLogPeekFrag())
                    return@setOnNavigationItemSelectedListener true
                }

                else -> {
                    return@setOnNavigationItemSelectedListener false
                }
            }
        }

        nav_view.selectedItemId = R.id.bluetrace
    }

    private fun openFragment(fragment: Fragment) {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.content, fragment, "tagger")
            transaction.commit()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
