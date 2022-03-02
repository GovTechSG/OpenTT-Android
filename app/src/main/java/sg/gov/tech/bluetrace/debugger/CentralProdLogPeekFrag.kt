package sg.gov.tech.bluetrace.debugger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.central_prod_log_peek.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.logging.persistence.LogRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*

class CentralProdLogPeekFrag : Fragment() {

    private var disposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.central_prod_log_peek, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getLogRecords()
    }

    private fun getLogRecords() {
        disposable = Observable.create<List<LogRecord>> {
            val dateCalendar = Calendar.getInstance()
            dateCalendar.add(Calendar.DATE, -2)
            dateCalendar.set(Calendar.HOUR, 0)
            dateCalendar.set(Calendar.MINUTE, 0)
            dateCalendar.set(Calendar.SECOND, 0)
            dateCalendar.set(Calendar.MILLISECOND, 0)

            val logRecords = StreetPassRecordDatabase.getDatabase(requireContext()).logRecordDao()
                .getLogRecords(dateCalendar.timeInMillis, System.currentTimeMillis())
            it.onNext(logRecords)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { logRecords ->
                progress_bar.visibility = View.GONE
                if (logRecords.isNotEmpty()) {
                    setAdapter(logRecords)
                }
            }
    }

    private fun setAdapter(logRecords: List<LogRecord>) {
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration =
            DividerItemDecoration(recyclerView.context, layoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)
        val adapter = LogListAdapter(requireContext(), logRecords)
        recyclerView.adapter = adapter
        setSearchFilter(adapter)
    }

    private fun setSearchFilter(adapter: LogListAdapter) {
        search_edit_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // filter recycler view when text is changed
                adapter.filter.filter(s?.trim()) {
                    if (adapter.itemCount > 0) {
                        recyclerView.visibility = View.VISIBLE
                        no_log_found_text.visibility = View.GONE
                    } else {
                        recyclerView.visibility = View.GONE
                        no_log_found_text.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}