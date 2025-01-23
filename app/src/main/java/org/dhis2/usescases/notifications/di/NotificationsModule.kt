package org.dhis2.usescases.notifications.di

import dagger.Module
import dagger.Provides
import org.dhis2.commons.prefs.BasicPreferenceProvider
import org.dhis2.data.notifications.NotificationD2Repository
import org.dhis2.data.notifications.NotificationsApi
import org.dhis2.data.notifications.UserD2Repository
import org.dhis2.data.notifications.UserGroupsApi
import org.dhis2.usescases.notifications.domain.GetNotifications
import org.dhis2.usescases.notifications.domain.MarkNotificationAsRead
import org.dhis2.usescases.notifications.domain.NotificationRepository
import org.dhis2.usescases.notifications.domain.UserRepository
import org.dhis2.usescases.notifications.presentation.NotificationsPresenter
import org.hisp.dhis.android.core.D2Manager
import javax.inject.Singleton

@Module
class NotificationsModule () {
    @Provides
    @Singleton
    internal fun notificationsPresenter(
        getNotifications: GetNotifications,
        markNotificationAsRead: MarkNotificationAsRead
    ): NotificationsPresenter {
        return NotificationsPresenter(
            getNotifications,
            markNotificationAsRead
        )
    }

    @Provides
    @Singleton
    internal fun getMarkNotificationAsRead(
        notificationRepository: NotificationRepository,
        userRepository: UserRepository
    ): MarkNotificationAsRead {
        return MarkNotificationAsRead(notificationRepository, userRepository)
    }

    @Provides
    @Singleton
    internal fun getNotifications(
        notificationRepository: NotificationRepository,
    ): GetNotifications {
        return GetNotifications(notificationRepository)
    }

    @Provides
    @Singleton
    internal fun notificationsRepository(
        preferences: BasicPreferenceProvider
    ): NotificationRepository {
        val d2 = D2Manager.getD2()

        val biometricsConfigApi = d2.retrofit().create(
            NotificationsApi::class.java
        )

        val userGroupsApi = d2.retrofit().create(
            UserGroupsApi::class.java
        )

        return NotificationD2Repository(
            d2,
            preferences,
            biometricsConfigApi,
            userGroupsApi
        )
    }

    @Provides
    @Singleton
    internal fun userRepository(): UserRepository {
        val d2 = D2Manager.getD2()

        return UserD2Repository(
            d2
        )
    }
}