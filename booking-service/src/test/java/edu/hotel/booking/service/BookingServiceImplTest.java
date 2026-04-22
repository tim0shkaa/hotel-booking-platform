package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingCreateRequest;
import edu.hotel.booking.dto.booking.BookingCreateResponse;
import edu.hotel.booking.dto.booking.BookingDetailResponse;
import edu.hotel.booking.entity.*;
import edu.hotel.booking.exception.NotAvailableRoomsException;
import edu.hotel.booking.kafka.BookingEventProducer;
import edu.hotel.booking.mapper.BookingMapper;
import edu.hotel.booking.model.BookingStatus;
import edu.hotel.booking.model.Currency;
import edu.hotel.booking.repository.BookingRepository;
import edu.hotel.booking.repository.GuestRepository;
import edu.hotel.booking.repository.RoomRepository;
import edu.hotel.booking.repository.TariffRepository;
import edu.hotel.common.exception.AccessDeniedException;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.events.BookingCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private GuestRepository guestRepository;
    @Mock private TariffRepository tariffRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BookingEventProducer bookingEventProducer;
    @Mock private BookingStatusHistoryService bookingStatusHistoryService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SetOperations<String, Object> setOperations;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void create_shouldReturnResponse_whenAllDataValid() {

        Long userId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 4); // 3 ночи

        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomTypeId(10L);
        request.setTariffId(20L);
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);

        Guest guest = new Guest();
        guest.setId(5L);
        guest.setUserId(userId);

        Hotel hotel = new Hotel();
        hotel.setId(100L);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setHotel(hotel);

        Room room = new Room();
        room.setId(50L);
        room.setRoomType(roomType);

        Tariff tariff = new Tariff();
        tariff.setId(20L);
        tariff.setPricePerNight(new BigDecimal("1000.00"));
        tariff.setCurrency(Currency.RUB);

        Booking bookingFromMapper = new Booking();
        bookingFromMapper.setCheckIn(checkIn);
        bookingFromMapper.setCheckOut(checkOut);

        Booking savedBooking = new Booking();
        savedBooking.setId(99L);
        savedBooking.setCheckIn(checkIn);
        savedBooking.setCheckOut(checkOut);
        savedBooking.setGuest(guest);
        savedBooking.setRoom(room);
        savedBooking.setTariff(tariff);
        savedBooking.setTotalPrice(new BigDecimal("3000.00"));
        savedBooking.setCurrency(Currency.RUB);
        savedBooking.setStatus(BookingStatus.PENDING_PAYMENT);

        when(bookingMapper.toEntity(request)).thenReturn(bookingFromMapper);
        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(guest));
        when(tariffRepository.findByIdAndRoomTypeId(20L, 10L)).thenReturn(Optional.of(tariff));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(roomRepository.findFirstAvailableRoom(10L, checkIn, checkOut)).thenReturn(Optional.of(room));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(Set.of());

        BookingCreateResponse response = bookingService.create(request, userId);

        assertThat(response.getBookingId()).isEqualTo(99L);
        assertThat(response.getPaymentUrl()).contains("99");



        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking captured = bookingCaptor.getValue();
        assertThat(captured.getTotalPrice()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(captured.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(captured.getGuest()).isEqualTo(guest);
        assertThat(captured.getRoom()).isEqualTo(room);

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        BookingCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getBookingId()).isEqualTo(99L);
        assertThat(event.getTotalPrice()).isEqualByComparingTo(new BigDecimal("3000.00"));

        verify(bookingStatusHistoryService).saveStatusHistory(
                eq(savedBooking),
                eq(BookingStatus.PENDING_PAYMENT),
                eq(userId.toString()),
                anyString()
        );

        verify(rLock).lock();
        verify(rLock).unlock();
    }

    @Test
    void create_shouldThrowNotFoundException_whenGuestNotFound() {
        Long userId = 99L;
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomTypeId(10L);
        request.setTariffId(20L);
        request.setCheckIn(LocalDate.of(2025, 6, 1));
        request.setCheckOut(LocalDate.of(2025, 6, 4));

        when(bookingMapper.toEntity(request)).thenReturn(new Booking());
        when(guestRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(request, userId))
                .isInstanceOf(NotFoundException.class);

        verify(bookingRepository, never()).save(any());
        verify(bookingEventProducer, never()).sendBookingCreated(any());
    }

    @Test
    void create_shouldThrowNotFoundException_whenTariffNotBelongsToRoomType() {
        Long userId = 1L;
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomTypeId(10L);
        request.setTariffId(20L);
        request.setCheckIn(LocalDate.of(2025, 6, 1));
        request.setCheckOut(LocalDate.of(2025, 6, 4));

        Guest guest = new Guest();
        guest.setId(5L);

        when(bookingMapper.toEntity(request)).thenReturn(new Booking());
        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(guest));
        when(tariffRepository.findByIdAndRoomTypeId(20L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(request, userId))
                .isInstanceOf(NotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotAvailableRoomsException_andUnlockAlways_whenNoRoomsAvailable() {
        Long userId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 4);

        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomTypeId(10L);
        request.setTariffId(20L);
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);

        Guest guest = new Guest();
        guest.setId(5L);

        Tariff tariff = new Tariff();
        tariff.setId(20L);
        tariff.setPricePerNight(new BigDecimal("1000.00"));
        tariff.setCurrency(Currency.RUB);

        when(bookingMapper.toEntity(request)).thenReturn(new Booking());
        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(guest));
        when(tariffRepository.findByIdAndRoomTypeId(20L, 10L)).thenReturn(Optional.of(tariff));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(roomRepository.findFirstAvailableRoom(10L, checkIn, checkOut)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(request, userId))
                .isInstanceOf(NotAvailableRoomsException.class);

        verify(rLock).lock();
        verify(rLock).unlock();
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_shouldCalculateCorrectPrice_whenOneNight() {
        Long userId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 1);
        LocalDate checkOut = LocalDate.of(2025, 6, 2);

        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomTypeId(10L);
        request.setTariffId(20L);
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);

        Guest guest = new Guest();
        guest.setId(5L);
        guest.setUserId(userId);

        Hotel hotel = new Hotel();
        hotel.setId(100L);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setHotel(hotel);

        Room room = new Room();
        room.setId(50L);
        room.setRoomType(roomType);

        Tariff tariff = new Tariff();
        tariff.setId(20L);
        tariff.setPricePerNight(new BigDecimal("1000.00"));
        tariff.setCurrency(Currency.RUB);

        Booking bookingFromMapper = new Booking();
        bookingFromMapper.setCheckIn(checkIn);
        bookingFromMapper.setCheckOut(checkOut);

        Booking savedBooking = new Booking();
        savedBooking.setId(99L);
        savedBooking.setGuest(guest);
        savedBooking.setRoom(room);

        when(bookingMapper.toEntity(request)).thenReturn(bookingFromMapper);
        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(guest));
        when(tariffRepository.findByIdAndRoomTypeId(20L, 10L)).thenReturn(Optional.of(tariff));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(roomRepository.findFirstAvailableRoom(10L, checkIn, checkOut)).thenReturn(Optional.of(room));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(Set.of());

        bookingService.create(request, userId);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void getById_shouldReturnResponse_whenGuestAccessesOwnBooking() {
        Long bookingId = 1L;
        Long userId = 10L;

        Guest guest = new Guest();
        guest.setUserId(userId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setGuest(guest);

        BookingDetailResponse response = new BookingDetailResponse();

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDetailResponse(booking)).thenReturn(response);

        BookingDetailResponse result = bookingService.getById(bookingId, userId, "ROLE_GUEST");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_shouldReturnResponse_whenAdminAccessesAnyBooking() {
        Long bookingId = 1L;
        Long userId = 99L;

        Guest guest = new Guest();
        guest.setUserId(1L);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setGuest(guest);

        BookingDetailResponse response = new BookingDetailResponse();

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDetailResponse(booking)).thenReturn(response);

        BookingDetailResponse result = bookingService.getById(bookingId, userId, "ROLE_ADMIN");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_shouldThrowAccessDeniedException_whenGuestAccessesAnotherBooking() {
        Long bookingId = 1L;
        Long userId = 10L;

        Guest guest = new Guest();
        guest.setUserId(99L);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setGuest(guest);

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getById(bookingId, userId, "ROLE_GUEST"))
                .isInstanceOf(AccessDeniedException.class);

        verify(bookingMapper, never()).toDetailResponse(any());
    }

    @Test
    void getById_shouldThrowNotFoundException_whenBookingNotFound() {
        when(bookingRepository.findDetailById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById(99L, 1L, "ROLE_GUEST"))
                .isInstanceOf(NotFoundException.class);

        verify(bookingMapper, never()).toDetailResponse(any());
    }

    @Test
    void checkInById_shouldUpdateStatus_whenStatusIsConfirmed() {
        Long bookingId = 1L;
        Long userId = 10L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = new Booking();
        savedBooking.setId(bookingId);
        savedBooking.setStatus(BookingStatus.CHECKED_IN);

        BookingDetailResponse response = new BookingDetailResponse();

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toDetailResponse(savedBooking)).thenReturn(response);

        BookingDetailResponse result = bookingService.checkInById(bookingId, userId);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.CHECKED_IN);

        verify(bookingStatusHistoryService).saveStatusHistory(
                eq(savedBooking),
                eq(BookingStatus.CHECKED_IN),
                eq(userId.toString()),
                anyString()
        );
    }

    @Test
    void checkInById_shouldThrowIllegalStateException_whenStatusIsPendingPayment() {
        Long bookingId = 1L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkInById(bookingId, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
        verify(bookingStatusHistoryService, never()).saveStatusHistory(any(), any(), any(), any());
    }

    @Test
    void checkInById_shouldThrowIllegalStateException_whenStatusIsCancelled() {
        Long bookingId = 1L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkInById(bookingId, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
        verify(bookingStatusHistoryService, never()).saveStatusHistory(any(), any(), any(), any());
    }

    @Test
    void checkOutById_shouldUpdateStatus_whenStatusIsCheckedIn() {
        Long bookingId = 1L;
        Long userId = 10L;

        Hotel hotel = new Hotel();
        hotel.setId(100L);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setHotel(hotel);

        Room room = new Room();
        room.setId(50L);
        room.setRoomType(roomType);

        Guest guest = new Guest();
        guest.setId(5L);
        guest.setUserId(userId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setRoom(room);
        booking.setGuest(guest);

        Booking savedBooking = new Booking();
        savedBooking.setId(bookingId);
        savedBooking.setStatus(BookingStatus.COMPLETED);
        savedBooking.setRoom(room);
        savedBooking.setGuest(guest);

        BookingDetailResponse response = new BookingDetailResponse();

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toDetailResponse(savedBooking)).thenReturn(response);

        BookingDetailResponse result = bookingService.checkOutById(bookingId, userId);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.COMPLETED);

        verify(bookingEventProducer).sendBookingCompleted(any());
        verify(bookingStatusHistoryService).saveStatusHistory(
                eq(savedBooking),
                eq(BookingStatus.COMPLETED),
                eq(userId.toString()),
                anyString()
        );
    }

    @Test
    void checkOutById_shouldThrowIllegalStateException_whenStatusIsConfirmed() {
        Long bookingId = 1L;

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findDetailById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkOutById(bookingId, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
        verify(bookingEventProducer, never()).sendBookingCompleted(any());
        verify(bookingStatusHistoryService, never()).saveStatusHistory(any(), any(), any(), any());
    }
}
