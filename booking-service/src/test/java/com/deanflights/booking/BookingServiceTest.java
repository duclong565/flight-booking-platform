package com.deanflights.booking;

import com.deanflights.booking.dto.CreateBookingRequest;
import com.deanflights.booking.event.BookingCreatedEvent;
import com.deanflights.booking.event.BookingEventPublisher;
import com.deanflights.common.BusinessRuleException;
import com.deanflights.flight.Flight;
import com.deanflights.flight.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit test for {@link BookingService} — no Spring context, no database, no Kafka. All
 * collaborators are mocked so we test the business logic (seat decrement, price calculation,
 * over-booking guard) in isolation.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    FlightRepository flightRepository;

    @Mock
    BookingEventPublisher eventPublisher;

    @InjectMocks
    BookingService bookingService;

    private Flight flightWith(long id, int availableSeats, String basePrice) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNumber("VN-001");
        flight.setOrigin("SGN");
        flight.setDestination("HAN");
        flight.setTotalSeats(availableSeats);
        flight.setAvailableSeats(availableSeats);
        flight.setBasePrice(new BigDecimal(basePrice));
        return flight;
    }

    @Test
    void create_successfulBooking_decrementsSeats_andComputesTotalPrice() {
        Flight flight = flightWith(1L, 10, "150.00");
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        // Return the saved booking as-is so we can assert on it.
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest request =
                new CreateBookingRequest(1L, "Dean Nguyen", "dean@example.com", 3);

        Booking result = bookingService.create(request);

        // Price = basePrice (150.00) * seats (3) = 450.00
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(result.getSeatsBooked()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getBookingReference()).startsWith("BK-");

        // Seats decremented 10 -> 7 and the flight persisted with the new count.
        assertThat(flight.getAvailableSeats()).isEqualTo(7);
        verify(flightRepository).save(flight);
        verify(bookingRepository).save(any(Booking.class));

        // The created event was published with the matching reference.
        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventPublisher).publishBookingCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingReference()).isEqualTo(result.getBookingReference());
        assertThat(eventCaptor.getValue().seatsBooked()).isEqualTo(3);
    }

    @Test
    void create_moreSeatsThanAvailable_throwsBusinessRule_andDoesNotSave() {
        Flight flight = flightWith(2L, 2, "100.00");
        when(flightRepository.findById(2L)).thenReturn(Optional.of(flight));

        CreateBookingRequest request =
                new CreateBookingRequest(2L, "Dean Nguyen", "dean@example.com", 5);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Not enough seats");

        // No booking saved, no seats touched, no event published.
        verify(bookingRepository, never()).save(any());
        verify(flightRepository, never()).save(any());
        verify(eventPublisher, never()).publishBookingCreated(any());
        assertThat(flight.getAvailableSeats()).isEqualTo(2);
    }
}
