package org.dhis2.usescases.notifications.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

data class Notification(
    val content: String,
    val createdAt: Date,
    val id: String,
    val readBy: List<ReadBy>,
    val recipients: Recipients,
    val permissions: Permissions?,
)

data class ReadBy(
    val date: Date,
    val id: String,
    val name: String
)

data class Recipients(
    val userGroups: ArrayList<Ref>,
    val users: List<Ref>,
    val wildcard: String,
){
    fun getWildcard(): NotificationWildcard? =
        NotificationWildcard.fromValue(wildcard.lowercase())
}

data class Ref(
    val id: String,
    val name: String?
)

data class UserGroups(
    val userGroups: List<Ref>,
)

data class Permissions(
    val publicAccess: String,
    val userAccesses: List<UserAccesses>,
    val userGroupAccesses: List<UserGroupAccesses>,
)

data class UserAccesses(
    val access: String,
    val id: String,
    val name: String,
)

data class UserGroupAccesses(
    val access: String,
    val id: String,
    val name: String,
)

@Serializable
enum class NotificationWildcard {
    @SerialName("ALL")
    ALL,
    @SerialName("android")
    ANDROID,
    @SerialName("web")
    WEB,
    @SerialName("both")
    BOTH;
    companion object {
        fun fromValue(value: String): NotificationWildcard? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}