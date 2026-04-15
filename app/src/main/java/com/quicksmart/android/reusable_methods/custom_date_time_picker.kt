package com.quicksmart.android.reusable_methods

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

//function for show date time dialog
fun Context.showDateTimePicker(
    minDateMillis: Long? = System.currentTimeMillis() - 1000,
    maxDateMillis: Long? = null,
    onResult: (formattedText: String, selected: Calendar) -> Unit
) {
    val now = Calendar.getInstance()

    val datePicker = DatePickerDialog(
        this,
        { _, year, month, day ->
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val formatted = String.format(
                        "%02d / %02d / %04d   %02d : %02d",
                        day, month + 1, year, hour, minute
                    )
                    onResult(formatted, selected)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    )

    minDateMillis?.let { datePicker.datePicker.minDate = it }
    maxDateMillis?.let { datePicker.datePicker.maxDate = it }
    datePicker.show()
}

//function for find days between from two dates
fun daysBetween(from: Calendar, to: Calendar): Int {
    val fromDay = from.clone() as Calendar
    val toDay   = to.clone()   as Calendar

    // Zero out time so we count full calendar days
    listOf(fromDay, toDay).forEach {
        it.set(Calendar.HOUR_OF_DAY, 0)
        it.set(Calendar.MINUTE, 0)
        it.set(Calendar.SECOND, 0)
        it.set(Calendar.MILLISECOND, 0)
    }

    val diffMs   = toDay.timeInMillis - fromDay.timeInMillis
    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
    return if (diffDays < 1) 1 else diffDays
}
