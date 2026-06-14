package com.deanflights.booking;

import com.deanflights.booking.dto.BookingResponse;
import com.deanflights.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP layer for bookings. Only concerns: routing, status codes, JSON in/out, and
 * triggering validation (@Valid). It never returns the entity directly — always a DTO.
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.create(request));
    }

    @GetMapping("/{reference}")
    public BookingResponse getByReference(@PathVariable String reference) {
        return BookingResponse.from(bookingService.getByReference(reference));
    }
}
