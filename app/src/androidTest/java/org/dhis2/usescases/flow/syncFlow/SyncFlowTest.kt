package org.dhis2.usescases.flow.syncFlow

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.WorkInfo
import org.dhis2.AppTest
import org.dhis2.lazyActivityScenarioRule
import org.dhis2.usescases.BaseTest
import org.dhis2.usescases.datasets.dataSetTableRobot
import org.dhis2.usescases.datasets.datasetDetail.DataSetDetailActivity
import org.dhis2.usescases.development.ConflictGenerator
import org.dhis2.usescases.flow.syncFlow.robot.dataSetRobot
import org.dhis2.usescases.flow.syncFlow.robot.eventWithoutRegistrationRobot
import org.dhis2.usescases.programEventDetail.ProgramEventDetailActivity
import org.dhis2.usescases.searchTrackEntity.SearchTEActivity
import org.dhis2.usescases.searchte.robot.searchTeiRobot
import org.dhis2.usescases.teidashboard.robot.eventRobot
import org.dhis2.usescases.teidashboard.robot.teiDashboardRobot
import org.hisp.dhis.android.core.D2Manager
import org.hisp.dhis.android.core.imports.ImportStatus
import org.hisp.dhis.android.core.mockwebserver.ResponseController.Companion.GET
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import syncFlowRobot
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SyncFlowTest : BaseTest() {

    @get:Rule
    val ruleDataSet = lazyActivityScenarioRule<DataSetDetailActivity>(launchActivity = false)

    @get:Rule
    val ruleSearch = lazyActivityScenarioRule<SearchTEActivity>(launchActivity = false)

    @get:Rule
    val ruleEventWithoutRegistration =
        lazyActivityScenarioRule<ProgramEventDetailActivity>(launchActivity = false)

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var workInfoStatusLiveData: MutableLiveData<List<WorkInfo>>

    private val d2 = D2Manager.getD2()
    private val conflictGenerator = ConflictGenerator(d2)

    override fun setUp() {
        super.setUp()
        setupMockServer()
        workInfoStatusLiveData =
            ApplicationProvider.getApplicationContext<AppTest>().mutableWorkInfoStatuses
    }

    @Ignore("Flaky test, will be addressed in issue ANDROAPP-6465")
    @Test
    fun shouldShowErrorWhenTEISyncFails() {
        mockWebServerRobot.addResponse(GET, "/api/system/ping", API_PING_RESPONSE_OK)

        val teiName = "Lars"
        val teiLastName = "Overland"

        prepareTBProgrammeIntentAndLaunchActivity(ruleSearch)

        searchTeiRobot(composeTestRule) {
            clickOnOpenSearch()
            openNextSearchParameter("First name")
            typeOnNextSearchTextParameter(teiName)
            openNextSearchParameter("Last name")
            typeOnNextSearchTextParameter(teiLastName)
            clickOnSearch()
            clickOnTEI(teiName)
        }

        teiDashboardRobot(composeTestRule) {
            clickOnEventWith(LAB_MONITORING_EVENT_DATE)
        }

        eventRobot(composeTestRule) {
//            fillRadioButtonForm(4)
            clickOnFormFabButton()
            clickOnCompleteButton()
        }

        syncFlowRobot(composeTestRule) {
            clickOnEventToSync()
            clickOnSyncButton()
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.RUNNING)))
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.FAILED)))
            checkSyncFailed()
        }
        cleanLocalDatabase()
    }

    @Ignore("Flaky test, will be addressed in issue ANDROAPP-6465")
    @Test
    fun shouldSuccessfullySyncSavedEvent() {
        mockWebServerRobot.addResponse(GET, "/api/system/ping", API_PING_RESPONSE_OK)

        prepareMalariaEventIntentAndLaunchActivity(ruleEventWithoutRegistration)

        eventWithoutRegistrationRobot(composeTestRule) {
            clickOnEventAtPosition(0)
        }

        eventRobot(composeTestRule) {
            clickOnFormFabButton()
            clickOnCompleteButton()
        }

        syncFlowRobot(composeTestRule) {
            clickOnEventToSync()
            clickOnSyncButton()
            conflictGenerator.generateConflictInEvent(ANTENATAL_CARE_EVENT_UID, ImportStatus.SUCCESS)
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.RUNNING)))
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.SUCCEEDED)))
            checkSyncWasSuccessfully()
        }
        cleanLocalDatabase()
    }

    @Ignore("Flaky test, will be addressed in issue ANDROAPP-6465")
    @Test
    fun shouldShowErrorWhenSyncEventFails() {
        mockWebServerRobot.addResponse(GET, "/api/system/ping", API_PING_RESPONSE_OK)

        prepareMalariaEventIntentAndLaunchActivity(ruleEventWithoutRegistration)

        eventWithoutRegistrationRobot(composeTestRule) {
            clickOnEventAtPosition(1)
        }

        eventRobot(composeTestRule) {
            clickOnFormFabButton()
            clickOnCompleteButton()
        }

        syncFlowRobot(composeTestRule) {
            clickOnEventToSync()
            clickOnSyncButton()
            conflictGenerator.generateConflictInEvent(ANTENATAL_CARE_EVENT_UID, ImportStatus.ERROR)
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.RUNNING)))
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.FAILED)))
            checkSyncFailed()
        }
        cleanLocalDatabase()
    }

    @Ignore("Flaky test, will be addressed in issue ANDROAPP-6465")
    @Test
    fun shouldSuccessfullySyncSavedDataSet() {
        mockWebServerRobot.addResponse(GET, "/api/system/ping", API_PING_RESPONSE_OK)

        prepareFacilityDataSetIntentAndLaunchActivity(ruleDataSet)

        dataSetRobot {
            composeTestRule.waitForIdle()
            clickOnDataSetAtPosition(0)
        }

        dataSetTableRobot(composeTestRule) {
            typeOnCell("bjDvmb4bfuf", 0, 0)
            clickOnEditValue()
            typeInput("1")
            clickOnAccept()
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            clickOnSaveButton()
            waitToDebounce(500)
            clickOnNegativeButton()
        }

        syncFlowRobot(composeTestRule) {
            clickOnDataSetToSync(0)
            clickOnSyncButton()
            conflictGenerator.generateStatusConflictInDataSet(ImportStatus.SUCCESS)
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.RUNNING)))
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.SUCCEEDED)))
            checkSyncWasSuccessfully()
        }
        cleanLocalDatabase()
    }

    @Ignore("Flaky test, will be addressed in issue ANDROAPP-6465")
    @Test
    fun shouldShowErrorWhenSyncDataSetFails() {
        prepareFacilityDataSetIntentAndLaunchActivity(ruleDataSet)

        dataSetRobot {
            clickOnDataSetAtPosition(0)
        }

        dataSetTableRobot(composeTestRule) {
            typeOnCell("bjDvmb4bfuf", 0, 0)
            clickOnEditValue()
            typeInput("1")
            clickOnAccept()
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            clickOnSaveButton()
            waitToDebounce(500)
            clickOnNegativeButton()
        }

        syncFlowRobot(composeTestRule) {
            clickOnDataSetToSync(0)
            clickOnSyncButton()
            conflictGenerator.generateStatusConflictInDataSet(ImportStatus.ERROR)
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.RUNNING)))
            workInfoStatusLiveData.postValue(arrayListOf(mockedGranularWorkInfo(WorkInfo.State.FAILED)))
            checkSyncFailed()
        }
        cleanLocalDatabase()
    }

    private fun mockedGranularWorkInfo(state: WorkInfo.State): WorkInfo {
        return WorkInfo(
            UUID.randomUUID(),
            state,
            Data.EMPTY,
            arrayListOf("GRANULAR"),
            Data.EMPTY,
            0,
            0,
        )
    }

    companion object {
        const val ANTENATAL_CARE_EVENT_UID = "onXW2DQHRGS"
        const val LAB_MONITORING_EVENT_DATE = "28/6/2020"
        const val API_PING_RESPONSE_OK = "mocks/systeminfo/ping.txt"
    }
}
