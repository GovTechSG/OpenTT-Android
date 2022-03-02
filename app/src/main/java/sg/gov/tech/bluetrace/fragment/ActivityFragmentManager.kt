package sg.gov.tech.bluetrace.fragment

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

/**
 * This class to show DialogFragment in priorities.
 * Please use show() and dismiss() instead of using DialogFragment.show() or DialogFragment.dismiss().
 * Btw, this class is nothing to do with FragmentManager Android class.
 */
class ActivityFragmentManager(private val fragmentActivity: FragmentActivity) {

    companion object {
        const val EMPTY = 0
        const val QUEUED = 1
        const val MARK_TO_REMOVE = 2
    }

    // this map is to find fragment by its tag.
    // this order must be same as fragmentOrderMap.
    private val fragmentMap = linkedMapOf<String, DialogVisibility?>(
        Pair("force_update", null),
        Pair("privacy_policy", null),
        Pair("swift_med", null),
        Pair("optional_update", null),
        Pair("PDF", null),
        Pair("unhappy", null),
        Pair("showcase_SE", null)
    )

    // this map is to find order.
    // this order must be same as fragmentMap.
    private val fragmentOrderMap = linkedMapOf(
        Pair("force_update", 0),
        Pair("privacy_policy", 1),
        Pair("swift_med", 2),
        Pair("optional_update", 3),
        Pair("PDF", 4),
        Pair("unhappy", 5),
        Pair("showcase_SE", 6)
    )

    /**
     * Show the fragment or queue it for next round.
     * If the current displayed fragment has lesser priority, the new fragment will overlay on top.
     * If the current displayed fragment has more priority, will be queued until showNext() is called.
     * @param tag of the fragment.
     * @fragment instance of DialogFragment to show.
     */
    fun show(tag: String, fragment: DialogFragment) {
        val visibleFragmentTag = getTopVisibleFragmentTag()

        if (fragmentMap.containsKey(tag)) {
            fragmentMap[tag] = DialogVisibility(fragment, QUEUED)
        } else {
            return
        }

        val visibleOrder = fragmentOrderMap[visibleFragmentTag] ?: Int.MAX_VALUE
        val toBeShownOrder = fragmentOrderMap[tag] ?: Int.MAX_VALUE
        if (visibleOrder > toBeShownOrder) {        // visible less priority, overlay this with new one.
            if (!fragment.isAdded) {
                fragment.show(fragmentActivity.supportFragmentManager, tag)
            }
        }
    }

    /**
     * Show next fragment in queue.
     */
    private fun showNext() {
        val visibleFragmentTag = getTopVisibleFragmentTag()
        fragmentMap[visibleFragmentTag]?.apply {
            if (status == QUEUED) {
                if (fragment?.isAdded == false) {
                    fragment?.show(fragmentActivity.supportFragmentManager, visibleFragmentTag)
                }
            } else {        // else is MARK_TO_REMOVE
                dismissFragmentNow(this)
            }
        }
    }

    /**
     * Dismiss the fragment with the tag.
     */
    fun dismiss(tag: String) {
        val visibleFragmentTag = getTopVisibleFragmentTag()
        if (tag == visibleFragmentTag) {    //dismiss that is displayed
            fragmentMap[visibleFragmentTag]?.run {
                dismissFragmentNow(this)
            }
            showNext()
        } else {
            val visibleOrder = fragmentOrderMap[visibleFragmentTag] ?: Int.MAX_VALUE
            val tagOrder = fragmentOrderMap[tag] ?: Int.MAX_VALUE
            if (visibleFragmentTag == null || visibleOrder >= tagOrder) {
                dismissFragmentNow(fragmentMap[tag])
            } else {
                fragmentMap[tag]?.status = MARK_TO_REMOVE
            }
        }
    }

    fun dismissAll() {
        for (f in fragmentMap) {
            dismissFragmentNow(f.value)
        }
    }

    /**
     * Get the tag of the visible fragment on the top.
     */
    private fun getTopVisibleFragmentTag(): String? {
        var visibleFragmentTag: String? = null
        for (f in fragmentMap) {
            if (f.value?.status == QUEUED || f.value?.status == MARK_TO_REMOVE) {
                visibleFragmentTag = f.key
                break
            }
        }
        return visibleFragmentTag
    }

    private fun dismissFragmentNow(dialogVisibility: DialogVisibility?) {
        try {
            if (dialogVisibility?.fragment?.isAdded == true) {
                dialogVisibility.fragment?.dismiss()
            }
            dialogVisibility?.status = EMPTY
        } catch (e: Exception) {
        }
    }

    inner class DialogVisibility(var fragment: DialogFragment?, var status: Int)
}
