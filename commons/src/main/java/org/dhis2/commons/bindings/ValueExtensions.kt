package org.dhis2.commons.bindings

import org.dhis2.commons.date.DateUtils
import org.dhis2.commons.extensions.invoke
import org.dhis2.commons.extensions.toPercentage
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueObjectRepository
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueObjectRepository

fun TrackedEntityAttributeValue.userFriendlyValue(
    d2: D2,
    addPercentageSymbol: Boolean = true,
): String? {
    if (value().isNullOrEmpty()) {
        return value()
    }

    val attribute = d2.trackedEntityModule().trackedEntityAttributes()
        .uid(trackedEntityAttribute())
        .blockingGet()

    if (attribute == null) {
        return value()
    }

    if (check(d2, attribute.valueType(), attribute.optionSet()?.uid(), value()!!)) {
        attribute.optionSet()?.takeIf { attribute.valueType() != ValueType.MULTI_TEXT }?.let {
            return checkOptionSetValue(d2, it.uid(), value()!!)
        } ?: return checkValueTypeValue(d2, attribute.valueType(), value()!!, addPercentageSymbol)
    } else {
        return null
    }
}

fun TrackedEntityDataValue?.userFriendlyValue(
    d2: D2,
    addPercentageSymbol: Boolean = true,
): String? {
    if (this == null) return null

    if (value().isNullOrEmpty()) {
        return value()
    }

    val dataElement = d2.dataElementModule().dataElements()
        .uid(dataElement())
        .blockingGet()

    if (dataElement == null) {
        return null
    } else if (check(d2, dataElement.valueType(), dataElement.optionSet()?.uid(), value()!!)) {
        dataElement.optionSet()?.takeIf { dataElement.valueType() != ValueType.MULTI_TEXT }?.let {
            return checkOptionSetValue(d2, it.uid(), value()!!)
        } ?: return checkValueTypeValue(d2, dataElement.valueType(), value()!!, addPercentageSymbol)
    } else {
        return null
    }
}

fun TrackedEntityDataValue?.valueByPropName(d2: D2, propName: String): String? {
    if (this == null) {
        return null
    } else {
        if (value().isNullOrEmpty()) {
            return value()
        }


        val dataElement = d2.dataElementModule().dataElements()
            .uid(dataElement())
            .blockingGet()

        return dataElement?.let { dataElement ->
            if (check(d2, dataElement.valueType(), dataElement.optionSet()?.uid(), value()!!)) {
                dataElement.optionSet()?.let {
                    return checkOptionSetValueByPropName(d2, it.uid(), value()!!, propName)
                } ?: return checkValueTypeValue(d2, dataElement.valueType(), value()!!)
            } else {
                return null
            }
        }
    }
}

fun checkOptionSetValue(d2: D2, optionSetUid: String, code: String): String? {
    return d2.optionModule().options()
        .byOptionSetUid().eq(optionSetUid)
        .byCode().eq(code).one().blockingGet()?.displayName()
}

fun checkOptionSetValueByPropName(
    d2: D2,
    optionSetUid: String,
    code: String,
    propName: String
): String? {
    val option = d2.optionModule().options()
        .byOptionSetUid().eq(optionSetUid)
        .byCode().eq(code).one().blockingGet()

    return option?.let {
        return try {
            option.invoke(propName) as String
        } catch (e: Exception) {
            option.displayName()
        }
    }
}

fun checkValueTypeValue(
    d2: D2,
    valueType: ValueType?,
    value: String,
    addPercentageSymbol: Boolean = true,
): String {
    return when (valueType) {
        ValueType.ORGANISATION_UNIT ->
            d2.organisationUnitModule().organisationUnits()
                .uid(value)
                .blockingGet()
                ?.displayName() ?: value

        ValueType.IMAGE, ValueType.FILE_RESOURCE ->
            d2.fileResourceModule().fileResources().uid(value).blockingGet()?.path() ?: ""

        ValueType.DATE, ValueType.AGE ->
            DateUtils.uiDateFormat().format(
                DateUtils.oldUiDateFormat().parse(value) ?: "",
            )

        ValueType.DATETIME ->
            DateUtils.uiDateTimeFormat().format(
                DateUtils.databaseDateFormatNoSeconds().parse(value) ?: "",
            )

        ValueType.TIME ->
            DateUtils.timeFormat().format(
                DateUtils.timeFormat().parse(value) ?: "",
            )

        ValueType.PERCENTAGE -> {
            if (addPercentageSymbol) {
                value.toPercentage()
            } else {
                value
            }
        }

        else -> value
    }
}

fun TrackedEntityAttributeValueObjectRepository.blockingSetCheck(
    d2: D2,
    attrUid: String,
    value: String,
    onCrash: (attrUid: String, value: String) -> Unit = { _, _ -> },
): Boolean {
    return d2.trackedEntityModule().trackedEntityAttributes().uid(attrUid).blockingGet()?.let {
        if (check(d2, it.valueType(), it.optionSet()?.uid(), value)) {
            val finalValue = assureCodeForOptionSet(d2, it.optionSet()?.uid(), value)
            try {
                blockingSet(finalValue)
            } catch (e: Exception) {
                onCrash(attrUid, value)
                return false
            }
            true
        } else {
            blockingDeleteIfExist()
            false
        }
    } ?: false
}

fun TrackedEntityAttributeValueObjectRepository.blockingGetCheck(
    d2: D2,
    attrUid: String,
): TrackedEntityAttributeValue? {
    return d2.trackedEntityModule().trackedEntityAttributes().uid(attrUid).blockingGet()?.let {
        if (blockingExists() && check(
                d2,
                it.valueType(),
                it.optionSet()?.uid(),
                blockingGet()?.value()!!,
            )
        ) {
            blockingGet()
        } else {
            blockingDeleteIfExist()
            null
        }
    }
}

fun TrackedEntityDataValueObjectRepository.blockingSetCheck(
    d2: D2,
    deUid: String,
    value: String,
): Boolean {
    return d2.dataElementModule().dataElements().uid(deUid).blockingGet()?.let {
        if (check(d2, it.valueType(), it.optionSet()?.uid(), value)) {
            val finalValue = assureCodeForOptionSet(d2, it.optionSet()?.uid(), value)
            blockingSet(finalValue)
            true
        } else {
            blockingDeleteIfExist()
            false
        }
    } ?: false
}

fun String?.withValueTypeCheck(valueType: ValueType?): String? {
    return this?.let {
        if (isEmpty()) return this
        when (valueType) {
            ValueType.PERCENTAGE,
            ValueType.INTEGER,
            ValueType.INTEGER_POSITIVE,
            ValueType.INTEGER_NEGATIVE,
            ValueType.INTEGER_ZERO_OR_POSITIVE,
            -> (
                it.toIntOrNull() ?: it.toFloat().toInt()
                ).toString()

            ValueType.UNIT_INTERVAL -> (it.toIntOrNull() ?: it.toFloat()).toString()
            else -> this
        }
    } ?: this
}

fun TrackedEntityDataValueObjectRepository.blockingGetValueCheck(
    d2: D2,
    deUid: String,
): TrackedEntityDataValue? {
    return d2.dataElementModule().dataElements().uid(deUid).blockingGet()?.let {
        if (blockingExists() && check(
                d2,
                it.valueType(),
                it.optionSet()?.uid(),
                blockingGet()?.value()!!,
            )
        ) {
            blockingGet()
        } else {
            blockingDeleteIfExist()
            null
        }
    }
}

private fun check(d2: D2, valueType: ValueType?, optionSetUid: String?, value: String): Boolean {
    return when {
        valueType != ValueType.MULTI_TEXT && optionSetUid != null -> {
            val optionByCodeExist = d2.optionModule().options().byOptionSetUid().eq(optionSetUid)
                .byCode().eq(value).one().blockingExists()
            val optionByNameExist = d2.optionModule().options().byOptionSetUid().eq(optionSetUid)
                .byDisplayName().eq(value).one().blockingExists()
            optionByCodeExist || optionByNameExist
        }
        valueType != null -> {
            if (valueType.isNumeric) {
                try {
                    value.toFloat().toString()
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                when (valueType) {
                    ValueType.FILE_RESOURCE, ValueType.IMAGE ->
                        d2.fileResourceModule().fileResources()
                            .byUid().eq(value).one().blockingExists()

                    ValueType.ORGANISATION_UNIT ->
                        d2.organisationUnitModule().organisationUnits().uid(value).blockingExists()

                    else -> true
                }
            }
        }

        else -> false
    }
}

private fun assureCodeForOptionSet(d2: D2, optionSetUid: String?, value: String): String {
    return optionSetUid?.let {
        if (d2.optionModule().options()
                .byOptionSetUid().eq(it)
                .byName().eq(value)
                .one().blockingExists()
        ) {
            d2.optionModule().options().byOptionSetUid().eq(it).byName().eq(value).one()
                .blockingGet()?.code()
        } else {
            value
        }
    } ?: value
}
