package org.dhis2.data.forms.dataentry.fields;

import org.hisp.dhis.android.core.common.ObjectStyle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class FieldViewModel {

    @NonNull
    public abstract String uid();

    @NonNull
    public abstract String label();

    @NonNull
    public abstract Boolean mandatory();

    @Nullable
    public abstract String value();

    @Nullable
    public abstract String programStageSection();

    @Nullable
    public abstract Boolean allowFutureDate();

    @Nullable
    public abstract Boolean editable();

    @Nullable
    public abstract String optionSet();

    @Nullable
    public abstract String warning();

    @Nullable
    public abstract String error();

    public abstract FieldViewModel setMandatory();

    @NonNull
    public abstract FieldViewModel withWarning(@NonNull String warning);

    @NonNull
    public abstract FieldViewModel withError(@NonNull String error);

    @Nullable
    public abstract String description();

    @NonNull
    public abstract FieldViewModel withValue(String data);

    @NonNull
    public abstract FieldViewModel withEditMode(boolean isEditable);

    @NonNull
    public abstract ObjectStyle objectStyle();

    @Nullable
    public abstract String fieldMask();

    @Nullable
    public abstract String url();

    public String getFormattedLabel() {
        if (mandatory()) {
            return label() + " *";
        } else {
            return label();
        }
    }

    public boolean shouldShowError() {
        return warning() != null || error() != null;
    }

    public String getErrorMessage() {
        if (error() != null) {
            return error();
        } else if (warning() != null) {
            return warning();
        } else {
            return null;
        }
    }
}