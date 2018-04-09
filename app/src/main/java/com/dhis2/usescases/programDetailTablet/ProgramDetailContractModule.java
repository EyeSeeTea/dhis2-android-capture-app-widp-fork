package com.dhis2.usescases.programDetailTablet;

import com.dhis2.usescases.general.AbstractActivityContracts;
import com.unnamed.b.atv.model.TreeNode;

import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;

import java.util.List;

/**
 * Created by ppajuelo on 31/10/2017.
 *
 */

public class ProgramDetailContractModule {

    public interface View extends AbstractActivityContracts.View {
        void swapData(TrackedEntityObject trackedEntityObject);

        void addTree(TreeNode treeNode);

        void setAttributeOrder(List<ProgramTrackedEntityAttributeModel> programAttributes);

        void setOrgUnitNames(List<OrganisationUnitModel> orgsUnits);

        void openDrawer();

        void showTimeUnitPicker();

        void showRageDatePicker();

        void setProgram(ProgramModel program);
    }

    public interface Presenter extends AbstractActivityContracts.Presenter {
        void init(View view, String programId);

        void onTimeButtonClick();

        void onDateRangeButtonClick();

        void onOrgUnitButtonClick();

        void onCatComboButtonClick();

        ProgramModel getCurrentProgram();

        void onSearchClick();

        void onBackClick();

        void setProgram(ProgramModel program);
    }

    public interface Interactor extends AbstractActivityContracts.Interactor {
        void init(View view, String programId);

        void getOrgUnits();

        void getData(int page);
    }
}
