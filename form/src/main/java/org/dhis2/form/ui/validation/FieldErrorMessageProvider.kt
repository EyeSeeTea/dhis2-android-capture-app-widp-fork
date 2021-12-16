package org.dhis2.form.ui.validation

import android.content.Context
import org.dhis2.form.R
import org.dhis2.form.ui.validation.failures.FieldMaskFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.EmailFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.IntegerFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.IntegerNegativeFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.IntegerPositiveFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.IntegerZeroOrPositiveFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.NumberFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.PercentageFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.PhoneNumberFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.UnitIntervalFailure
import org.hisp.dhis.android.core.common.valuetype.validation.failures.UrlFailure

class FieldErrorMessageProvider(private val context: Context) {

    fun getFriendlyErrorMessage(error: Throwable) =
        context.getString(parseErrorToMessageResource(error))

    private fun parseErrorToMessageResource(error: Throwable) =
        when (error) {
            is PhoneNumberFailure -> getPhoneNumberError(error)
            is EmailFailure -> getEmailError(error)
            is IntegerNegativeFailure -> getIntegerNegativeError(error)
            is IntegerZeroOrPositiveFailure -> getIntegerZeroOrPositiveError(error)
            is IntegerPositiveFailure -> getIntegerPositiveError(error)
            is UnitIntervalFailure -> getUnitIntervalFailure(error)
            is PercentageFailure -> getPercentageError(error)
            is UrlFailure -> getUrlError(error)
            is IntegerFailure -> getIntegerError(error)
            is NumberFailure -> getNumberError(error)
            is FieldMaskFailure -> getFieldMaskError(error)
            else -> R.string.invalid_field
        }

    private fun getFieldMaskError(error: FieldMaskFailure) =
        when (error) {
            FieldMaskFailure.WrongPatternException -> R.string.wrong_pattern
        }

    private fun getPhoneNumberError(error: PhoneNumberFailure) =
        when (error) {
            PhoneNumberFailure.MalformedPhoneNumberException -> R.string.invalid_phone_number
        }

    private fun getEmailError(error: EmailFailure) =
        when (error) {
            EmailFailure.MalformedEmailException -> R.string.invalid_email
        }

    private fun getIntegerNegativeError(error: IntegerNegativeFailure) =
        when (error) {
            IntegerNegativeFailure.IntegerOverflow -> R.string.formatting_error
            IntegerNegativeFailure.NumberFormatException -> R.string.formatting_error
            IntegerNegativeFailure.ValueIsPositive -> R.string.invalid_negative_number
            IntegerNegativeFailure.ValueIsZero -> R.string.invalid_negative_number
        }

    private fun getIntegerZeroOrPositiveError(error: IntegerZeroOrPositiveFailure) =
        when (error) {
            IntegerZeroOrPositiveFailure.IntegerOverflow -> R.string.formatting_error
            IntegerZeroOrPositiveFailure.NumberFormatException -> R.string.formatting_error
            IntegerZeroOrPositiveFailure.ValueIsNegative -> R.string.invalid_possitive_zero
        }

    private fun getIntegerPositiveError(error: IntegerPositiveFailure) =
        when (error) {
            IntegerPositiveFailure.IntegerOverflow -> R.string.formatting_error
            IntegerPositiveFailure.NumberFormatException -> R.string.formatting_error
            IntegerPositiveFailure.ValueIsNegative -> R.string.invalid_possitive
            IntegerPositiveFailure.ValueIsZero -> R.string.invalid_possitive
        }

    private fun getUnitIntervalFailure(error: UnitIntervalFailure) =
        when (error) {
            UnitIntervalFailure.GreaterThanOneException -> R.string.invalid_interval
            UnitIntervalFailure.NumberFormatException -> R.string.formatting_error
            UnitIntervalFailure.ScientificNotationException -> R.string.formatting_error
            UnitIntervalFailure.SmallerThanZeroException -> R.string.invalid_interval
        }

    private fun getPercentageError(error: PercentageFailure) = when (error) {
        PercentageFailure.NumberFormatException -> R.string.formatting_error
        PercentageFailure.ValueGreaterThan100 -> R.string.invalid_percentage
        PercentageFailure.ValueIsNegative -> R.string.invalid_possitive
    }

    private fun getUrlError(error: UrlFailure) =
        when (error) {
            UrlFailure.MalformedUrlException -> R.string.validation_url
        }

    private fun getIntegerError(error: IntegerFailure) =
        when (error) {
            IntegerFailure.IntegerOverflow -> R.string.formatting_error
            IntegerFailure.NumberFormatException -> R.string.invalid_integer
        }

    private fun getNumberError(error: NumberFailure) =
        when (error) {
            NumberFailure.NumberFormatException -> R.string.formatting_error
            NumberFailure.ScientificNotationException -> R.string.formatting_error
        }
}
