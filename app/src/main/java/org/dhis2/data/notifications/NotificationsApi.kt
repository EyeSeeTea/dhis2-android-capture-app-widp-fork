
import org.dhis2.usescases.notifications.domain.Notification
import org.dhis2.usescases.notifications.domain.UserGroups
import org.hisp.dhis.android.core.arch.api.HttpServiceClient

class NotificationsApi (private val client: HttpServiceClient) {
    suspend fun getData(): List<Notification>{
        return client.get {
            url("dataStore/notifications/notifications")
        }
    }

    suspend fun postData( notifications:List<Notification>) {
        return client.put {
            url("dataStore/notifications/notifications")
            body(notifications)
        }
    }
}

class UserGroupsApi (private val client: HttpServiceClient) {
    suspend fun getData(): UserGroups {
        return client.get {
            url("me?fields=userGroups")
        }
    }
}

