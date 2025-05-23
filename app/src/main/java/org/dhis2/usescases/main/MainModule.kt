package org.dhis2.usescases.main

import dagger.Module
import dagger.Provides
import dhis2.org.analytics.charts.Charts
import org.dhis2.commons.di.dagger.PerActivity
import org.dhis2.commons.featureconfig.data.FeatureConfigRepository
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.matomo.MatomoAnalyticsController
import org.dhis2.commons.prefs.PreferenceProvider
import org.dhis2.commons.resources.ColorUtils
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.schedulers.SchedulerProvider
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.data.server.UserManager
import org.dhis2.data.service.SyncStatusController
import org.dhis2.data.service.VersionRepository
import org.dhis2.data.service.workManager.WorkManagerController
import org.dhis2.usescases.login.SyncIsPerformedInteractor
import org.dhis2.usescases.settings.DeleteUserData
import org.dhis2.utils.customviews.navigationbar.NavigationPageConfigurator
import org.hisp.dhis.android.core.D2

@Module
class MainModule(val view: MainView, private val forceToNotSynced: Boolean) {

    @Provides
    @PerActivity
    fun homePresenter(
        homeRepository: HomeRepository,
        schedulerProvider: SchedulerProvider,
        preferences: PreferenceProvider,
        workManagerController: WorkManagerController,
        filterManager: FilterManager,
        matomoAnalyticsController: MatomoAnalyticsController,
        userManager: UserManager,
        deleteUserData: DeleteUserData,
        syncIsPerformedInteractor: SyncIsPerformedInteractor,
        syncStatusController: SyncStatusController,
        versionRepository: VersionRepository,
        dispatcherProvider: DispatcherProvider,
    ): MainPresenter {
        return MainPresenter(
            view,
            homeRepository,
            schedulerProvider,
            preferences,
            workManagerController,
            filterManager,
            matomoAnalyticsController,
            userManager,
            deleteUserData,
            syncIsPerformedInteractor,
            syncStatusController,
            versionRepository,
            dispatcherProvider,
            forceToNotSynced,
        )
    }

    @Provides
    @PerActivity
    fun provideSyncIsPerfomedInteractor(userManager: UserManager): SyncIsPerformedInteractor {
        return SyncIsPerformedInteractor(userManager)
    }

    @Provides
    @PerActivity
    fun provideHomeRepository(
        d2: D2,
        charts: Charts?,
        featureConfigRepositoryImpl: FeatureConfigRepository,
    ): HomeRepository {
        return HomeRepositoryImpl(d2, charts, featureConfigRepositoryImpl)
    }

    @Provides
    @PerActivity
    fun providePageConfigurator(
        homeRepository: HomeRepository,
    ): NavigationPageConfigurator {
        return HomePageConfigurator(homeRepository, ResourceManager(view.context, ColorUtils()))
    }

    @Provides
    @PerActivity
    fun provideDeleteUserData(
        workManagerController: WorkManagerController,
        preferencesProvider: PreferenceProvider,
        filterManager: FilterManager,
    ): DeleteUserData {
        return DeleteUserData(
            workManagerController,
            filterManager,
            preferencesProvider,
        )
    }
}
