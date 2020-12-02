package org.dhis2.utils.filters

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import org.dhis2.utils.filters.workingLists.WorkingListItem
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.CategoryOptionCombo
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.android.core.event.EventStatus
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit
import org.hisp.dhis.android.core.period.DatePeriod

sealed class FilterItem(val type: Filters, var isEnable: Boolean = true)

data class PeriodFilter(
    val periodLabel: String,
    var selectedPeriod: List<DatePeriod>,
    val selectedPeriodId: Int
) :
    FilterItem(Filters.PERIOD)

data class OrgUnitFilter(var selectedOrgUnits: LiveData<List<OrganisationUnit>>) :
    FilterItem(Filters.ORG_UNIT)

data class SyncStateFilter(var selectedSyncStates: List<State>) : FilterItem(Filters.SYNC_STATE) {
    fun setSyncStatus(addState: Boolean, vararg syncStates: State) {
        FilterManager.getInstance().addState(!addState, *syncStates)
    }

    fun observeSyncState(): ObservableField<List<State>> {
        return FilterManager.getInstance().observeSyncState()
    }
}

data class CatOptionComboFilter(
    val catCombo: CategoryCombo,
    val catOptionCombos: List<CategoryOptionCombo>,
    var selectedCatOptCombos: List<CategoryOptionCombo>
) :
    FilterItem(Filters.CAT_OPT_COMB)

data class EventStatusFilter(var selectedEventStatus: List<EventStatus>) :
    FilterItem(Filters.EVENT_STATUS) {
    fun setEventStatus(addStatus: Boolean, vararg eventStatus: EventStatus) {
        FilterManager.getInstance().addEventStatus(!addStatus, *eventStatus)
    }

    fun observeEventStatus(): ObservableField<List<EventStatus>> {
        return FilterManager.getInstance().observeEventStatus()
    }
}

data class AssignedFilter(var assignedToMe: Boolean) : FilterItem(Filters.ASSIGNED_TO_ME) {
    fun activate(setActive: Boolean) {
        FilterManager.getInstance().setAssignedToMe(setActive)
    }
}

data class EnrollmentDateFilter(
    val enrollmentDateLabel: String,
    var selectedEnrollmentDate: List<DatePeriod>,
    val selectedEnrollmentPeriodId: Int
) : FilterItem(Filters.ENROLLMENT_DATE)

data class EnrollmentStatusFilter(var selectedEnrollmentStatus: List<EnrollmentStatus>?) :
    FilterItem(Filters.ENROLLMENT_STATUS) {

    fun setEnrollmentStatus(addEnrollment: Boolean, enrollmentStatus: EnrollmentStatus) {
        FilterManager.getInstance().addEnrollmentStatus(!addEnrollment, enrollmentStatus)
    }

    fun observeEnrollmentStatus(): ObservableField<EnrollmentStatus> {
        return FilterManager.getInstance().observeEnrollmentStatus()
    }
}

data class WorkingListFilter(
    val workingLists: List<WorkingListItem>,
    var currentWorkingList: WorkingListItem?
) : FilterItem(Filters.WORKING_LIST) {
    fun onChecked(checkedId: Int) {
        workingLists.firstOrNull { it.hashCode() == checkedId }?.let {
            if (!it.isSelected()) {
                it.select()
            }
        } ?: workingLists.forEach {
            it.deselect()
        }.also {
            FilterManager.getInstance().currentWorkingList(null)
        }
    }
}