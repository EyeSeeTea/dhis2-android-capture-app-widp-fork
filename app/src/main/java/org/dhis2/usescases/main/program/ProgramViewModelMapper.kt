package org.dhis2.usescases.main.program

import org.dhis2.ui.MetadataIconData
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.dataset.DataSet
import org.hisp.dhis.android.core.dataset.DataSetInstanceSummary
import org.hisp.dhis.android.core.program.Program
import java.util.Date

class ProgramViewModelMapper() {
    fun map(
        program: Program,
        recordCount: Int,
        recordLabel: String,
        state: State,
        metadataIconData: MetadataIconData,
    ): ProgramUiModel {
        return ProgramUiModel(
            uid = program.uid(),
            title = program.displayName()!!,
            metadataIconData = metadataIconData,
            count = recordCount,
            type = if (program.trackedEntityType() != null) {
                program.trackedEntityType()!!.uid()
            } else {
                null
            },
            typeName = recordLabel,
            programType = program.programType()!!.name,
            description = program.displayDescription(),
            onlyEnrollOnce = program.onlyEnrollOnce() == true,
            accessDataWrite = program.access().data().write(),
            state = State.valueOf(state.name),
            downloadState = ProgramDownloadState.NONE,
            stockConfig = null,
            lastUpdated = program.lastUpdated() ?: Date(),
        )
    }

    fun map(
        dataSet: DataSet,
        dataSetInstanceSummary: DataSetInstanceSummary,
        recordCount: Int,
        dataSetLabel: String,
        metadataIconData: MetadataIconData,
    ): ProgramUiModel {
        return ProgramUiModel(
            uid = dataSetInstanceSummary.dataSetUid(),
            title = dataSetInstanceSummary.dataSetDisplayName(),
            metadataIconData = metadataIconData,
            count = recordCount,
            type = null,
            typeName = dataSetLabel,
            programType = "",
            description = dataSet.description(),
            onlyEnrollOnce = false,
            accessDataWrite = dataSet.access().data().write(),
            state = dataSetInstanceSummary.state(),
            downloadState = ProgramDownloadState.NONE,
            stockConfig = null,
            lastUpdated = dataSet.lastUpdated() ?: Date(),
        )
    }

    fun map(
        programUiModel: ProgramUiModel,
        downloadState: ProgramDownloadState,
    ): ProgramUiModel {
        return programUiModel.copy(
            downloadState = downloadState,
        )
    }
}
