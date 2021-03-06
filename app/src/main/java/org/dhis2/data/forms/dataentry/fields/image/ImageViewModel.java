package org.dhis2.data.forms.dataentry.fields.image;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.hisp.dhis.android.core.common.ObjectStyle;

@AutoValue
public abstract class ImageViewModel extends FieldViewModel {

    public static final String NAME_CODE_DELIMITATOR = "_op_";

    public static ImageViewModel create(String id, String label, String optionSet, String value, String section, Boolean editable, Boolean mandatory, String description, ObjectStyle objectStyle, String url) {
        return new AutoValue_ImageViewModel(id, label, mandatory, value, section, true, editable, optionSet, null, null, description, objectStyle, null, url);
    }

    @Override
    public FieldViewModel setMandatory() {
        return new AutoValue_ImageViewModel(uid(), label(), true, value(), programStageSection(),
                allowFutureDate(), editable(), optionSet(), warning(), error(), description(), objectStyle(), null, url());
    }

    @NonNull
    @Override
    public FieldViewModel withError(@NonNull String error) {
        return new AutoValue_ImageViewModel(uid(), label(), mandatory(), value(), programStageSection(),
                allowFutureDate(), editable(), optionSet(), warning(), error, description(), objectStyle(), null, url());
    }

    @NonNull
    @Override
    public FieldViewModel withWarning(@NonNull String warning) {
        return new AutoValue_ImageViewModel(uid(), label(), mandatory(), value(), programStageSection(),
                allowFutureDate(), editable(), optionSet(), warning, error(), description(), objectStyle(), null, url());
    }

    @NonNull
    @Override
    public FieldViewModel withValue(String data) {
        return new AutoValue_ImageViewModel(uid(), label(), mandatory(), data, programStageSection(),
                allowFutureDate(), false, optionSet(), warning(), error(), description(), objectStyle(), null, url());
    }

    @NonNull
    @Override
    public FieldViewModel withEditMode(boolean isEditable) {
        return new AutoValue_ImageViewModel(uid(), label(), mandatory(), value(), programStageSection(),
                allowFutureDate(), isEditable, optionSet(), warning(), error(), description(), objectStyle(), null, url());
    }

    public String fieldUid() {
        return uid().split("\\.")[0];
    }

    public String optionUid() {
        return uid().split("\\.")[1];
    }

    public String fieldDisplayName() {
        return label().split(NAME_CODE_DELIMITATOR)[0];
    }

    public String optionDisplayName() {
        return label().split(NAME_CODE_DELIMITATOR)[1];
    }

    public String optionCode() {
        return label().split(NAME_CODE_DELIMITATOR)[2];
    }

    @Override
    public String getFormattedLabel() {
        if (mandatory()) {
            return optionDisplayName() + " *";
        } else {
            return optionDisplayName();
        }
    }
}
