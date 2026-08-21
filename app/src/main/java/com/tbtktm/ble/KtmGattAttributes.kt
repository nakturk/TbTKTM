package com.tbtktm.ble

import java.util.UUID

object KtmGattAttributes {
    // 1. TBT Navigasyon Servisi
    val TBT_SERVICE: UUID = UUID.fromString("71ced1ac-0700-44f5-9454-806ff70b3e02")
    val TBT_AUTH_REQUESTS: UUID = UUID.fromString("71ced1ac-0701-44f5-9454-806ff70b3e02")
    val TBT_AUTH_REPLIES: UUID = UUID.fromString("71ced1ac-0702-44f5-9454-806ff70b3e02")
    val TBT_NAVIGATION_STATE: UUID = UUID.fromString("71ced1ac-0703-44f5-9454-806ff70b3e02")
    val TBT_TURN_ICON: UUID = UUID.fromString("71ced1ac-0704-44f5-9454-806ff70b3e02")
    val TBT_TURN_DISTANCE: UUID = UUID.fromString("71ced1ac-0705-44f5-9454-806ff70b3e02")
    val TBT_TURN_INFO: UUID = UUID.fromString("71ced1ac-0706-44f5-9454-806ff70b3e02")
    val TBT_TURN_ROAD: UUID = UUID.fromString("71ced1ac-0707-44f5-9454-806ff70b3e02")
    val TBT_EST_ARRIVAL_TIME: UUID = UUID.fromString("71ced1ac-0708-44f5-9454-806ff70b3e02")
    val TBT_DISTANCE_TO_DESTINATION: UUID = UUID.fromString("71ced1ac-0709-44f5-9454-806ff70b3e02")
    val TBT_NOTIFICATION_TEXT: UUID = UUID.fromString("71ced1ac-070a-44f5-9454-806ff70b3e02")
    val TBT_NAVIGATION_REQUEST: UUID = UUID.fromString("71ced1ac-070b-44f5-9454-806ff70b3e02")

    // 2. RCM (Remote Control Manager - Gidon Tuşları)
    val RCM_SERVICE: UUID = UUID.fromString("71ced1ac-0100-44f5-9454-806ff70b3e02")
    val RCM_REMOTE_CONTROL: UUID = UUID.fromString("71ced1ac-0103-44f5-9454-806ff70b3e02")

    // 3. Base Servis (VIN)
    val BASE_SERVICE: UUID = UUID.fromString("71ced1ac-0000-44f5-9454-806ff70b3e02")
    val BASE_GET_VIN_REQ: UUID = UUID.fromString("71ced1ac-0001-44f5-9454-806ff70b3e02")
    val BASE_VIN: UUID = UUID.fromString("71ced1ac-0002-44f5-9454-806ff70b3e02")

    // Client Characteristic Configuration Descriptor (CCCD)
    val CCCD_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
