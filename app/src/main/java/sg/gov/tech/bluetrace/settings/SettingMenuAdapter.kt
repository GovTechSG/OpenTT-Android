package sg.gov.tech.bluetrace.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData


class SettingMenuAdapter(val context: Context, childFragmentManager: FragmentManager, val versionName: String, val idType: IdentityType) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mFragManager: FragmentManager
    private var callback: FragmentCallBack? = null
    private var sectionModelArrayList = ArrayList<SectionModel>()

    private val LAYOUT_TITLE_APP_VER = 0
    private val LAYOUT_HEADER = 1
    private val LAYOUT_CHILD = 2
    private val LAYOUT_FOOTER = 3
    private var isChildClicked = false

    class TitleAppVerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val app_version_tv = itemView.findViewById<AppCompatTextView>(R.id.app_version_tv)
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuTitle = itemView.findViewById<AppCompatTextView>(R.id.menu_title_tv)
    }
    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuListContainer = itemView.findViewById<LinearLayout>(R.id.list_item_container)
        val GDSLogo = itemView.findViewById<AppCompatImageView>(R.id.gds_logo_iv)
    }

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuTitle = itemView.findViewById<AppCompatTextView>(R.id.menu_title_tv)
        val newImg = itemView.findViewById<AppCompatImageView>(R.id.new_img)
        val rootContainer = itemView.findViewById<ConstraintLayout>(R.id.root_cl)
    }

    init {
        mFragManager = childFragmentManager
        initSectionsList()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            LAYOUT_TITLE_APP_VER -> {
                TitleAppVerViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.setting_menu_title_app_version_list_item, parent, false)
                )
            }
            LAYOUT_HEADER -> {
                HeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.setting_menu_header_list_item, parent, false)
                )
            }
            LAYOUT_FOOTER -> {
                FooterViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.more_list_footer, parent, false)
                )
            }
            else -> {
                MenuViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.setting_menu_list_item, parent, false)
                )
            }
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return sectionModelArrayList.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType)
        {
            LAYOUT_TITLE_APP_VER -> {
                val titleAppVerHolder: TitleAppVerViewHolder = holder as TitleAppVerViewHolder
                titleAppVerHolder.app_version_tv.text = versionName
            }

            LAYOUT_HEADER -> {
                val headerHolder: HeaderViewHolder = holder as HeaderViewHolder
                headerHolder.menuTitle.text = sectionModelArrayList.get(position).sectionLabel
            }

            LAYOUT_CHILD -> {
                val childHolder: MenuViewHolder = holder as MenuViewHolder
                val context = childHolder.menuTitle.context

                childHolder.menuTitle.text = sectionModelArrayList.get(position).sectionLabel

                if(childHolder.menuTitle.text == context.getString(R.string.manage_family_members))
                {
                    if (Preference.isManageFamilyMemNew(context))
                        childHolder.newImg.visibility = View.VISIBLE
                    else
                        childHolder.newImg.visibility = View.GONE
                }
                if(childHolder.menuTitle.text == context.getString(R.string.submit_error_logs)){
                    if (Preference.isSubmitErrorLogsNew(context))
                        childHolder.newImg.visibility = View.VISIBLE
                    else
                        childHolder.newImg.visibility = View.GONE
                }

                childHolder.rootContainer.setOnClickListener {
                    if(!isChildClicked){
                        isChildClicked = true
                        callback?.onNextClicked(position, sectionModelArrayList[position].sectionLabel)
                    }
                }
            }
            LAYOUT_FOOTER -> {
                val footerHolder: FooterViewHolder = holder as FooterViewHolder
                footerHolder.GDSLogo.setOnClickListener {
                    callback?.onNextClicked(position, "Logo")
                }
                if(sectionModelArrayList.get(position).titleList != null){
                    sectionModelArrayList.get(position).titleList!!.forEach { label ->
                        var inflater = LayoutInflater.from(context)
                        var inflatedLayout = inflater.inflate(R.layout.manu_list_item_layout, null, false)
                        val menuTitle = inflatedLayout.findViewById<AppCompatTextView>(R.id.footer_title)
                        menuTitle.text = label
                        footerHolder.menuListContainer.addView(inflatedLayout)
                        menuTitle.setOnClickListener {
                            callback?.onNextClicked(position, label)
                        }
                    }
                }
            }
        }
    }

    fun setCallBackListener(callback: FragmentCallBack) {
        this.callback = callback
    }

    override fun getItemViewType(position: Int): Int {
        return if (sectionModelArrayList.get(position).isTitle) {
            LAYOUT_TITLE_APP_VER
        } else if (sectionModelArrayList.get(position).isHeader) {
            LAYOUT_HEADER
        } else if (sectionModelArrayList.get(position).isFooter) {
            LAYOUT_FOOTER
        } else {
            LAYOUT_CHILD
        }
    }

    interface FragmentCallBack {
        fun onNextClicked(position: Int, sectionLabel: String)
    }

    private fun initSectionsList() {
        val actList = if (RegisterUserData.isInvalidPassportOrInvalidUser(idType.tag))
            context.resources.getStringArray(R.array.settings_account_menu_array_invalid_passport_user)
        else
            context.resources.getStringArray(R.array.settings_account_menu_array)

        val helpList = context.resources.getStringArray(R.array.settings_help_menu_array)

        val otherList  = context.resources.getStringArray(R.array.settings_others_menu_array)

        val isTitle = true
        val isHeader = true

        sectionModelArrayList.add(SectionModel(context.resources.getString(R.string.setting_hello), isTitle, !isHeader))

        sectionModelArrayList.add(SectionModel(context.resources.getString(R.string.title_account), !isTitle, isHeader))
        actList.forEach {
                sectionModelArrayList.add(SectionModel(it))
        }

        sectionModelArrayList.add(SectionModel(context.resources.getString(R.string.title_help_feedback), !isTitle, isHeader))
        helpList.forEach {
            sectionModelArrayList.add(SectionModel(it))
        }
        sectionModelArrayList.add(SectionModel(context.resources.getString(R.string.txt_others),!isTitle,!isHeader,true,otherList))

    }
    fun resetItemClickFlag(){
        isChildClicked = false
    }
}
