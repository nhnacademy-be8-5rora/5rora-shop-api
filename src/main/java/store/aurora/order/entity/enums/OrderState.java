package store.aurora.order.entity.enums;

public enum OrderState {
    PENDING,    // 대기
    CANCELLED,  // 취소됨
    SHIPPED,    // 배송중
    DELIVERED,  // 배송 완료
    REFUND_PENDING, // 환불 대기
    REFUNDED,   // 환불됨
    CONFIRMED;  // 주문 확정

    public static OrderState fromOrdinal(int ordinal){
        for (OrderState state : values()) {
            if (state.ordinal() == ordinal) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal for OrderState: " + ordinal);
    }
}
