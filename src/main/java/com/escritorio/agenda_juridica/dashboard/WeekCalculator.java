package com.escritorio.agenda_juridica.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Component;

@Component
public class WeekCalculator {

    public WorkWeek calculate(LocalDate referenceDate) {
        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new WorkWeek(monday, monday.plusDays(4));
    }

    public record WorkWeek(LocalDate start, LocalDate end) {
    }
}
