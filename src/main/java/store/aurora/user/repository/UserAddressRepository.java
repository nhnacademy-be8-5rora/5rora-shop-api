package store.aurora.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.aurora.user.entity.UserAddress;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    int countByUserId(String userId);

    //중복된 배송지 검색
    Optional<UserAddress> findByUserIdAndAddressIdAndAddrDetailAndReceiver(
            String userId,
            Long addressId,
            String addrDetail,
            String receiver
    );

    List<UserAddress> findByUserId(String userId);
    Optional<UserAddress> findByIdAndUserId(Long id, String userId);
}