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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final BookingRepository bookingRepository;

    private final GuestRepository guestRepository;

    private final BookingMapper bookingMapper;

    private final GuestMapper guestMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getHistoryBookings(
            Long guestId, Long userId, String role, Pageable pageable) {

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new NotFoundException("Гость с id: " + guestId + " не найден"));

        if (role.equals("ROLE_GUEST") && !guest.getUserId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чужим бронированиям");
        }

        Page<Booking> bookings = bookingRepository.findByGuestId(guestId, pageable);
        return bookings.map(bookingMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public GuestResponse create(Long userId, GuestRequest request) {
        if (guestRepository.findByUserId(userId).isPresent()) {
            throw new AlreadyExistsException("Гость с id: " + userId + " уже существует");
        }

        Guest guest = guestMapper.toEntity(request);
        guest.setUserId(userId);

        guestRepository.save(guest);

        return guestMapper.toResponse(guest);
    }

    @Override
    @Transactional
    public GuestResponse update(Long userId, GuestRequest request) {
        Guest existedGuest = guestRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Гостя с id: " + userId + " не существует"));
        guestMapper.updateEntityFrom(request, existedGuest);
        Guest guest = guestRepository.save(existedGuest);
        return guestMapper.toResponse(guest);
    }
}
