package store.aurora.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import store.aurora.book.config.QuerydslConfiguration;
import store.aurora.user.entity.Rank;
import store.aurora.user.entity.UserRank;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Import(QuerydslConfiguration.class)
@DataJpaTest
class UserRankRepositoryTest {

    @Autowired
    private UserRankRepository userRankRepository;

    private UserRank userRank;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 준비
        userRank = new UserRank();
        userRank.setRankName(Rank.GENERAL);
        userRank.setMinAmount(0);
        userRank.setMaxAmount(100000);
        userRank.setPointRate(BigDecimal.valueOf(0.01));
        userRankRepository.save(userRank);
    }

    @Test
    @DisplayName("주어진 Rank로 UserRank 조회")
    void testFindByRankName() {
        UserRank foundUserRank = userRankRepository.findByRankName(Rank.GENERAL);

        assertThat(foundUserRank).isNotNull();
        assertThat(foundUserRank.getRankName()).isEqualTo(Rank.GENERAL);
        assertThat(foundUserRank.getMinAmount()).isEqualTo(0);
        assertThat(foundUserRank.getMaxAmount()).isEqualTo(100000);
        assertThat(foundUserRank.getPointRate()).isEqualTo(BigDecimal.valueOf(0.01));
    }

    @Test
    @DisplayName("존재하지 않는 Rank로 조회 시 null 반환")
    void testFindByRankNameNotFound() {
        UserRank foundUserRank = userRankRepository.findByRankName(Rank.GOLD);

        assertThat(foundUserRank).isNull();
    }

    @Test
    @DisplayName("UserRank 저장 후 조회")
    void testSaveAndFindUserRank() {
        // Given
        UserRank newUserRank = new UserRank();
        newUserRank.setRankName(Rank.ROYAL);
        newUserRank.setMinAmount(100000);
        newUserRank.setMaxAmount(200000);
        newUserRank.setPointRate(BigDecimal.valueOf(0.02));

        // When
        userRankRepository.save(newUserRank);
        UserRank foundUserRank = userRankRepository.findByRankName(Rank.ROYAL);

        // Then
        assertThat(foundUserRank).isNotNull();
        assertThat(foundUserRank.getRankName()).isEqualTo(Rank.ROYAL);
        assertThat(foundUserRank.getMinAmount()).isEqualTo(100000);
        assertThat(foundUserRank.getMaxAmount()).isEqualTo(200000);
        assertThat(foundUserRank.getPointRate()).isEqualTo(BigDecimal.valueOf(0.02));
    }
}
