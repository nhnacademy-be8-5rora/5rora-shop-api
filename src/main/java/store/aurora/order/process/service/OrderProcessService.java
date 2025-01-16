package store.aurora.order.process.service;

import store.aurora.order.dto.*;
import store.aurora.order.entity.Order;

import java.util.List;

public interface OrderProcessService {

    /**
     * 주문 번호 uuid 생성
     * @return uuid 주문 번호
     */
    String getOrderUuid();

    /**
     * 주문 상세 정보를 통해 주문 총 금액 계산 (결제 금액은 포인트 사용량 포함해야 함)
     * @param orderDetailList 주문 상세 정보
     * @return totalAmount 총 금액
     */
    int getTotalAmountFromOrderDetailList(List<OrderDetailDTO> orderDetailList);

    /**
     * 주문 정보를 uuid를 key로 하여 redis에 저장
     * @param uuid 주문 번호
     * @param orderInfo 주문 정보
     */
    void saveOrderInfoInRedisWithUuid(String uuid, OrderRequestDto orderInfo);

    /**
     * uuid를 key로 하여 redis에서 주문 정보를 가져옴
     * OrderResponseDto 생성
     * <pre>
     *     <b>customerKey</b>   : UUID 생성
     *     <b>currency</b>      : 통화 ( KRW )
     *     <b>amount</b>        : OrderDetailDtoList를 통해서 생성
     *     <b>orderName</b>     : OrderDetailDtoList를 통해서 생성
     * </pre>
     * <pre>
     *     <b>확장상 고려</b>      : 통화 정보 (currency) parameter로 받아서 생성
     * </pre>
     * @param uuid 주문 번호 ( redis-key )
     * @return responseDto
     */
    OrderResponseDto getOrderResponseFromOrderRequestDtoInRedis(String uuid);

    /**
     * 회원 주문 내역 저장
     * @param redisOrderId 레디스 저장된 정보의 키
     * @param paymentKey 결제 키
     * @param amount 결제 금액
     * @return order 주문 엔티티
     */
    Order userOrderProcess(String redisOrderId, String paymentKey, int amount);

    /**
     * 비회원 주문 내역 저장
     * @param redisOrderId 레디스 저장된 정보의 키
     * @param paymentKey 결제 키
     * @param amount 결제 금액
     */
    Long nonUserOrderProcess(String redisOrderId, String paymentKey, int amount);
}
