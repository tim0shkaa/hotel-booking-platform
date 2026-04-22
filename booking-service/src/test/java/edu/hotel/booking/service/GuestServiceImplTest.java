package edu.hotel.booking.service;

import edu.hotel.booking.dto.booking.BookingSummaryResponse;
import edu.hotel.booking.dto.guest.GuestRequest;
import edu.hotel.booking.dto.guest.GuestResponse;
import edu.hotel.booking.entity.Booking;
import edu.hotel.booking.entity.Guest;
import edu.hotel.booking.mapper.BookingMapper;
import edu.hotel.booking.mapper.GuestMapper;
import edu.hotel.booking.repository.BookingRepository;
import edu.hotel.booking.repository.GuestRepository;
import edu.hotel.common.exception.AccessDeniedException;
import edu.hotel.common.exception.AlreadyExistsException;
import edu.hotel.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceImplTest {

    @Mock private GuestRepository guestRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private GuestMapper guestMapper;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks
    private GuestServiceImpl guestService;

    @Test
    void getHistoryBookings_shouldReturnPage_whenGuestAccessesOwnHistory() {
        Long guestId = 1L;
        Long userId = 10L;
        Pageable pageable = Pageable.unpaged();

        Guest guest = new Guest();
        guest.setId(guestId);
        guest.setUserId(userId);

        Booking booking = new Booking();
        Page<Booking> bookingPage = new PageImpl<>(List.of(booking));
        BookingSummaryResponse summaryResponse = new BookingSummaryResponse();

        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(bookingRepository.findByGuestId(guestId, pageable)).thenReturn(bookingPage);
        when(bookingMapper.toSummaryResponse(booking)).thenReturn(summaryResponse);

        Page<BookingSummaryResponse> result = guestService.getHistoryBookings(guestId, userId, "ROLE_GUEST", pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(summaryResponse);
    }

    @Test
    void getHistoryBookings_shouldReturnPage_whenAdminAccessesAnyHistory() {
        Long guestId = 1L;
        Long userId = 99L;
        Pageable pageable = Pageable.unpaged();

        Guest guest = new Guest();
        guest.setId(guestId);
        guest.setUserId(10L);

        Page<Booking> bookingPage = new PageImpl<>(List.of());

        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(bookingRepository.findByGuestId(guestId, pageable)).thenReturn(bookingPage);

        Page<BookingSummaryResponse> result = guestService.getHistoryBookings(guestId, userId, "ROLE_ADMIN", pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getHistoryBookings_shouldThrowAccessDeniedException_whenGuestAccessesAnotherHistory() {
        Long guestId = 1L;
        Long userId = 10L;

        Guest guest = new Guest();
        guest.setId(guestId);
        guest.setUserId(99L);

        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));

        assertThatThrownBy(() -> guestService.getHistoryBookings(guestId, userId, "ROLE_GUEST", Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class);

        verify(bookingRepository, never()).findByGuestId(any(), any());
    }

    @Test
    void getHistoryBookings_shouldThrowNotFoundException_whenGuestNotFound() {
        when(guestRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestService.getHistoryBookings(1L, 1L, "ROLE_GUEST", Pageable.unpaged()))
                .isInstanceOf(NotFoundException.class);

        verify(bookingRepository, never()).findByGuestId(any(), any());
    }

    @Test
    void create_shouldReturnResponse_whenGuestNotExists() {
        Long userId = 10L;
        GuestRequest request = new GuestRequest();
        Guest guest = new Guest();
        GuestResponse response = new GuestResponse();

        when(guestRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(guestMapper.toEntity(request)).thenReturn(guest);
        when(guestMapper.toResponse(guest)).thenReturn(response);

        GuestResponse result = guestService.create(userId, request);

        assertThat(result).isEqualTo(response);
        assertThat(guest.getUserId()).isEqualTo(userId);
        verify(guestRepository).save(guest);
    }

    @Test
    void create_shouldThrowAlreadyExistsException_whenGuestAlreadyExists() {
        Long userId = 10L;

        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(new Guest()));

        assertThatThrownBy(() -> guestService.create(userId, new GuestRequest()))
                .isInstanceOf(AlreadyExistsException.class);

        verify(guestRepository, never()).save(any());
    }

    @Test
    void update_shouldReturnUpdatedResponse_whenGuestExists() {
        Long userId = 10L;
        GuestRequest request = new GuestRequest();

        Guest existedGuest = new Guest();
        existedGuest.setUserId(userId);

        Guest savedGuest = new Guest();
        GuestResponse response = new GuestResponse();

        when(guestRepository.findByUserId(userId)).thenReturn(Optional.of(existedGuest));
        when(guestRepository.save(existedGuest)).thenReturn(savedGuest);
        when(guestMapper.toResponse(savedGuest)).thenReturn(response);

        GuestResponse result = guestService.update(userId, request);

        assertThat(result).isEqualTo(response);
        verify(guestMapper).updateEntityFrom(eq(request), eq(existedGuest));
        verify(guestRepository).save(existedGuest);
    }

    @Test
    void update_shouldThrowNotFoundException_whenGuestNotExists() {
        Long userId = 10L;

        when(guestRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestService.update(userId, new GuestRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(guestRepository, never()).save(any());
    }
}
