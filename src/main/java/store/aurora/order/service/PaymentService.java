package store.aurora.order.service;

import store.aurora.order.entity.Payment;

public interface PaymentService {
    Payment createPayment(Payment payment);
    Payment getPaymentById(Long paymentId);
    void deletePaymentById(Long paymentId);
}
