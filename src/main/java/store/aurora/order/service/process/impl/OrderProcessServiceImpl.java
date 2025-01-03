package store.aurora.order.service.process.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.aurora.book.service.BookService;
import store.aurora.order.dto.OrderDTO;
import store.aurora.order.dto.OrderDetailDTO;
import store.aurora.order.dto.OrderedPersonInfoDTO;
import store.aurora.order.dto.ReceiverInfoDTO;
import store.aurora.order.entity.Order;
import store.aurora.order.entity.OrderDetail;
import store.aurora.order.entity.Shipment;
import store.aurora.order.entity.ShipmentInformation;
import store.aurora.order.entity.enums.OrderState;
import store.aurora.order.entity.enums.ShipmentState;
import store.aurora.order.service.*;
import store.aurora.order.service.process.OrderProcessService;
import store.aurora.user.entity.User;
import store.aurora.user.service.UserService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessServiceImpl implements OrderProcessService {
    private final BookService bookService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final ShipmentInformationService shipmentInformationService;
    private final ShipmentService shipmentService;
    private final WrapService wrapService;
    private final UserService userService;

    /* todo 배송비 로직 수정
        이 로직으로는 배송비 정책을 수정할 때마다 코드를 수정하고, 서버를 재배포해야 한다.
        그래서 setting 테이블을 작성하여 관리하고자 함

        1단계. setting 테이블에 배송비 관련 정보를 저장하고, 배송비를 가져오는 로직으로 수정

        2단계. 매 주문마다 배송비를 setting에서 불러오는 것이 아닌 캐싱하여 사용
            2-1. 특정 시간마다 배송비를 캐싱하고, 배송비를 가져올 때 캐싱된 값을 사용
            2-2. 배송비 관련 정보가 변경되었을 때 캐싱된 값을 삭제하고, 새로운 값을 캐싱
     */
    // todo public이어야 할 필요에 대한 고려 ( public이 아닌 private로 변경해도 되는지 확인 필요 )
    /**
     * 배송비 계산
     * 배송비 정책에 의거해 배송비 계산
     * <pre> 현재: 총 주문 금액이 30,000원 이상일 경우 배송비 무료 </pre>
     * <pre>
     *      배송비 정책이 수정될 경우를 고려해 setting 테이블에서 배송비 관련 정보를 가져오도록 수정해야 함
     *      to_do 참고
     * </pre>
     * @param totalAmount 쿠폰을 적용한 총 주문 금액 ( 포인트 적용 전 )
     * @return 배송비
     */
    @Override
    public int getDeliveryFee(int totalAmount) {
        int minAmount = 30000;

        int deliveryFee = 5000;
        if(totalAmount >= minAmount){
            deliveryFee = 0;
        }

        return deliveryFee;
    }

    // todo public이어야 할 필요에 대한 고려 ( public이 아닌 private로 변경해도 되는지 확인 필요 )
    @Override
    public int getTotalAmountFromOrderDetailList(List<OrderDetailDTO> orderDetailList) {
        int totalAmount = 0;
        for (OrderDetailDTO orderDetail : orderDetailList) {
            int amount = bookService.getBookById(orderDetail.getBookId()).getSalePrice() * orderDetail.getQuantity();
            totalAmount += amount - orderDetail.getDiscountAmount();
        }
        return totalAmount;
    }

    // todo Payment 처리 로직 추가
    // todo nonUserProcess 작성 후 중복 코드 메소드화
    /**
     * 사용자 주문 처리
     * @param order OrderDTO 주문 정보
     * @param orderDetailList List<OrderDetailDTO> 주문 상세 정보
     * @param receiverInfo ReceiverInfoDTO 수령인 정보
     * @param user User 사용자 정보
     * @param orderedPersonInfo OrderedPersonInfoDTO 주문자 정보
     */
    @Override
    public void userOrderProcess(OrderDTO order,
                                 List<OrderDetailDTO> orderDetailList,
                                 ReceiverInfoDTO receiverInfo,
                                 User user,
                                 OrderedPersonInfoDTO orderedPersonInfo){

        int totalAmount = getTotalAmountFromOrderDetailList(orderDetailList);
        Order newOrder = Order.builder()
                .deliveryFee(getDeliveryFee(totalAmount))
                .orderTime(order.getOrderTime())
                .totalAmount(0)
                .pointAmount(order.getPointAmount())
                .state(OrderState.PENDING)
                .name(orderedPersonInfo.getName())
                .orderPhone(orderedPersonInfo.getPhone())
                .preferredDeliveryDate(order.getPreferredDeliveryDate())
                .user(user)
                .build();

        orderSuccessProcess(orderService.createOrder(newOrder), orderDetailList, receiverInfo);
    }

    @Override
    public void nonUserOrderProcess(OrderDTO order,
                                    List<OrderDetailDTO> orderDetailList,
                                    ReceiverInfoDTO receiverInfo,
                                    OrderedPersonInfoDTO orderedPersonInfo){

        int totalAmount = getTotalAmountFromOrderDetailList(orderDetailList);
        Order newOrder = Order.builder()
                .deliveryFee(getDeliveryFee(totalAmount))
                .orderTime(order.getOrderTime())
                .totalAmount(0)
                .pointAmount(order.getPointAmount())
                .state(OrderState.PENDING)
                .name(orderedPersonInfo.getName())
                .orderPhone(orderedPersonInfo.getPhone())
                .preferredDeliveryDate(order.getPreferredDeliveryDate())
                .password(orderedPersonInfo.getPassword())
                .build();

        orderSuccessProcess(orderService.createOrder(newOrder), orderDetailList, receiverInfo);
    }

    private void orderSuccessProcess(Order order,
                                     List<OrderDetailDTO> orderDetailList,
                                     ReceiverInfoDTO receiverInfo){

        Order createdOrder = orderService.createOrder(order);

        Shipment shipment = Shipment.builder()
                .state(ShipmentState.PENDING)
                .build();
        shipment = shipmentService.createShipment(shipment);

        for (OrderDetailDTO detailDTO : orderDetailList) {
            OrderDetail detail = OrderDetail.builder()
                    .order(createdOrder)
                    .state(createdOrder.getState())
                    .amountDetail(bookService.getBookById(detailDTO.getBookId()).getSalePrice() * detailDTO.getQuantity())
                    .quantity(detailDTO.getQuantity())
                    .wrap(wrapService.getWrap(detailDTO.getWrapId()))
                    .couponId(detailDTO.getCouponId())
                    .book(bookService.getBookById(detailDTO.getBookId()))
                    .shipment(shipment)
                    .build();

            orderDetailService.createOrderDetail(detail);
        }

        ShipmentInformation info = ShipmentInformation.builder()
                .order(createdOrder)
                .receiverName(receiverInfo.getName())
                .receiverPhone(receiverInfo.getPhone())
                .receiverAddress(receiverInfo.getAddress())
                .build();

        shipmentInformationService.createShipmentInformation(info);
    }
}
