package org.dhis2.data.notifications

import NotificationsApi
import UserGroupsApi
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import org.dhis2.commons.prefs.BasicPreferenceProvider
import org.dhis2.commons.prefs.Preference
import org.dhis2.usescases.notifications.domain.Notification
import org.dhis2.usescases.notifications.domain.NotificationRepository
import org.dhis2.usescases.notifications.domain.Ref
import org.dhis2.usescases.notifications.domain.UserGroups
import org.hisp.dhis.android.core.D2
import timber.log.Timber

class NotificationD2Repository(
    private val d2: D2,
    private val preferenceProvider: BasicPreferenceProvider,
    private val notificationsApi: NotificationsApi,
    private val userGroupsApi: UserGroupsApi
) : NotificationRepository {

    override fun sync(): Flow<Unit> = flow {
        try {
            val allNotifications = getAllNotificationsFromRemote()

            val userGroups = getUserGroups()

            val userNotifications = getNotificationsForCurrentUser(
                allNotifications,
                userGroups.userGroups
            )

            preferenceProvider.saveAsJson(Preference.NOTIFICATIONS, userNotifications)

            emit(Unit)

            Timber.d("Notifications synced")
            Timber.d("Notifications: $userNotifications")

        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun get(): Flow<List<Notification>> = flow {
        val listStringType = object : TypeToken<List<Notification>>() {}

        val notifications = preferenceProvider.getObjectFromJson(
            Preference.NOTIFICATIONS,
            listStringType,
            listOf()
        )

        emit(notifications)
    }

    override fun getById(id: String): Flow<Notification?> = flow {
        val notifications = getAllNotificationsFromRemote()

        val notification = notifications.find { it.id == id }

        emit(notification)
    }

    override fun save(notification: Notification): Flow<Unit> = flow {
        try {
            val notifications = getAllNotificationsFromRemote().map {
                if (it.id == notification.id) {
                    notification
                } else {
                    it
                }
            }

            notificationsApi.postData(notifications)

            emit(Unit)

        }catch (e: Exception){
            Timber.e("Error updating notifications: $e")
        }
    }.flatMapConcat { sync() }

    private suspend fun getAllNotificationsFromRemote(): List<Notification> {
        try {
            val notifications = notificationsApi.getData()

            return notifications
        } catch (e: Exception) {
            Timber.e("Error getting notifications: $e")
            return emptyList()
        }
    }

    private suspend fun getUserGroups(): UserGroups {
        try {
            val userGroups =
                userGroupsApi.getData(d2.userModule().user().blockingGet()!!.uid())

            return userGroups
        } catch (e: Exception) {
            Timber.e("Error getting userGroups: $e")
            return UserGroups(listOf())
        }
    }

    private fun getNotificationsForCurrentUser(
        allNotifications: List<Notification>,
        userGroups: List<Ref>
    ): List<Notification> {
        val userGroupIds = userGroups.map { it.id }
        val userId = d2.userModule().user().blockingGet()!!.uid()

        val nonReadByUserNotifications = allNotifications.filter { notification ->
            notification.readBy.none { readBy ->
                readBy.id == userId
            }
        }

        val notificationsByAll = nonReadByUserNotifications.filter { notification ->
            val wildcard = notification.recipients.wildcard.lowercase()
            wildcard == "ALL".lowercase() || wildcard.contains("ALL".lowercase())
        }

        val notificationsByUserGroup = nonReadByUserNotifications.filter { notification ->
            notification.recipients.userGroups.any {
                userGroupIds.contains(it.id) } && isForAndroid(notification)
        }

        val notificationsByUser = nonReadByUserNotifications.filter { notification ->
            notification.recipients.users.any { it.id == userId } && isForAndroid(notification)
        }

        return (notificationsByAll + notificationsByUserGroup + notificationsByUser).distinct()
    }

    private fun isForAndroid(notification: Notification): Boolean {
        val wildcard = notification.recipients.wildcard.lowercase()
        return wildcard == "Android".lowercase() ||
            wildcard.isEmpty() ||
            wildcard == "BOTH".lowercase() ||
            wildcard.contains("Android".lowercase()) ||
            wildcard.contains("BOTH".lowercase())
    }
}


