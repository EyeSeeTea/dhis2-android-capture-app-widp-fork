package org.dhis2.usescases.programEventDetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dhis2.org.analytics.charts.ui.GroupAnalyticsFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.dhis2.R
import org.dhis2.bindings.app
import org.dhis2.bindings.clipWithRoundedCorners
import org.dhis2.bindings.dp
import org.dhis2.commons.Constants
import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.date.DateUtils.OnFromToSelector
import org.dhis2.commons.date.Period
import org.dhis2.commons.filters.FilterItem
import org.dhis2.commons.filters.FilterManager
import org.dhis2.commons.filters.FilterManager.PeriodRequest
import org.dhis2.commons.filters.FiltersAdapter
import org.dhis2.commons.matomo.Actions.Companion.CREATE_EVENT
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.orgunitselector.OURepositoryConfiguration
import org.dhis2.commons.orgunitselector.OUTreeFragment
import org.dhis2.commons.orgunitselector.OrgUnitSelectorScope
import org.dhis2.commons.prefs.Preference.Companion.CURRENT_ORG_UNIT
import org.dhis2.commons.sync.OnDismissListener
import org.dhis2.commons.sync.SyncContext
import org.dhis2.databinding.ActivityProgramEventDetailBinding
import org.dhis2.form.model.EventMode
import org.dhis2.ui.ThemeManager
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity
import org.dhis2.usescases.general.ActivityGlobalAbstract
import org.dhis2.usescases.programEventDetail.ProgramEventDetailViewModel.EventProgramScreen
import org.dhis2.usescases.programEventDetail.eventList.EventListFragment
import org.dhis2.usescases.programEventDetail.eventMap.EventMapFragment
import org.dhis2.utils.analytics.DATA_CREATION
import org.dhis2.utils.category.CategoryDialog
import org.dhis2.utils.category.CategoryDialog.Companion.TAG
import org.dhis2.utils.customviews.RxDateDialog
import org.dhis2.utils.customviews.navigationbar.NavigationPage
import org.dhis2.utils.granularsync.SyncStatusDialog
import org.dhis2.utils.granularsync.shouldLaunchSyncDialog
import org.hisp.dhis.android.core.period.DatePeriod
import org.hisp.dhis.android.core.program.Program
import org.hisp.dhis.mobile.ui.designsystem.component.navigationBar.NavigationBar
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class ProgramEventDetailActivity :
    ActivityGlobalAbstract(),
    ProgramEventDetailView {

    private lateinit var binding: ActivityProgramEventDetailBinding

    @Inject
    lateinit var presenter: ProgramEventDetailPresenter

    @Inject
    lateinit var filtersAdapter: FiltersAdapter

    @Inject
    lateinit var networkUtils: NetworkUtils

    @JvmField
    @Inject
    var themeManager: ThemeManager? = null

    @Inject
    lateinit var viewModelFactory: ProgramEventDetailViewModelFactory

    @Inject
    lateinit var ouRepositoryConfiguration: OURepositoryConfiguration

    private var backDropActive = false
    private var programUid: String = ""

    private val programEventsViewModel: ProgramEventDetailViewModel by viewModels {
        viewModelFactory
    }

    var component: ProgramEventDetailComponent? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        initExtras()
        initInjection()
        themeManager?.setProgramTheme(programUid)
        super.onCreate(savedInstanceState)
        initEventFilters()
        initViewModel()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_program_event_detail)
        binding.presenter = presenter
        binding.totalFilters = FilterManager.getInstance().totalFilters

        setupBottomNavigation()
        binding.fragmentContainer.clipWithRoundedCorners(16.dp)
        binding.filterLayout.adapter = filtersAdapter
        presenter.init()
        binding.syncButton.setOnClickListener { showSyncDialogProgram() }

        if (intent.shouldLaunchSyncDialog()) {
            showSyncDialogProgram()
        }

        programEventsViewModel.viewModelScope.launch {
            programEventsViewModel.shouldNavigateToEventDetails.collectLatest { eventUid ->
                analyticsHelper.setEvent(CREATE_EVENT, DATA_CREATION, CREATE_EVENT)
                val intent = EventCaptureActivity.intent(
                    context = context,
                    eventUid = eventUid,
                    programUid = programUid,
                    eventMode = EventMode.NEW,
                )
                startActivity(intent)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.navigationBar.setContent {
            DHIS2Theme {
                val uiState by programEventsViewModel.navigationBarUIState
                val isBackdropActive by programEventsViewModel.backdropActive.observeAsState(false)
                var selectedItemIndex by remember(uiState) {
                    mutableIntStateOf(
                        uiState.items.indexOfFirst {
                            it.id == uiState.selectedItem
                        },
                    )
                }

                LaunchedEffect(uiState.selectedItem) {
                    when (uiState.selectedItem) {
                        NavigationPage.LIST_VIEW -> {
                            programEventsViewModel.showList()
                        }

                        NavigationPage.MAP_VIEW -> {
                            networkUtils.performIfOnline(
                                context = this@ProgramEventDetailActivity,
                                action = {
                                    presenter.trackEventProgramMap()
                                    programEventsViewModel.showMap()
                                },
                                onDialogDismissed = {
                                    selectedItemIndex = 0
                                },
                                noNetworkMessage = getString(R.string.msg_network_connection_maps),
                            )
                        }

                        NavigationPage.ANALYTICS -> {
                            presenter.trackEventProgramAnalytics()
                            programEventsViewModel.showAnalytics()
                        }

                        else -> {
                            // no-op
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.items.size > 1 && isBackdropActive.not(),
                    enter = slideInVertically(animationSpec = tween(200)) { it },
                    exit = slideOutVertically(animationSpec = tween(200)) { it },
                ) {
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        items = uiState.items,
                        selectedItemIndex = selectedItemIndex,
                    ) { page ->
                        programEventsViewModel.onNavigationPageChanged(page)
                    }
                }
            }
        }
    }

    private fun initExtras() {
        programUid = intent.getStringExtra(EXTRA_PROGRAM_UID) ?: ""
    }

    private fun initInjection() {
        component = app().userComponent()
            ?.plus(
                ProgramEventDetailModule(
                    this,
                    this, programUid,
                    OrgUnitSelectorScope.ProgramCaptureScope(programUid),
                ),
            )
        component?.inject(this)
    }

    private fun initEventFilters() {
        FilterManager.getInstance().clearCatOptCombo()
        FilterManager.getInstance().clearEventStatus()
    }

    private fun initViewModel() {
        programEventsViewModel.progress().observe(this) { showProgress: Boolean ->
            if (showProgress) {
                binding.toolbarProgress.show()
            } else {
                binding.toolbarProgress.hide()
            }
        }
        programEventsViewModel.eventSyncClicked.observe(this) { eventUid: String? ->
            if (eventUid != null) {
                presenter.onSyncIconClick(eventUid)
            }
        }
        programEventsViewModel.eventClicked.observe(this) { eventData: Pair<String, String>? ->
            if (eventData != null && !programEventsViewModel.recreationActivity) {
                programEventsViewModel.onRecreationActivity(false)
                navigateToEvent(eventData.component1(), eventData.component2())
            } else if (programEventsViewModel.recreationActivity) {
                programEventsViewModel.onRecreationActivity(false)
            }
        }
        programEventsViewModel.writePermission.observe(this) { canWrite: Boolean ->
            binding.addEventButton.visibility = if (canWrite) View.VISIBLE else View.GONE
        }
        programEventsViewModel.currentScreen.observe(this) { currentScreen: EventProgramScreen? ->
            currentScreen?.let {
                when (it) {
                    EventProgramScreen.LIST -> showList()
                    EventProgramScreen.MAP -> showMap()
                    EventProgramScreen.ANALYTICS -> showAnalytics()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.addEventButton.isEnabled = true
        binding.totalFilters = FilterManager.getInstance().totalFilters
    }

    private fun showSyncDialogProgram() {
        SyncStatusDialog.Builder()
            .withContext(this)
            .withSyncContext(SyncContext.EventProgram(programUid))
            .onDismissListener(object : OnDismissListener {
                override fun onDismiss(hasChanged: Boolean) {
                    if (hasChanged) FilterManager.getInstance().publishData()
                }
            })
            .onNoConnectionListener {
                Snackbar.make(
                    binding.root,
                    R.string.sync_offline_check_connection,
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
            .show("EVENT_SYNC")
    }

    public override fun onPause() {
        super.onPause()
        if (isChangingConfigurations) {
            programEventsViewModel.onRecreationActivity(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sessionManagerServiceImpl.isUserLoggedIn()) {
            presenter.setOpeningFilterToNone()
            presenter.onDettach()
            FilterManager.getInstance().clearEventStatus()
            FilterManager.getInstance().clearCatOptCombo()
            FilterManager.getInstance().clearWorkingList(true)
            FilterManager.getInstance().clearAssignToMe()
            FilterManager.getInstance().clearFlow()
            presenter.clearOtherFiltersIfWebAppIsConfig()
        }
    }

    override fun setProgram(programModel: Program) {
        binding.name = programModel.displayName()
    }

    override fun renderError(message: String) {
        if (activity != null) {
            MaterialAlertDialogBuilder(activity, R.style.MaterialDialog)
                .setPositiveButton(getString(R.string.button_ok), null)
                .setTitle(getString(R.string.error))
                .setMessage(message)
                .show()
        }
    }

    override fun showHideFilter() {
        val transition: Transition = ChangeBounds()
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                programEventsViewModel.updateBackdrop(backDropActive)
                if (!backDropActive) {
                    binding.clearFilters.hide()
                }
            }

            override fun onTransitionEnd(transition: Transition) {
                if (backDropActive) {
                    binding.clearFilters.show()
                }
            }

            override fun onTransitionCancel(transition: Transition) {
                /*No action needed*/
            }

            override fun onTransitionPause(transition: Transition) {
                /*No action needed*/
            }

            override fun onTransitionResume(transition: Transition) {
                /*No action needed*/
            }
        })
        backDropActive = !backDropActive

        transition.duration = 200
        TransitionManager.beginDelayedTransition(binding.backdropLayout, transition)

        val initSet = ConstraintSet()
        initSet.clone(binding.backdropLayout)
        if (backDropActive) {
            initSet.connect(
                R.id.fragmentContainer,
                ConstraintSet.TOP,
                R.id.filterLayout,
                ConstraintSet.BOTTOM,
                16.dp,
            )
            initSet.connect(
                R.id.fragmentContainer,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0,
            )
            initSet.connect(
                R.id.addEventButton,
                ConstraintSet.BOTTOM,
                R.id.fragmentContainer,
                ConstraintSet.BOTTOM,
                16.dp,
            )
        } else {
            initSet.connect(
                R.id.fragmentContainer,
                ConstraintSet.TOP,
                R.id.backdropGuideTop,
                ConstraintSet.BOTTOM,
                0,
            )
            initSet.connect(
                R.id.fragmentContainer,
                ConstraintSet.BOTTOM,
                R.id.navigationBar,
                ConstraintSet.TOP,
                0,
            )
            initSet.connect(
                R.id.addEventButton,
                ConstraintSet.BOTTOM,
                R.id.navigationBar,
                ConstraintSet.TOP,
                16.dp,
            )
        }
        initSet.applyTo(binding.backdropLayout)
    }

    override fun selectOrgUnitForNewEvent() {
        val orgUnitList = ouRepositoryConfiguration.orgUnitRepository(null)
        if (orgUnitList.size == 1) {
            presenter.stageUid?.let {
                programEventsViewModel.onOrgUnitForNewEventSelected(
                    programUid = programUid,
                    orgUnitUid = orgUnitList.first().uid(),
                    programStageUid = it,
                )
            }
        } else {
            OUTreeFragment.Builder()
                .singleSelection()
                .withPreselectedOrgUnits(
                    listOf(sharedPreferences.getString(CURRENT_ORG_UNIT, "") ?: ""),
                )
                .orgUnitScope(
                    OrgUnitSelectorScope.ProgramCaptureScope(programUid),
                )
                .onSelection { selectedOrgUnits ->
                    if (selectedOrgUnits.isNotEmpty()) {
                        presenter.stageUid?.let {
                            programEventsViewModel.onOrgUnitForNewEventSelected(
                                programUid = programUid,
                                orgUnitUid = selectedOrgUnits.first().uid(),
                                programStageUid = it,
                            )
                        }
                    } else {
                        enableAddEventButton(true)
                    }
                }
                .build()
                .show(supportFragmentManager, "ORG_UNIT_DIALOG")
        }
    }

    private fun enableAddEventButton(enable: Boolean) {
        binding.addEventButton.isEnabled = enable
    }

    override fun setWritePermission(canWrite: Boolean) {
        programEventsViewModel.writePermission.value = canWrite
    }

    override fun updateFilters(totalFilters: Int) {
        binding.totalFilters = totalFilters
        binding.executePendingBindings()
    }

    override fun showPeriodRequest(periodRequest: PeriodRequest) {
        if (periodRequest == PeriodRequest.FROM_TO) {
            DateUtils.getInstance()
                .fromCalendarSelector(this.context) { datePeriod: List<DatePeriod?>? ->
                    FilterManager.getInstance().addPeriod(datePeriod)
                }
        } else {
            val onFromToSelector =
                OnFromToSelector { datePeriods ->
                    FilterManager.getInstance().addPeriod(datePeriods)
                }

            DateUtils.getInstance().showPeriodDialog(
                this,
                onFromToSelector,
                true,
            ) {
                val disposable = RxDateDialog(activity, Period.WEEKLY)
                    .createForFilter().show()
                    .subscribe(
                        { selectedDates: org.dhis2.commons.data.tuples.Pair<Period?, List<Date?>?> ->
                            onFromToSelector.onFromToSelected(
                                DateUtils.getInstance().getDatePeriodListFor(
                                    selectedDates.val1(),
                                    selectedDates.val0(),
                                ),
                            )
                        },
                        { t: Throwable? -> Timber.e(t) },
                    )
            }
        }
    }

    override fun openOrgUnitTreeSelector() {
        OUTreeFragment.Builder()
            .withPreselectedOrgUnits(FilterManager.getInstance().orgUnitUidsFilters)
            .onSelection { selectedOrgUnits ->
                presenter.setOrgUnitFilters(selectedOrgUnits)
            }
            .build()
            .show(supportFragmentManager, "OUTreeFragment")
    }

    override fun showTutorial(shaked: Boolean) {
        setTutorial()
    }

    override fun navigateToEvent(eventId: String, orgUnit: String) {
        programEventsViewModel.updateEvent = eventId
        val bundle = Bundle()
        bundle.putString(Constants.PROGRAM_UID, programUid)
        bundle.putString(Constants.EVENT_UID, eventId)
        bundle.putString(Constants.ORG_UNIT, orgUnit)
        startActivity(
            EventCaptureActivity::class.java,
            EventCaptureActivity.getActivityBundle(eventId, programUid, EventMode.CHECK),
            false,
            false,
            null,
        )
    }

    override fun showSyncDialog(uid: String) {
        SyncStatusDialog.Builder()
            .withContext(this)
            .withSyncContext(SyncContext.Event(uid))
            .onDismissListener(object : OnDismissListener {
                override fun onDismiss(hasChanged: Boolean) {
                    if (hasChanged) FilterManager.getInstance().publishData()
                }
            })
            .onNoConnectionListener {
                Snackbar.make(
                    binding.root,
                    R.string.sync_offline_check_connection,
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
            .show(FRAGMENT_TAG)
    }

    private fun showList() {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragmentContainer,
            EventListFragment(),
            "EVENT_LIST",
        ).commitNow()
        binding.addEventButton.visibility =
            if (programEventsViewModel.writePermission.value == true) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.filter.visibility = View.VISIBLE
    }

    private fun showMap() {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragmentContainer,
            EventMapFragment(),
            "EVENT_MAP",
        ).commitNow()
        binding.addEventButton.visibility = View.GONE
        binding.filter.visibility = View.VISIBLE
    }

    private fun showAnalytics() {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragmentContainer,
            GroupAnalyticsFragment.forProgram(programUid),
        ).commitNow()
        binding.addEventButton.visibility = View.GONE
        binding.filter.visibility = View.GONE
    }

    override fun showCatOptComboDialog(catComboUid: String) {
        CategoryDialog(
            CategoryDialog.Type.CATEGORY_OPTION_COMBO,
            catComboUid,
            false,
            null,
        ) { selectedCatOptionCombo ->
            presenter.filterCatOptCombo(selectedCatOptionCombo)
        }.show(supportFragmentManager, TAG)
    }

    override fun setFilterItems(programFilters: List<FilterItem>) {
        filtersAdapter.submitList(programFilters)
    }

    override fun hideFilters() {
        binding.filter.visibility = View.GONE
    }

    companion object {
        private const val FRAGMENT_TAG = "SYNC"
        const val EXTRA_PROGRAM_UID = "PROGRAM_UID"
        fun getBundle(programUid: String?): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_PROGRAM_UID, programUid)
            return bundle
        }

        fun intent(context: Context, programUid: String): Intent {
            return Intent(context, ProgramEventDetailActivity::class.java).apply {
                putExtra(EXTRA_PROGRAM_UID, programUid)
            }
        }
    }
}
