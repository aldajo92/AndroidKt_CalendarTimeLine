package com.aldajo92.calendarswipeexample

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class CalendarOperationsTest {

    @Test
    fun `when calendar is converted to SimpleDateFormat, given current Calendar, it returns a SimpleDateFormat`() {
        val currentTimestamp = 1667780466266L
        val currentCalendar = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
        }

        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val day = currentCalendar.get(Calendar.DAY_OF_MONTH)

        assertEquals(2022, year)
        assertEquals(10, month)
        assertEquals(6, day)

        val currentSimpleDateModel = currentCalendar.toSimpleDateModel()

        assertEquals(year, currentSimpleDateModel.year)
        assertEquals(month, currentSimpleDateModel.month)
        assertEquals(day, currentSimpleDateModel.dayOfMonth)
        assertEquals(6, currentSimpleDateModel.dayOfWeekIndex)
    }
}
