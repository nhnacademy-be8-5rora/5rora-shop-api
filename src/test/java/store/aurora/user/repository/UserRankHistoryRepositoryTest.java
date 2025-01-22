package store.aurora.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import store.aurora.book.config.QuerydslConfiguration;
import store.aurora.user.entity.Rank;
import store.aurora.user.entity.User;
import store.aurora.user.entity.UserRank;
import store.aurora.user.entity.UserRankHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(QuerydslConfiguration.class)
@DataJpaTest
class UserRankHistoryRepositoryTest {

    @Autowired
    private UserRankHistoryRepository userRankHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRankRepository userRankRepository;

    @BeforeEach
    void setUp() {
        User user = new User("user123", "John Doe", LocalDate.of(1990, 1, 1), "20000101", "john_doe@example.com", true);
        userRepository.save(user);

        UserRank userRank1 = new UserRank(1L, Rank.GENERAL, 0, 1000, BigDecimal.valueOf(0.01));
        UserRank userRank2 = new UserRank(2L, Rank.ROYAL, 1000, 2000, BigDecimal.valueOf(0.02));
        userRankRepository.save(userRank1);
        userRankRepository.save(userRank2);

        UserRankHistory userRankHistory1 = new UserRankHistory(1L, "회원 가입", LocalDateTime.now().minusDays(2), user, userRank1);
        UserRankHistory userRankHistory2 = new UserRankHistory(2L, "등급 변경", LocalDateTime.now(), user, userRank2);
        userRankHistoryRepository.save(userRankHistory1);
        userRankHistoryRepository.save(userRankHistory2);
    }

//    @Test
//    @DisplayName("특정 userId의 최신 rank_name을 조회")
//    void testFindLatestRankNameByUserId() {
//
//        Optional<Rank> latestRankName = userRankHistoryRepository.findLatestRankNameByUserId("user123");
//
//        assertThat(latestRankName).isPresent();
//        assertThat(latestRankName.get()).isEqualTo(Rank.ROYAL);
//    }
}
