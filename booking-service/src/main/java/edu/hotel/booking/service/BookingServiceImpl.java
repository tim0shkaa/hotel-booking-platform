package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingCreateResponse;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.entity.Guest;
import edu.hotel.booking.entity.Room;
import edu.hotel.booking.entity.Tariff;
import edu.hotel.booking.exception.NotAvailableRoomsException;
import edu.hotel.booking.mapper.BookingMapper;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.repository.BookingRepository;
import edu.hotel.booking.repository.GuestRepository;
import edu.hotel.booking.repository.RoomRepository;
import edu.hotel.booking.repository.TariffRepository;
import edu.hotel.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    private final BookingMapper bookingMapper;

    private final GuestRepository guestRepository;

    private final TariffRepository tariffRepository;

    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public BookingCreateResponse create(BookingCreateRequest request, Long guestId) {
        Booking booking = bookingMapper.toEntity(request);

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new NotFoundException("Гость с id: " + guestId + " не найден"));

        Tariff tariff = tariffRepository.findByIdAndRoomTypeId(request.getTariffId(), request.getRoomTypeId())
                .orElseThrow(() -> new NotFoundException("Тариф с id: " + request.getTariffId() + " не найден или не принадлежит данному типу номера"));

        Room room = roomRepository.findFirstAvailableRoom(request.getRoomTypeId(), request.getCheckIn(), request.getCheckOut())
                .orElseThrow(() -> new NotAvailableRoomsException(
                        "Нет доступных номеров для типа " + request.getRoomTypeId() + " на даты: "
                                + request.getCheckIn() + " - " + request.getCheckOut()
                ));

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = tariff.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        booking.setTariff(tariff);
        booking.setTotalPrice(totalPrice);
        booking.setCurrency(tariff.getCurrency());
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        Booking savedBooking = bookingRepository.save(booking);

        BookingCreateResponse response = new BookingCreateResponse();
        response.setBookingId(savedBooking.getId());
        response.setPaymentUrl("https://payment.example.com/pay/" + savedBooking.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getById(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        return bookingMapper.toDetailResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getBookingWithFilters(Long hotelId, BookingStatus status, LocalDate checkIn, LocalDate checkOut, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;

        Page<Booking> bookings = bookingRepository.findAllWithFilters(hotelId, statusStr, checkIn, checkOut, pageable);

        return bookings.map(bookingMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getHistoryBookings(Long guestId, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findByGuestId(guestId, pageable);
        return bookings.map(bookingMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public BookingDetailResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Невозможно отменить бронирование со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updateBooking = bookingRepository.save(booking);
        return bookingMapper.toDetailResponse(updateBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse checkInById(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Невозможно отметить заезд для бронирования со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        Booking updateBooking = bookingRepository.save(booking);
        return bookingMapper.toDetailResponse(updateBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse checkOutById(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Невозможно отметить выезд для бронирования со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking updateBooking = bookingRepository.save(booking);
        return bookingMapper.toDetailResponse(updateBooking);
    }
}
