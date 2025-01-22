package store.aurora.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.aurora.order.entity.enums.OrderState;
import store.aurora.order.repository.OrderDetailRepository;
import store.aurora.user.entity.User;
import store.aurora.user.entity.UserRank;
import store.aurora.user.entity.UserRankHistory;
import store.aurora.user.repository.UserRankHistoryRepository;
import store.aurora.user.repository.UserRankRepository;
import store.aurora.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserRankService {

    private final UserRepository userRepository;
    private final UserRankRepository userRankRepository;
    private final UserRankHistoryRepository userRankHistoryRepository;
    private final OrderDetailRepository orderDetailRepository;

    public Optional<BigDecimal> getCurrentPointRateByUserId(String userId) {
        return userRankHistoryRepository.findTopByUserIdOrderByChangedAtDesc(userId)
                .map(userRankHistory -> userRankHistory.getUserRank().getPointRate());

    }

    // 3개월 이내의 주문 순수 금액을 기준으로 사용자 등급 업데이트 및 기록
    @Transactional
    public void updateUserRanks() {

        List<User> users = userRepository.findAll();
        List<UserRank> ranks = userRankRepository.findAll();

        for (User user : users) {
            // 3개월 이내의 순수 금액 계산
            Integer pureAmount = calculatePureAmountForLastThreeMonths(user);

            // 새로운 등급 계산
            UserRank newRank = getRankBasedOnAmount(pureAmount, ranks);
            if (newRank == null) {
                continue;
            }

            Optional<UserRankHistory> latestRankHistoryOpt = userRankHistoryRepository.findTopByUserIdOrderByChangedAtDesc(user.getId());
            UserRank currentRank = latestRankHistoryOpt.map(UserRankHistory::getUserRank).orElse(null);

            // 등급이 변경되었을 때만 기록하고 업데이트
            if (!Objects.equals(newRank, currentRank)) {
                UserRankHistory rankHistory = new UserRankHistory();
                rankHistory.setUser(user);
                rankHistory.setUserRank(newRank);
//                rankHistory.setChangedAt(LocalDateTime.now());
                rankHistory.setChangeReason("3개월 이내 순수 금액 기반으로 등급 업데이트: " + newRank.getRankName());
                userRankHistoryRepository.save(rankHistory);
            }
        }
    }

    // 3개월 이내의 순수 금액 계산
    public Integer calculatePureAmountForLastThreeMonths(User user) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

        return user.getOrders().stream()
                .filter(order -> order.getOrderTime().isAfter(threeMonthsAgo.atStartOfDay())
                                    && order.getState() == OrderState.CONFIRMED)  // 3개월 이내 주문만 필터링
                .mapToInt(order -> {
                    Integer totalAmount = order.getTotalAmount(); // 전체 주문 금액
                    Integer totalWrapCost = Optional.ofNullable(
                            orderDetailRepository.calculateTotalWrapCostByOrderId(order.getId())
                    ).orElse(0); // 랩 비용
                    return totalAmount - totalWrapCost; // 순수 금액 계산
                })
                .sum();
    }


    // 순수 금액을 기준으로 적합한 UserRank를 반환
    private UserRank getRankBasedOnAmount(Integer pureAmount, List<UserRank> ranks) {
        return ranks.stream()
                .filter(rank -> pureAmount >= rank.getMinAmount() &&
                        (rank.getMaxAmount() == null || pureAmount < rank.getMaxAmount()))
                .findFirst()
                .orElse(null);
    }


    public List<UserRank> getAllUserRanks() {
        return userRankRepository.findAll();
    }
}