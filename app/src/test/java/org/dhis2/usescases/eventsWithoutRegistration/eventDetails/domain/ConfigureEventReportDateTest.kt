package org.dhis2.usescases.eventsWithoutRegistration.eventDetails.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.dhis2.commons.data.EventCreationType
import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.resources.DhisPeriodUtils
import org.dhis2.usescases.eventsWithoutRegistration.eventDetails.data.EventDetailsRepository
import org.dhis2.usescases.eventsWithoutRegistration.eventDetails.providers.EventDetailResourcesProvider
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.period.PeriodType
import org.hisp.dhis.android.core.program.ProgramStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConfigureEventReportDateTest {

    private val resourcesProvider: EventDetailResourcesProvider = mock {
        on { provideDueDate() } doReturn DUE_DATE
        on { provideEventDate() } doReturn EVENT_DATE
        on { provideNextEventDate(label = null) } doReturn NEXT_EVENT
    }

    private val programStage: ProgramStage = mock()

    private val repository: EventDetailsRepository = mock {
        on { getProgramStage() } doReturn programStage
    }

    private val periodUtils: DhisPeriodUtils = mock()

    private lateinit var configureEventReportDate: ConfigureEventReportDate

    @Test
    fun `Should return stored event info when existing event`() = runBlocking {
        // Given Existing event
        configureEventReportDate = ConfigureEventReportDate(
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
        )

        // And has a concrete date
        val expectedDate = "14/02/2022"

        val event: Event = mock {
            on { eventDate() } doReturn DateUtils.uiDateFormat().parse(expectedDate)
        }
        whenever(repository.getEvent()) doReturn event

        // When reportDate is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then report date should be active
        assert(eventDate.active)
        // Then stored date should be displayed
        assert(eventDate.dateValue == expectedDate)
        // Then default label should be displayed
        assert(eventDate.label == EVENT_DATE)
    }

    @Test
    fun `Should return tomorrow when new daily event`() = runBlocking {
        // Given the creation of new event
        // And periodType is daily
        val periodType = PeriodType.Daily
        configureEventReportDate = ConfigureEventReportDate(
            resourceProvider = resourcesProvider,
            repository = repository,
            periodType = periodType,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
        )
        val today = "15/02/2022"

        whenever(
            repository.getEnrollmentDate(ENROLLMENT_ID),
        ) doReturn DateUtils.getInstance().getStartOfDay(DateUtils.uiDateFormat().parse(today))
        whenever(
            repository.getEnrollmentIncidentDate(ENROLLMENT_ID),
        ) doReturn DateUtils.getInstance().getStartOfDay(DateUtils.uiDateFormat().parse(today))
        val tomorrow = "16/02/2022"

        whenever(
            periodUtils.getPeriodUIString(any(), any(), any()),
        ) doReturn tomorrow

        // When reportDate is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then date should be tomorrow
        assert(eventDate.dateValue == tomorrow)
    }

    @Test
    fun `Get next period when creating scheduled event`() = runBlocking {
        // Given the creation of new scheduled event
        configureEventReportDate = ConfigureEventReportDate(
            creationType = EventCreationType.SCHEDULE,
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
            scheduleInterval = 6,
        )

        val lastEventDate = "13/02/2022"
        val nextEventDate = "19/02/2022"
        whenever(
            repository.getStageLastDate(ENROLLMENT_ID),
        ) doReturn DateUtils.uiDateFormat().parse(lastEventDate)

        // When reportDate is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then date should be next period
        assert(eventDate.dateValue == nextEventDate)
    }

    @Test
    fun `Get next period when creating first scheduled event generated by enrollment date`() = runBlocking {
        // Given the creation of new scheduled event
        configureEventReportDate = ConfigureEventReportDate(
            creationType = EventCreationType.SCHEDULE,
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
            scheduleInterval = 6,
        )

        val lastEventDate = "13/02/2022"
        val nextEventDate = "19/02/2022"
        whenever(
            repository.getStageLastDate(ENROLLMENT_ID),
        ) doReturn null

        whenever(
            repository.getProgramStage()?.generatedByEnrollmentDate(),
        ) doReturn true
        whenever(
            repository.getEnrollmentDate(ENROLLMENT_ID),
        ) doReturn DateUtils.uiDateFormat().parse(lastEventDate)
        whenever(
            repository.getMinDaysFromStartByProgramStage(),
        ) doReturn 6

        // When reportDate is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then date should be next period
        assert(eventDate.dateValue == nextEventDate)
    }

    @Test
    fun `Get next period when creating first scheduled event generated by incident date`() = runBlocking {
        // Given the creation of new scheduled event
        configureEventReportDate = ConfigureEventReportDate(
            creationType = EventCreationType.SCHEDULE,
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
        )

        val lastEventDate = "13/02/2022"
        val nextEventDate = "15/02/2022"
        whenever(
            repository.getStageLastDate(ENROLLMENT_ID),
        ) doReturn null
        whenever(
            repository.getProgramStage()?.generatedByEnrollmentDate(),
        ) doReturn false
        whenever(
            repository.getEnrollmentIncidentDate(ENROLLMENT_ID),
        ) doReturn DateUtils.uiDateFormat().parse(lastEventDate)
        whenever(
            repository.getMinDaysFromStartByProgramStage(),
        ) doReturn 2

        // When reportDate is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then date should be next period
        assert(eventDate.dateValue == nextEventDate)
    }

    @Test
    fun `Should hide field when scheduled`() = runBlocking {
        // Given an scheduled event
        configureEventReportDate = ConfigureEventReportDate(
            creationType = EventCreationType.SCHEDULE,
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
        )

        // And with hidden due date
        whenever(programStage.hideDueDate()) doReturn true
        whenever(repository.getStageLastDate(ENROLLMENT_ID)) doReturn DateUtils.getInstance().today
        whenever(repository.getMinDaysFromStartByProgramStage()) doReturn 6
        whenever(repository.getProgramStage()) doReturn programStage
        whenever(repository.getEvent()) doReturn null
        whenever(programStage.displayEventLabel()) doReturn null

        // When report date is invoked
        val eventDate = configureEventReportDate.invoke().first()

        // Then report date should be hidden
        assertFalse(eventDate.active)
        assertEquals(DUE_DATE, eventDate.label)
    }

    @Test
    fun `Should allow future dates when event type is scheduled`() = runBlocking {
        // Given an schedule event
        configureEventReportDate = ConfigureEventReportDate(
            creationType = EventCreationType.SCHEDULE,
            resourceProvider = resourcesProvider,
            repository = repository,
            periodUtils = periodUtils,
            enrollmentId = ENROLLMENT_ID,
        )

        whenever(repository.getStageLastDate(ENROLLMENT_ID)) doReturn DateUtils.getInstance().today
        whenever(repository.getMinDaysFromStartByProgramStage()) doReturn 6

        // When report date is invoked
        val eventDate = configureEventReportDate.invoke().first()
        // Then future dates should be allowed
        assertTrue(eventDate.allowFutureDates)
    }

    companion object {
        const val PROGRAM_STAGE_ID = "programStageId"
        const val ENROLLMENT_ID = "enrollmentId"
        const val EVENT_ID = "eventId"
        const val DUE_DATE = "Due date"
        const val EVENT_DATE = "Event date"
        const val NEXT_EVENT = "Next event"
    }
}
