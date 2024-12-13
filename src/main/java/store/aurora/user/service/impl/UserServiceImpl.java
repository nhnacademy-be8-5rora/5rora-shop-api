package store.aurora.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import store.aurora.user.dto.SignUpRequest;
import store.aurora.user.entity.User;
import store.aurora.user.entity.UserRank;
import store.aurora.user.entity.UserRankHistory;
import store.aurora.user.entity.UserStatus;
import store.aurora.user.repository.UserRankHistoryRepository;
import store.aurora.user.repository.UserRankRepository;
import store.aurora.user.repository.UserRepository;
import store.aurora.user.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserRankRepository userRankRepository;
    private final UserRankHistoryRepository userRankHistoryRepository;
//    private final PasswordEncoder passwordEncoder;

    private static final int INACTIVE_PERIOD_MONTHS = 3;    // 휴면 3개월 기준

    // 회원가입
    @Override
    public void registerUser(SignUpRequest request) {
        // 유효성 검사
        if (userRepository.existsById(request.getId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 존재하는 전화번호입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }


//        // 두레이 인증코드 확인
//        String storedCode = redisService.getVerificationCode(request.getPhoneNumber());
//        if (storedCode == null || !storedCode.equals(request.getVerificationCode())) {
//            throw new IllegalArgumentException("전화번호 인증이 실패했습니다.");
//        }

        // 비밀번호 암호화
//        String encodedPassword = passwordEncoder.encode(request.getPassword());


        // User에 저장
        User user = new User();
        user.setId(request.getId());
        user.setPassword(request.getPassword());  // 암호화된 비밀번호 저장
        user.setName(request.getName());
        user.setBirth(LocalDate.parse(request.getBirth(), DateTimeFormatter.ofPattern("yyyyMMdd")));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLogin(null);
        user.setSignUpDate(LocalDate.now());
        user.setIsOauth(false);

        userRepository.save(user);

        // 회원등급 저장
        UserRank userRank = userRankRepository.findByRankName("GENERAL");
        UserRankHistory userRankHistory = new UserRankHistory();
        userRankHistory.setUserRank(userRank);
        userRankHistory.setChangeReason("회원가입");
        userRankHistory.setChangedAt(LocalDateTime.now());
        userRankHistory.setUser(user);

        userRankHistoryRepository.save(userRankHistory);
    }

    // 회원탈퇴
    @Override
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    // 3개월 이상 로그인하지 않은 회원 휴면 처리
    @Override
    public void checkAndSetSleepStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdDate = now.minusMonths(INACTIVE_PERIOD_MONTHS);

        // 탈퇴회원은 제외
        List<User> inactiveUsers = userRepository.findByLastLoginBeforeAndStatusNot(thresholdDate, UserStatus.DELETED);

        for (User user: inactiveUsers) {
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
        }
    }

    // 휴면 해제 처리
    @Override
    public void reactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // 인증 api에서 비밀번호 확인 인증 처리

        if (user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("이미 휴면해제된 계정입니다.");
        }
    }


}