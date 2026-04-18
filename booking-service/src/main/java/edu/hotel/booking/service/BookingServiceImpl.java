package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingCreateResponse;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.entity.*;
import edu.hotel.booking.exception.NotAvailableRoomsException;
import edu.hotel.booking.kafka.BookingEventProducer;
import edu.hotel.booking.mapper.BookingMapper;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.repository.*;
import edu.hotel.common.exception.AccessDeniedException;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.common.model.KafkaTopics;
import edu.hotel.events.BookingCancelledEvent;
import edu.hotel.events.BookingCompletedEvent;
import edu.hotel.events.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    private final BookingMapper bookingMapper;

    private final GuestRepository guestRepository;

    private final TariffRepository tariffRepository;

    private final RoomRepository roomRepository;

    private final BookingEventProducer bookingEventProducer;

    private final BookingStatusHistoryService bookingStatusHistoryService;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public BookingCreateResponse create(BookingCreateRequest request, Long userId) {
        Booking booking = bookingMapper.toEntity(request);

        Guest guest = guestRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Гостя с id: " + userId + " не существует"));

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

        evictAvailableCache(room.getRoomType().getHotel().getId());

        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaTopics.BOOKING_CREATED)
                .bookingId(savedBooking.getId())
                .userId(userId)
                .guestId(guest.getId())
                .hotelId(room.getRoomType().getHotel().getId())
                .roomTypeId(room.getRoomType().getId())
                .tariffId(tariff.getId())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .totalPrice(booking.getTotalPrice())
                .currency(booking.getCurrency().name())
                .occurredAt(LocalDateTime.now())
                .build();

        bookingEventProducer.sendBookingCreated(event);

        bookingStatusHistoryService.saveStatusHistory(savedBooking, BookingStatus.PENDING_PAYMENT,
                userId.toString(), "Бронирование создано");

        BookingCreateResponse response = new BookingCreateResponse();
        response.setBookingId(savedBooking.getId());
        response.setPaymentUrl("https://payment.example.com/pay/" + savedBooking.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getById(Long id, Long userId, String role) {

        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (role.equals("ROLE_GUEST") && !booking.getGuest().getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужому бронированию");
        }

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
    @Transactional
    public BookingDetailResponse cancelBooking(Long id, Long userId, String role) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (role.equals("ROLE_GUEST") && !booking.getGuest().getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужому бронированию");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Невозможно отменить бронирование со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking updateBooking = bookingRepository.save(booking);

        evictAvailableCache(booking.getRoom().getRoomType().getHotel().getId());

        BookingCancelledEvent bookingCancelledEvent = BookingCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaTopics.BOOKING_CANCELLED)
                .bookingId(booking.getId())
                .guestId(booking.getGuest().getId())
                .reason("Отменено пользователем")
                .occurredAt(LocalDateTime.now())
                .build();

        bookingEventProducer.sendBookingCancelled(bookingCancelledEvent);

        bookingStatusHistoryService.saveStatusHistory(updateBooking, BookingStatus.CANCELLED,
                userId.toString(), "Бронирование отменено");

        return bookingMapper.toDetailResponse(updateBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse checkInById(Long id, Long userId) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Невозможно отметить заезд для бронирования со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        Booking updateBooking = bookingRepository.save(booking);

        bookingStatusHistoryService.saveStatusHistory(updateBooking, BookingStatus.CHECKED_IN,
                userId.toString(), "Гость заехал");

        return bookingMapper.toDetailResponse(updateBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse checkOutById(Long id, Long userId) {
        Booking booking = bookingRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id: " + id + " не найдено"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Невозможно отметить выезд для бронирования со статусом: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking updateBooking = bookingRepository.save(booking);

        BookingCompletedEvent bookingCompletedEvent = BookingCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaTopics.BOOKING_COMPLETED)
                .bookingId(booking.getId())
                .hotelId(booking.getRoom().getRoomType().getHotel().getId())
                .roomTypeId(booking.getRoom().getRoomType().getId())
                .guestId(booking.getGuest().getId())
                .userId(booking.getGuest().getUserId())
                .occurredAt(LocalDateTime.now())
                .build();

        bookingEventProducer.sendBookingCompleted(bookingCompletedEvent);

        bookingStatusHistoryService.saveStatusHistory(updateBooking, BookingStatus.COMPLETED,
                userId.toString(), "Гость выехал");

        return bookingMapper.toDetailResponse(updateBooking);
    }

    private void evictAvailableCache(Long hotelId) {
        String indexKey = "Available:keys:" + hotelId;
        Set<Object> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null) {
            keys.forEach(key -> redisTemplate.delete(key.toString()));
        }
        redisTemplate.delete(indexKey);
    }
}
