package com.aldajo92.calendarswipeexample

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class SimpleDateModelTest {

    @Test
    fun `when two days in same weeks, then offset returns zero`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 26)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val yesterdaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 27)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(yesterdaySimpleDateModel)
        assertEquals(0, weeksOffset)
    }

    @Test
    fun `when two days is compared, given current day and same day in next week, then offset returns one`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 28)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val afterOneWeekSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 4)
            this.set(Calendar.MONTH, 10)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(afterOneWeekSimpleDateModel)
        assertEquals(1, weeksOffset)
    }

    @Test
    fun `when two days is compared, given current day and same day in from past week, then offset returns minus one`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 28)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val oneWeekBeforeSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 21)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(oneWeekBeforeSimpleDateModel)
        assertEquals(-1, weeksOffset)
    }

    @Test
    fun `when two days consecutive are compared, from different weeks, then offset returns one`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 30)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val oneWeekBeforeSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 31)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(oneWeekBeforeSimpleDateModel)
        assertEquals(1, weeksOffset)
    }

    @Test
    fun `when two days consecutive are compared, from different weeks, then offset returns minus one`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 31)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val oneWeekBeforeSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 30)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(oneWeekBeforeSimpleDateModel)
        assertEquals(-1, weeksOffset)
    }

    @Test
    fun `when two days, with a difference of 3 weeks`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 14)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val oneWeekBeforeSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 31)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(oneWeekBeforeSimpleDateModel)
        assertEquals(3, weeksOffset)
    }

    @Test
    fun `when two days, with a difference of minus 6 weeks`(){
        val todaySimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 28)
            this.set(Calendar.MONTH, 9)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val oneWeekBeforeSimpleDateModel = Calendar.getInstance().apply {
            this.set(Calendar.DAY_OF_MONTH, 16)
            this.set(Calendar.MONTH, 8)
            this.set(Calendar.YEAR, 2022)
        }.toSimpleDateModel()

        val weeksOffset = todaySimpleDateModel.getWeeksOffset(oneWeekBeforeSimpleDateModel)
        assertEquals(-6, weeksOffset)
    }
}
