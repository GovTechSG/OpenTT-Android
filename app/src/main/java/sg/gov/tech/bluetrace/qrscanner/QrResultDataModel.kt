package sg.gov.tech.bluetrace.qrscanner

import android.os.Parcel
import android.os.Parcelable

data class QrResultDataModel(
    val venueName: String?,
    val venueId: String?,
    val tenantName: String?,
    val tenantId: String?,
    val postalCode: String?,
    val address: String?,
    val id: Int?,
    val checkInTimeMS: Long?,
    val groupMembersCount: Int?,
    val groupMembers: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(venueName)
        dest.writeString(venueId)
        dest.writeString(tenantName)
        dest.writeString(tenantId)
        dest.writeString(postalCode)
        dest.writeString(address)
        dest.writeInt(id ?: -1)
        dest.writeLong(checkInTimeMS ?: 0)
        dest.writeInt(groupMembersCount ?: 0)
        dest.writeString(groupMembers)
    }

    companion object CREATOR : Parcelable.Creator<QrResultDataModel> {
        override fun createFromParcel(parcel: Parcel): QrResultDataModel {
            return QrResultDataModel(parcel)
        }

        override fun newArray(size: Int): Array<QrResultDataModel?> {
            return arrayOfNulls(size)
        }
    }
}
