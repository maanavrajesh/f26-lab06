package edu.cmu.cs214.booking;

/**
 * Everything needed to create one booking, passed to
 * {@link BookingApi#createBooking(BookingRequest)}.
 *
 * <p>Fields mean exactly what the parameters of the same names meant on the
 * old positional {@code createBooking}: {@code waitlistKey} null declines
 * waitlisting, and {@code notes} null means no notes. Validation happens in
 * {@code createBooking}, not here.
 *
 * @param roomId      the room to book
 * @param startMinute first minute of the booking, inclusive
 * @param endMinute   first minute after the booking, exclusive
 * @param waitlistKey caller's waitlist key, or null to decline waitlisting
 * @param notes       free-form notes, or null for none
 */
public record BookingRequest(String roomId, long startMinute, long endMinute,
                             String waitlistKey, String notes) {
}
