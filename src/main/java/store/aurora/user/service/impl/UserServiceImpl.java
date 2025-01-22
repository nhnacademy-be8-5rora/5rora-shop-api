package store.aurora.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.user.dto.SignUpRequest;
import store.aurora.user.dto.UserDetailResponseDto;
import store.aurora.user.dto.UserInfoResponseDto;
import store.aurora.user.dto.UserResponseDto;
import store.aurora.user.entity.*;
import store.aurora.user.exception.*;
import store.aurora.user.repository.*;
import store.aurora.user.service.UserService;

import java.time.Clock;
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
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int INACTIVE_PERIOD_MONTHS = 3;    // 휴면 3개월 기준
    private final Clock clock;
    private final RedisTemplate redisTemplate;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    // 회원가입
    @Override
    public User registerUser(SignUpRequest request) {
        // 인증 상태 확인
//        String verificationStatus = (String) redisTemplate.opsForValue().get(request.getPhoneNumber() + "_verified");
//        if (verificationStatus == null || !verificationStatus.equals("true")) {
//            throw new VerificationException("인증 코드가 확인되지 않았습니다. 인증을 완료한 후 회원가입을 진행해주세요.");
//        }

        // 유효성 검사
        if (userRepository.existsById(request.getId())) {
            throw new DuplicateUserException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateUserException("이미 존재하는 전화번호입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());


        // User에 저장
        User user = new User();
        user.setId(request.getId());
        user.setPassword(encodedPassword);  // 암호화된 비밀번호 저장
        user.setName(request.getName());
        user.setBirth(LocalDate.parse(request.getBirth(), DateTimeFormatter.ofPattern("yyyyMMdd")));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLogin(null);
        user.setSignUpDate(LocalDate.now());
        user.setIsOauth(false);

        User savedUser = userRepository.save(user);

        // 회원등급 저장
        UserRank userRank = userRankRepository.findByRankName(Rank.GENERAL);
        if (userRank == null) {
            throw new RankNotFoundException("해당 등급을 찾을 수 없습니다.");
        }
        UserRankHistory userRankHistory = new UserRankHistory();
        userRankHistory.setUserRank(userRank);
        userRankHistory.setChangeReason("회원가입");
        userRankHistory.setChangedAt(LocalDateTime.now());
        userRankHistory.setUser(user);

        userRankHistoryRepository.save(userRankHistory);

        // 권한 저장
        Role role = roleRepository.findByRoleName("ROLE_USER");
        if (role == null) {
            throw new RoleNotFoundException("해당 권한을 찾을 수 없습니다.");
        }
        UserRole userRole = new UserRole();
        userRole.setRole(role);
        userRole.setUser(user);

        userRoleRepository.save(userRole);

        // 인증 상태 삭제
//        redisTemplate.delete(request.getPhoneNumber() + "_verified");

        return savedUser;
    }

    @Override
    public User registerOauthUser(SignUpRequest request) {
        User user = new User();
        user.setId(request.getId());
        user.setName(request.getName());
        user.setBirth(LocalDate.parse(request.getBirth(), DateTimeFormatter.ofPattern("yyyyMMdd")));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLogin(null);
        user.setSignUpDate(LocalDate.now());
        user.setIsOauth(true);

        User savedUser = userRepository.save(user);

        // 회원등급 저장
        UserRank userRank = userRankRepository.findByRankName(Rank.GENERAL);
        if (userRank == null) {
            throw new RankNotFoundException("해당 등급을 찾을 수 없습니다.");
        }
        UserRankHistory userRankHistory = new UserRankHistory();
        userRankHistory.setUserRank(userRank);
        userRankHistory.setChangeReason("회원가입");
        userRankHistory.setChangedAt(LocalDateTime.now());
        userRankHistory.setUser(user);

        userRankHistoryRepository.save(userRankHistory);

        // 권한 저장
        Role role = roleRepository.findByRoleName("ROLE_USER");
        if (role == null) {
            throw new RoleNotFoundException("해당 권한을 찾을 수 없습니다.");
        }
        UserRole userRole = new UserRole();
        userRole.setRole(role);
        userRole.setUser(user);

        userRoleRepository.save(userRole);

        return savedUser;
    }

    // 회원탈퇴
    @Override
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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

        if (inactiveUsers.isEmpty()) { return; }

        for (User user: inactiveUsers) {
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
        }
    }

    @Override
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // 휴면 해제 처리
    @Override
    public void reactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 인증 상태 확인
        Boolean verificationStatus = (Boolean) redisTemplate.opsForValue().get(user.getPhoneNumber() + "_verified");

        if (user.getStatus() == UserStatus.INACTIVE) {
            if (verificationStatus == null || !verificationStatus) {
                throw new VerificationException("인증 코드가 확인되지 않았습니다. 인증이 완료가 되어야 휴면해제됩니다.");
            }
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

        } else {
            throw new AlreadyActiveUserException("이미 휴면해제된 계정입니다.");
        }

        // 인증 상태 삭제
        redisTemplate.delete(user.getPhoneNumber() + "_verified");
    }

    private String getRole(User user, String userId) {
        return user.getUserRoles().stream()
                .findFirst()
                .map(userRole -> userRole.getRole().getRoleName())
//                .orElse("ROLE_USER"); // 기본값 설정
                .orElseThrow(() -> new RoleNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByUserId(String userId) {
        User user = getUser(userId);
        String role = getRole(user, userId);
        return new UserResponseDto(user.getId(), role);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isUserExists(String userId) {
        return userRepository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public UserDetailResponseDto getPasswordAndRole(String userId) {
        User user = getUser(userId);
        if (user.getStatus() == UserStatus.DELETED) { // 탈퇴회원일때
            throw new DeletedAccountException("탈퇴한 계정입니다.");
        } else if (user.getStatus() == UserStatus.INACTIVE) {  // 휴면회원일때
            throw new DormantAccountException("휴면 계정입니다. 휴면 해제 후 이용해주세요.");
        }
        String role = getRole(user, userId);
        return new UserDetailResponseDto(user.getPassword(), role);
    }

    // 회원정보 조회
    @Override
    @Transactional(readOnly = true)
    public UserInfoResponseDto getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Rank rankName = userRankHistoryRepository.findLatestRankNameByUserId(userId)
                .orElseThrow(() -> new RankNotFoundException("해당 등급을 찾을 수 없습니다."));

        return new UserInfoResponseDto(
                user.getId(),
                user.getName(),
                user.getBirth(),
                user.getPhoneNumber(),
                user.getEmail(),
                rankName
        );
    }

    @Override
    public List<String> searchByMonth(int currentMonth) {
        return userRepository.findUserIdsByBirthMonth(currentMonth);
    }



    @Transactional
    public void updateLastLogin(String userId, LocalDateTime lastLogin) {
        getUser(userId).setLastLogin(lastLogin);
    }
}