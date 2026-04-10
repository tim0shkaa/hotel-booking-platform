package edu.hotel.common.model;

public final class KafkaTopics {

    public static final String BOOKING_CREATED = "booking.created";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKING_COMPLETED = "booking.completed";
    public static final String PAYMENT_CONFIRMED = "payment.confirmed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String REVIEW_CREATED = "review.created";

    public static final String BOOKING_CREATED_DLQ = "booking.created.dlq";
    public static final String BOOKING_CANCELLED_DLQ = "booking.cancelled.dlq";
    public static final String BOOKING_COMPLETED_DLQ = "booking.completed.dlq";
    public static final String PAYMENT_CONFIRMED_DLQ = "payment.confirmed.dlq";
    public static final String PAYMENT_FAILED_DLQ = "payment.failed.dlq";

    private KafkaTopics() {}
}
