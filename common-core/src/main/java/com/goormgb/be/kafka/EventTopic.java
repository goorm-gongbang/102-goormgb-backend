package com.goormgb.be.kafka;

public final class EventTopic {

    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String ORDER_CANCELLED = "order-cancelled";
    public static final String BANK_TRANSFER_EXPIRED = "bank-transfer-expired";
    public static final String USER_BLOCKED = "user-blocked";

    private EventTopic() {
    }
}
