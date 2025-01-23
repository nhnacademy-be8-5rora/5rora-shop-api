package store.aurora.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.aurora.user.entity.UserRole;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    // 특정 UserId로 Role 조회
    List<UserRole> findByUserId(String userId);
}
