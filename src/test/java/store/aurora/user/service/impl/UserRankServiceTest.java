package store.aurora.user.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import store.aurora.order.entity.Order;
import store.aurora.order.entity.enums.OrderState;
import store.aurora.order.repository.OrderDetailRepository;
import store.aurora.user.entity.Rank;
import store.aurora.user.entity.User;
import store.aurora.user.entity.UserRank;
import store.aurora.user.entity.UserRankHistory;
import store.aurora.user.repository.UserRankHistoryRepository;
import store.aurora.user.repository.UserRankRepository;
import store.aurora.user.repository.UserRepository;
import store.aurora.user.service.impl.UserRankService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;


class UserRankServiceTest {

    @InjectMocks
    private UserRankService userRankService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRankRepository userRankRepository;

    @Mock
    private UserRankHistoryRepository userRankHistoryRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private User user;

    @Mock
    private Order order;

    @Mock
    private UserRank userRank;

    @Mock
    private UserRankHistory userRankHistory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCurrentPointRateByUserId() {
        String userId = "user123";
        BigDecimal expectedPointRate = BigDecimal.valueOf(1.2);

        when(userRankHistoryRepository.findTopByUserIdOrderByChangedAtDesc(userId))
                .thenReturn(Optional.of(userRankHistory));
        when(userRankHistory.getUserRank()).thenReturn(userRank);
        when(userRank.getPointRate()).thenReturn(expectedPointRate);

        Optional<BigDecimal> pointRate = userRankService.getCurrentPointRateByUserId(userId);

        assertTrue(pointRate.isPresent());
        assertEquals(expectedPointRate, pointRate.get());
    }

    @Test
    void testGetAllUserRanks() {
        // Arrange
        UserRank rank1 = new UserRank(1L, Rank.GENERAL, 0, 1000, BigDecimal.valueOf(0.01));
        UserRank rank2 = new UserRank(2L, Rank.PLATINUM, 1001, 5000, BigDecimal.valueOf(0.02));
        List<UserRank> mockRanks = Arrays.asList(rank1, rank2);

        when(userRankRepository.findAll()).thenReturn(mockRanks);

        // Act
        List<UserRank> result = userRankService.getAllUserRanks();

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2);

        assertThat(result.get(0))
                .hasFieldOrPropertyWithValue("rankName", Rank.GENERAL)
                .hasFieldOrPropertyWithValue("minAmount", 0)
                .hasFieldOrPropertyWithValue("maxAmount", 1000)
                .hasFieldOrPropertyWithValue("pointRate", BigDecimal.valueOf(0.01));

        assertThat(result.get(1))
                .hasFieldOrPropertyWithValue("rankName", Rank.PLATINUM)
                .hasFieldOrPropertyWithValue("minAmount", 1001)
                .hasFieldOrPropertyWithValue("maxAmount", 5000)
                .hasFieldOrPropertyWithValue("pointRate", BigDecimal.valueOf(0.02));

        verify(userRankRepository, times(1)).findAll();
    }

    @Test
    void testUpdateUserRanks() {
        // Arrange
        User user1 = new User();
        user1.setId("user123");
        user1.setOrders(Collections.singletonList(order)); // 설정한 주문목록

        User user2 = new User();
        user2.setId("user456");
        user2.setOrders(Collections.singletonList(order));

        List<User> mockUsers = Arrays.asList(user1, user2);

        UserRank rank1 = new UserRank(1L, Rank.GENERAL, 0, 1000, BigDecimal.valueOf(0.01));
        UserRank rank2 = new UserRank(2L, Rank.PLATINUM, 1001, 5000, BigDecimal.valueOf(0.02));
        List<UserRank> mockRanks = Arrays.asList(rank1, rank2);

        when(userRepository.findAll()).thenReturn(mockUsers);
        when(userRankRepository.findAll()).thenReturn(mockRanks);
        when(order.getOrderTime()).thenReturn(LocalDateTime.now().minusMonths(1));  // 3개월 이내의 주문 설정
        when(order.getState()).thenReturn(OrderState.CONFIRMED);
        when(order.getTotalAmount()).thenReturn(500);  // 예시 순수 금액
        when(orderDetailRepository.calculateTotalWrapCostByOrderId(anyLong())).thenReturn(50); // 예시 랩 비용

        Optional<UserRankHistory> latestRankHistoryOpt = Optional.of(userRankHistory);
        when(userRankHistoryRepository.findTopByUserIdOrderByChangedAtDesc("user123"))
                .thenReturn(latestRankHistoryOpt);
        when(userRankHistory.getUserRank()).thenReturn(rank1); // 기존의 등급

        // Act
        userRankService.updateUserRanks();

        // Assert
        verify(userRankHistoryRepository, times(1)).save(any(UserRankHistory.class)); // 기록이 추가된 것 확인
        verify(userRepository, times(1)).findAll(); // 모든 사용자 조회 확인
    }


}
