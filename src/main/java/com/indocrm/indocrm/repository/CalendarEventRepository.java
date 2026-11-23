package com.indocrm.indocrm.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.indocrm.indocrm.model.CalendarEvent;

@Repository
public interface CalendarEventRepository
        extends MongoRepository<CalendarEvent, String> {

    @Query(
        value = "{ " +
                "'active': true, " +
                "'date': { '$gte': ?0, '$lte': ?1 }" +
                " }",
        sort = "{ 'date': 1 }"
    )
    List<CalendarEvent> findActiveEventsBetweenDates(
            LocalDate from,
            LocalDate to
    );

    List<CalendarEvent> findByActiveTrueOrderByDateAsc();

    List<CalendarEvent> findByActiveTrueAndDateGreaterThanEqualOrderByDateAsc(
            LocalDate date
    );

    List<CalendarEvent> findByActiveTrueAndDateLessThanEqualOrderByDateAsc(
            LocalDate date
    );
}