package store.aurora.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import store.aurora.book.config.QuerydslConfiguration;
import store.aurora.user.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

@Import(QuerydslConfiguration.class)
@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("roleName으로 Role 조회")
    void testFindByRoleName() {
        // Given
        Role role = new Role("ROLE_USER");
        roleRepository.save(role);

        // When
        Role foundRole = roleRepository.findByRoleName("ROLE_USER");

        // Then
        assertThat(foundRole).isNotNull();
        assertThat(foundRole.getRoleName()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("존재하지 않는 roleName 조회 시 null 반환")
    void testFindByRoleNameNotFound() {
        // When
        Role foundRole = roleRepository.findByRoleName("NON_EXISTENT_ROLE");

        // Then
        assertThat(foundRole).isNull();
    }
}
