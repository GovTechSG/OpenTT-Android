package sg.gov.tech.bluetrace.database.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LiveDataTestUtil {

    @Throws(InterruptedException::class)
    fun <T> getValue(liveData: LiveData<T>): T? {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer = object : Observer<T?> {
            override fun onChanged(o: T?) {
                data[0] = o
                latch.countDown()
                liveData.removeObserver(this)
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            liveData.observeForever(observer)
        }
        latch.await(2, TimeUnit.SECONDS)
        return data[0] as T?
    }
}