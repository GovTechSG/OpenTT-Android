package sg.gov.tech.bluetrace.debugger

import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_plot.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.fragment.ExportData
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.status.persistence.StatusRecordStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecordLiteStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordStorage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator

class PlotActivity : AppCompatActivity() {
    private var TAG = "PlotActivity"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_plot)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        val displayTimePeriod = intent.getIntExtra("time_period", 1) // in hours
        val type = intent.getStringExtra("type")

        val observableStreetRecords = Observable.create<List<StreetPassRecord>> {
            val result = StreetPassRecordStorage(this).getAllRecords()
            it.onNext(result)
        }

        val observableStreetPassLiteRecords = Observable.create<List<StreetPassLiteRecord>> {
            val result = StreetPassLiteRecordLiteStorage(this).getAllRecords()
            it.onNext(result)
        }

        val observableStatusRecords = Observable.create<List<StatusRecord>> {
            val result = StatusRecordStorage(this).getAllRecords()
            it.onNext(result)
        }

        val observableStringRecords = Observable.create<List<String>> {
            val result = ArrayList<String>()
            it.onNext(result)
        }

        val observable = Observable.zip(observableStreetRecords,
            observableStreetPassLiteRecords,
            observableStatusRecords,
            observableStringRecords,
            Function4<List<StreetPassRecord>, List<StreetPassLiteRecord>, List<StatusRecord>, List<String>, ExportData> { records, recordsLite, status, strings ->
                ExportData(
                    records,
                    recordsLite,
                    status,
                    strings
                )
            }
        )

        val zipResult =
            observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { exportedData ->

                    var listToUse = exportedData.recordList

                    if (type.equals("btlite")) {
                        listToUse = exportedData.recordLiteList.map {
                            var decoded = Base64.decode(it.msg, Base64.DEFAULT)
                            var modelP = decoded[17].toString() + decoded[16].toString()

                            var spr = StreetPassRecord(
                                decoded[19].toInt(),
                                it.msg,
                                "GovTech",
                                modelP,
                                TracerApp.asCentralDevice().modelC,
                                it.rssi,
                                it.txPower
                            )
                            spr.timestamp = it.timestamp
                            spr
                        }
                    }

                    if (listToUse.isEmpty()) {
                        return@subscribe
                    }

                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                    // Use the date of the last record as the end time (Epoch time in seconds)
                    val endTime =
                        listToUse.sortedByDescending { it.timestamp }[0].timestamp / 1000 + 1 * 60
                    val endTimeString = dateFormatter.format(Date(endTime * 1000))

                    val startTime =
                        endTime - displayTimePeriod * 3600 // ignore records older than X hour(s)
                    val startTimeString = dateFormatter.format(Date(startTime * 1000))

                    val filteredRecords = listToUse.filter {
                        it.timestamp / 1000 >= startTime && it.timestamp / 1000 <= endTime
                    }


                    if (filteredRecords.isNotEmpty()) {
                        val dataByModelC = filteredRecords.groupBy { it.modelC }
                        val dataByModelP = filteredRecords.groupBy { it.modelP }

                        // get all models
                        val allModelList = dataByModelC.keys union dataByModelP.keys.toList()
//                        CentralLog.d(TAG, "allModels: ${allModelList}")

                        // sort the list by the models that appear the most frequently
                        val sortedModelList =
                            allModelList.sortedWith(Comparator { a: String, b: String ->
                                val aSize =
                                    (dataByModelC[a]?.size ?: 0) + (dataByModelP[a]?.size ?: 0)
                                val bSize =
                                    (dataByModelC[b]?.size ?: 0) + (dataByModelP[b]?.size ?: 0)

                                bSize - aSize
                            })

                        // for each model form the data for that model
                        // e.g.:
                        //    var data1 = [];
                        //    var data1a = {
                        //        name: 'central',
                        //        x: ["2020-02-20 13:49"],
                        //        y: [-97],
                        //        xaxis: 'x1',
                        //        yaxis: 'y1',
                        //        mode: 'markers',
                        //        type: 'scatter',
                        //        line: {color: 'blue'}
                        //    };
                        //    data1 = data1.concat(data1a);
                        //    var data1b = {
                        //        name: 'peripheral',
                        //        x: ["2020-02-20 13:49", "2020-02-20 13:50", "2020-02-20 13:51", "2020-02-20 13:51", "2020-02-20 13:52", "2020-02-20 13:53", "2020-02-20 13:53", "2020-02-20 13:53"],
                        //        y: [-91, -94, -91, -98, -93, -101, -101, -97],
                        //        xaxis: 'x1',
                        //        yaxis: 'y1',
                        //        mode: 'markers',
                        //        type: 'scatter',
                        //        line: {color: 'red'}
                        //    };
                        //    data1 = data1.concat(data1b);
                        //
                        val individualData = sortedModelList.map { model ->
                            val index = sortedModelList.indexOf(model) + 1

                            val hasC = dataByModelC.containsKey(model)
                            val hasP = dataByModelP.containsKey(model)

                            val x1 = dataByModelC[model]?.map {
                                dateFormatter.format(Date(it.timestamp))
                            }?.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")

                            val y1 = dataByModelC[model]?.map { it.rssi }
                                ?.joinToString(separator = ", ", prefix = "[", postfix = "]")

                            val x2 = dataByModelP[model]?.map {
                                dateFormatter.format(Date(it.timestamp))
                            }?.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")

                            val y2 = dataByModelP[model]?.map { it.rssi }
                                ?.joinToString(separator = ", ", prefix = "[", postfix = "]")

                            val dataHead = "var data${index} = [];"

                            val dataA = if (!hasC) "" else """
                            var data${index}a = {
                              name: 'central',
                              x: ${x1},
                              y: ${y1},
                              xaxis: 'x${index}',
                              yaxis: 'y${index}',
                              mode: 'markers',
                              type: 'scatter',
                              line: {color: 'blue'}
                            };
                            data${index} = data${index}.concat(data${index}a);
                        """.trimIndent()

                            val dataB = if (!hasP) "" else """
                            var data${index}b = {
                              name: 'peripheral',
                              x: ${x2},
                              y: ${y2},
                              xaxis: 'x${index}',
                              yaxis: 'y${index}',
                              mode: 'markers',
                              type: 'scatter',
                              line: {color: 'red'}
                            };
                            data${index} = data${index}.concat(data${index}b);
                        """.trimIndent()

                            val data = dataHead + dataA + dataB

                            data

                        }.joinToString(separator = "\n")

                        val top = 20

                        // Combine data of all the models
                        // e.g.
                        //    var data = [];
                        //    data = data.concat(data1);
                        //    data = data.concat(data2);
                        //    data = data.concat(data3);
                        //    data = data.concat(data4);
                        //    data = data.concat(data5);
                        //    data = data.concat(data6);
                        //    data = data.concat(data7);
                        //
                        val combinedData = sortedModelList.map { model ->
                            val index = sortedModelList.indexOf(model) + 1
                            if (index < top) """
                            data = data.concat(data${index});
                        """.trimIndent() else ""
                        }.joinToString(separator = "\n")

                        // Get definition for all xAxes
                        // e.g.
                        //    xaxis1: {
                        //        type: 'date',
                        //        tickformat: '%H:%M',
                        //        range: ['2020-02-20 13:00', '2020-02-20 14:00'],
                        //        dtick: 5 * 60 * 1000
                        //    },
                        //    xaxis2: {
                        //        type: 'date',
                        //        tickformat: '%H:%M',
                        //        range: ['2020-02-20 13:00', '2020-02-20 14:00'],
                        //        dtick: 5 * 60 * 1000
                        //    }
                        //
                        val xAxis = sortedModelList.map { model ->
                            val index = sortedModelList.indexOf(model) + 1
                            if (index < top) """
                                  xaxis${index}: {
                                    type: 'date',
                                    tickformat: '%H:%M:%S',
                                    range: ['${startTimeString}', '${endTimeString}'],
                                    dtick: ${displayTimePeriod * 5} * 60 * 1000
                                  }
                        """.trimIndent() else ""
                        }.joinToString(separator = ",\n")

                        // Get definition for all xAxes
                        // e.g.
                        //    yaxis1: {
                        //        range: [-100, -30],
                        //        ticks: 'outside',
                        //        dtick: 10,
                        //        title: {
                        //            text: "SM-N960F"
                        //        }
                        //    },
                        //    yaxis2: {
                        //        range: [-100, -30],
                        //        ticks: 'outside',
                        //        dtick: 10,
                        //        title: {
                        //            text: "POCOPHONE F1"
                        //        }
                        //    }
                        //
                        val yAxis = sortedModelList.map { model ->
                            val index = sortedModelList.indexOf(model) + 1
                            if (index < top) """
                                  yaxis${index}: {
                                    range: [-100, -30],
                                    ticks: 'outside',
                                    dtick: 10,
                                    title: {
                                      text: "${model}"
                                    }
                                  }
                        """.trimIndent() else ""
                        }.joinToString(separator = ",\n")

                        // Form the complete HTML
                        val customHtml = """
                        <head>
                            <script src='https://cdn.plot.ly/plotly-latest.min.js'></script>
                        </head>
                        <body>
                            <div id='myDiv'></div>
                            <script>
                                ${individualData}
                                
                                var data = [];
                                ${combinedData}
                                
                                var layout = {
                                  title: 'Activities from <b>${startTimeString.substring(11..15)}</b> to <b>${endTimeString.substring(
                            11..15
                        )}</b>   <span style="color:blue">central</span> <span style="color:red">peripheral</span>',
                                  height: 135 * ${allModelList.size},
                                  showlegend: false,
                                  grid: {rows: ${allModelList.size}, columns: 1, pattern: 'independent'},
                                  margin: {
                                    t: 30,
                                    r: 30,
                                    b: 20,
                                    l: 50,
                                    pad: 0
                                  },
                                  ${xAxis},
                                  ${yAxis}
                                };
                                
                                var config = {
                                    responsive: true, 
                                    displayModeBar: false, 
                                    displaylogo: false, 
                                    modeBarButtonsToRemove: ['toImage', 'sendDataToCloud', 'editInChartStudio', 'zoom2d', 'select2d', 'pan2d', 'lasso2d', 'autoScale2d', 'resetScale2d', 'zoomIn2d', 'zoomOut2d', 'hoverClosestCartesian', 'hoverCompareCartesian', 'toggleHover', 'toggleSpikelines']
                                }
                                
                                Plotly.newPlot('myDiv', data, layout, config);
                            </script>
                        </body>
                    """.trimIndent()

//                        CentralLog.d(TAG, "customHtml: ${customHtml}")
                        webView.loadData(customHtml, "text/html", "UTF-8")
                    } else {
                        webView.loadData(
                            "No data received in the last ${displayTimePeriod} hour(s) or more.",
                            "text/html",
                            "UTF-8"
                        )
                    }
                }
//        CentralLog.d(TAG, "zipResult: ${zipResult}")

        webView.loadData("Loading...", "text/html", "UTF-8")
    }
}
