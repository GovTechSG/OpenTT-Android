package sg.gov.tech.bluetrace.zendesk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import zendesk.support.CustomField
import zendesk.support.request.RequestActivity
import zendesk.support.requestlist.RequestListActivity
import java.util.*

class WebViewZendeskSupportFragment : MainActivityFragment("WebViewZendeskSupportFragment") {
    var mWebviewAck: WebView? = null
    private var mUrl: String? = null
    private var loadingFinished = true
    private var redirect = false
    private var mPbLoading: ProgressBar? = null
    private var mFAB: FloatingActionButton? = null
    private var mClose: AppCompatImageView? = null
    private var isTermPrivacy = false
    private var isFabVisibility = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_acknowledge_detail, container, false)
    }

    fun setUrl(url: String?): Fragment {
        mUrl = url
        return this
    }

    fun setIsTermPrivacy(): Fragment {
        isTermPrivacy = true
        return this
    }

    fun setFabInvisible(): Fragment {
        isFabVisibility = false
        return this
    }

    private fun createFAB() {
        mFAB = view!!.findViewById(R.id.fab_create_ticket)
        mClose = view!!.findViewById(R.id.close)

        if (isTermPrivacy) {
            mFAB?.visibility = View.INVISIBLE
            mClose?.visibility = View.VISIBLE
            mClose?.setOnClickListener {
                activity?.onBackPressed()

            }
        } else {
            if (isFabVisibility)
                mFAB?.visibility = View.VISIBLE
            else
                mFAB?.visibility = View.INVISIBLE

            mClose?.visibility = View.GONE
        }
        mFAB?.setOnClickListener {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val androidVersion = Build.VERSION.SDK_INT
            val deviceModel =
                if (model.startsWith(manufacturer)) model else "$manufacturer $model"
            var version = ""
            try {
                val pInfo = activity!!.packageManager
                    .getPackageInfo(activity!!.packageName, 0)
                version = pInfo.versionName
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            val phoneModel = CustomField(360034523134L, deviceModel)
            val AppVersion = CustomField(360034523154L, version)
            val OsVersion = CustomField(360034523174L, androidVersion.toString())
            val requestActivityConfig = RequestActivity.builder()
                .withTags("Android")
                .withCustomFields(Arrays.asList(phoneModel, AppVersion, OsVersion))
                .config()

            RequestListActivity.builder()
                .show(activity!!, requestActivityConfig)
        }

        mFAB?.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.new_accent))
    }

    override fun didProcessBack(): Boolean {
        mWebviewAck?.let {

            if (it.canGoBack()) {
                it.goBack()
                return true
            }
        }
        return false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_HELP_SUPPORT
        )

        arguments?.getString("url")?.let {
            setUrl(it)
        }

        arguments?.getBoolean("term_privacy")?.let {
            if (it)
                setIsTermPrivacy()
        }

        mWebviewAck =
            view!!.findViewById<View>(R.id.webview_acknowledgement) as WebView
        mPbLoading =
            view!!.findViewById<View>(R.id.progressBar_loading) as ProgressBar
        mWebviewAck!!.settings.javaScriptEnabled = true
        mWebviewAck!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                if (!loadingFinished) {
                    redirect = true
                }
                loadingFinished = false
                handleUrl(url, view)
                return true
            }

            override fun onPageStarted(
                view: WebView,
                url: String,
                facIcon: Bitmap?
            ) {
                loadingFinished = false
                mPbLoading!!.visibility = View.VISIBLE
                //SHOW LOADING IF IT ISN'T ALREADY VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!redirect) {
                    loadingFinished = true
                }
                if (loadingFinished && !redirect) { //HIDE LOADING IT HAS FINISHED
                    mPbLoading!!.visibility = View.GONE
                } else {
                    redirect = false
                }
            }
        }
        setDesktopMode(mWebviewAck, false)
        mWebviewAck!!.loadUrl(mUrl)
        createFAB()
    }

    private fun handleUrl(url: String, view: WebView) {
        when {
            /*url.startsWith("https://play.google.com/store/apps") -> {
                // url =  https://play.google.com/store/apps/details?id=sg.gov.tech.bluetrace
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (canHandleIntent(context, intent)) {
                    startActivity(intent)
                } else {
                    // No Intent available to handle the action
                    view.loadUrl(url)
                }
            }*/
            url.startsWith("intent") -> {
                // url = intent://play.app.goo.gl/?link=https://play.google.com/store/apps/details?id=sg.gov.tech.bluetrace
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    // fallbackUrl = https://play.google.com/store/apps/details?id=sg.gov.tech.bluetrace
                    if (!fallbackUrl.isNullOrBlank()) {
                        val fallbackUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                        if (canHandleIntent(context, fallbackUrlIntent)) {
                            startActivity(fallbackUrlIntent)
                        } else {
                            // No Intent available to handle the action
                            view.loadUrl(url)
                        }
                    }
                } catch (e: Exception) {
                    // Not an intent uri
                    view.loadUrl(url)
                }
            }
            url.startsWith("hiapplink://com.huawei.appmarket") -> {
                // url = hiapplink://com.huawei.appmarket?appid=c102170931
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (canHandleIntent(context, intent)) {
                    startActivity(intent)
                } else {
                    // No Intent available to handle the action
                    view.loadUrl(url)
                }
            }
            else -> {
                view.loadUrl(url)
            }
        }
    }

    private fun canHandleIntent(context: Context?, intent: Intent): Boolean {
        return context?.packageManager?.let { intent.resolveActivity(it) } != null
    }

    fun setDesktopMode(webView: WebView?, enabled: Boolean) {
        var newUserAgent = webView!!.settings.userAgentString
        if (enabled) {
            try {
                val ua = webView.settings.userAgentString
                val androidOSString = webView.settings.userAgentString
                    .substring(ua.indexOf("("), ua.indexOf(")") + 1)
                newUserAgent = webView.settings.userAgentString
                    .replace(androidOSString, "(X11; Linux x86_64)")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            newUserAgent = null
        }
        webView.settings.userAgentString = newUserAgent
        webView.settings.useWideViewPort = enabled
        webView.settings.loadWithOverviewMode = enabled
//        webView.settings.setAppCacheEnabled(true)
//        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.domStorageEnabled = true
        webView.reload()
    }

    override fun onStop() {
        super.onStop()
        loadLocale()
    }
}
