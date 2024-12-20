package store.aurora.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.aurora.user.entity.UserRole;

@Repository
public interface AdminRoleRepository extends JpaRepository<UserRole, Long> {
}
