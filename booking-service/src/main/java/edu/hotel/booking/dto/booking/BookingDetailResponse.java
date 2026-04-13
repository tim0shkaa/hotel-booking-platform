package edu.hotel.booking.dto.booking;

import edu.hotel.booking.dto.guest.GuestResponse;
import edu.hotel.booking.dto.room.RoomSummaryResponse;
import edu.hotel.booking.dto.audit.BookingStatusHistoryResponse;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.model.Currency;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingDetailResponse {

    private Long id;

    private RoomSummaryResponse room;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private BookingStatus status;

    private BigDecimal totalPrice;

    private Currency currency;

    private GuestResponse guest;

    private String notes;

    private LocalDateTime createdAt;

    private List<BookingStatusHistoryResponse> statusHistory;
}
