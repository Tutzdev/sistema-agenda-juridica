package com.escritorio.agenda_juridica.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WeekCalculatorTest {

    @Test
    void calculatesMondayThroughFriday() {
        WeekCalculator.WorkWeek week = new WeekCalculator().calculate(LocalDate.of(2026, 8, 18));

        assertEquals(LocalDate.of(2026, 8, 17), week.start());
        assertEquals(LocalDate.of(2026, 8, 21), week.end());
    }
}
