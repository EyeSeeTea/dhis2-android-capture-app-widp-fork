package org.dhis2.data.forms.dataentry.fields.orgUnit;

import androidx.lifecycle.MutableLiveData;

import org.dhis2.data.forms.dataentry.fields.FormViewHolder;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.databinding.FormOrgUnitBinding;
import io.reactivex.processors.FlowableProcessor;

import static android.text.TextUtils.isEmpty;

/**
 * QUADRAM. Created by ppajuelo on 19/03/2018.
 */

public class OrgUnitHolder extends FormViewHolder {
    private final FormOrgUnitBinding binding;
    private OrgUnitViewModel model;

    OrgUnitHolder(FormOrgUnitBinding binding, FlowableProcessor<RowAction> processor, boolean isSearchMode, MutableLiveData<String> currentSelection) {
        super(binding);
        this.binding = binding;
        this.currentUid = currentSelection;

        binding.orgUnitView.setListener(orgUnitUid -> {
            processor.onNext(RowAction.create(model.uid(), orgUnitUid, getAdapterPosition()));
        });

        binding.orgUnitView.setActivationListener(() -> setSelectedBackground(isSearchMode));
    }

    public void update(OrgUnitViewModel viewModel) {
        this.model = viewModel;
        fieldUid = viewModel.uid();

        String uid_value_name = viewModel.value();
        String ouUid = null;
        String ouName = null;
        if (!isEmpty(uid_value_name)) {
            ouUid = uid_value_name.split("_ou_")[0];
            ouName = uid_value_name.split("_ou_")[1];
        }

        if (model.objectStyle() != null) {
            objectStyle = model.objectStyle();
        }
        binding.orgUnitView.setLabel(viewModel.label(), viewModel.mandatory());
        if (viewModel.url() != null){
            descriptionText = viewModel.description() + "\n" + viewModel.url()  ;
        } else {
            descriptionText = viewModel.description();
        }
        binding.orgUnitView.setDescription(descriptionText);
        binding.orgUnitView.setWarning(viewModel.warning(), viewModel.error());
        binding.orgUnitView.setValue(ouUid, ouName);
        binding.orgUnitView.getEditText().setText(ouName);
        binding.orgUnitView.updateEditable(viewModel.editable());
        label = new StringBuilder().append(viewModel.label());

        initFieldFocus();
    }
}
