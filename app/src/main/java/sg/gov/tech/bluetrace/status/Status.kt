package sg.gov.tech.bluetrace.status

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Status(
    val msg: String
) : Parcelable