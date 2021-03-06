package sg.gov.tech.bluetrace.bluetooth.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import sg.gov.tech.bluetrace.BuildConfig
import java.util.*
import kotlin.properties.Delegates

class GattService constructor(val context: Context, serviceUUIDString: String) {

    private var serviceUUID = UUID.fromString(serviceUUIDString)

    var gattService: BluetoothGattService by Delegates.notNull()

    private var characteristicV1: BluetoothGattCharacteristic
    private var characteristicV2: BluetoothGattCharacteristic

    init {
        gattService = BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        //create characteristic and descriptors - this will be the device specific information to be read/written?
        //TO-DO use encrypted read and write??
        characteristicV1 = BluetoothGattCharacteristic(
            serviceUUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        gattService.addCharacteristic(characteristicV1)

        characteristicV2 = BluetoothGattCharacteristic(
            UUID.fromString(BuildConfig.V2_CHARACTERISTIC_ID),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        gattService.addCharacteristic(characteristicV2)
    }

    fun setValue(value: String) {
        setValue(value.toByteArray(Charsets.UTF_8))
    }

    fun setValue(value: ByteArray) {
        characteristicV1.value = value
    }
}
