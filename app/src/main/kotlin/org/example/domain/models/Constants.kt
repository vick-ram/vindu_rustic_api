package org.example.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationChannel {
    @SerialName( "fcm" ) FCM,
    @SerialName("email") EMAIL,
    @SerialName("database") DATABASE,
    @SerialName("sms") SMS
}

enum class Platform { ANDROID, IOS, WEB }

enum class MediaType { IMAGE, VIDEO, DOCUMENT, AUDIO }
