package store.aurora.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
        import static org.junit.jupiter.api.Assertions.*;

        import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
        import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import store.aurora.user.dto.*;
import store.aurora.user.entity.*;
        import store.aurora.user.exception.*;
        import store.aurora.user.repository.*;

        import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;

    @Mock private UserRankRepository userRankRepository;

    @Mock private UserRankHistoryRepository userRankHistoryRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private UserRoleRepository userRoleRepository;

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Mock private ValueOperations<String, Object> valueOperations;

    @Mock private Clock clock;

    @InjectMocks private UserServiceImpl userService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private SignUpRequest signUpRequest;
    private User testUser;
    private User inactiveUser;
    private UserDetailResponseDto expectedResponse;
    String userId = "testId";
    Rank testRank = Rank.GENERAL;

    @BeforeEach
    void setUp() {
        testUser = new User(userId, "hyewon", LocalDate.of(2000, 1, 1), "010-0000-0000", "example@google.com", true);
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ACTIVE);
        expectedResponse = new UserDetailResponseDto(testUser.getPassword(), "USER_ROLE");

        inactiveUser = new User("userId", "John Doe", LocalDate.of(1990, 1, 1), "010-0000-0000", "john@example.com", true);
        inactiveUser.setStatus(UserStatus.INACTIVE);
        inactiveUser.setPhoneNumber("010-0000-0000");


        Role testRole = new Role("USER");
        UserRole testUserRole = new UserRole(testUser, testRole);
        LinkedList<UserRole> list = new LinkedList<>();
        list.add(testUserRole);
        testUser.setUserRoles(list);

        // Bypass passwordEncoder initialization in UserServiceImpl
        ReflectionTestUtils.setField(userService, "passwordEncoder", new BCryptPasswordEncoder());

        signUpRequest = new SignUpRequest();
        signUpRequest.setId("newUser");
        signUpRequest.setPassword("password123");
        signUpRequest.setName("John Doe");
        signUpRequest.setBirth("19900101");
        signUpRequest.setPhoneNumber("010-1234-5678");
        signUpRequest.setEmail("johndoe@example.com");
    }

    @Test
    void testGetUserByUserId_Success() {
        // Given
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));

        // When
        UserResponseDto result = userService.getUserByUserId("user1");

        // Then
        assertNotNull(result);
        assertEquals("testId", result.getUsername());
        assertEquals("USER", result.getRole());
    }

    @Test
    void testGetUserByUserId_RoleNotFound() {
        // Given
        User userWithoutRole = new User("user2", "hyewon", LocalDate.of(2000, 1, 1), "010-0000-0000", "example@google.com", true);
        userWithoutRole.setUserRoles(new LinkedList<>());

        when(userRepository.findById("user2")).thenReturn(Optional.of(userWithoutRole));

        // When & Then
        assertThrows(RoleNotFoundException.class, () -> userService.getUserByUserId("user2"));
    }

    @Test
    @DisplayName("일반 회원가입 성공")
    void testRegisterUser_Success() {
        // Given
        UserRank mockUserRank = new UserRank();
        mockUserRank.setRankName(Rank.GENERAL);

        Role mockRole = new Role("ROLE_USER");

        // Mock 설정
        when(userRepository.existsById(signUpRequest.getId())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())).thenReturn(false);
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
        when(userRankRepository.findByRankName(Rank.GENERAL)).thenReturn(mockUserRank);
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(mockRole);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.registerUser(signUpRequest);

        // Then
        assertNotNull(result);
        assertEquals(signUpRequest.getId(), result.getId());
        assertTrue(passwordEncoder.matches(signUpRequest.getPassword(), result.getPassword()));
        assertEquals(signUpRequest.getName(), result.getName());

        // Mock 호출 검증
        verify(userRepository).existsById(signUpRequest.getId());
        verify(userRepository).existsByPhoneNumber(signUpRequest.getPhoneNumber());
        verify(userRepository).existsByEmail(signUpRequest.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userRankRepository).findByRankName(Rank.GENERAL);
        verify(userRankHistoryRepository).save(any(UserRankHistory.class));
        verify(roleRepository).findByRoleName("ROLE_USER");
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("중복된 ID로 회원 가입 시 예외 처리")
    void registerUser_DuplicateId() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setId("existingUser");
        when(userRepository.existsById(request.getId())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateUserException.class, () -> userService.registerUser(request));
        verify(userRepository).existsById(request.getId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 전화번호로 회원 가입 시 예외 처리")
    void registerUser_DuplicatePhoneNumber() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setPhoneNumber("010-1234-5678");
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateUserException.class, () -> userService.registerUser(request));
        verify(userRepository).existsByPhoneNumber(request.getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("중복된 이메일로 회원 가입 시 예외 처리")
    void registerUser_DuplicateEmail() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setEmail("example@google.com");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateUserException.class, () -> userService.registerUser(request));
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("오어스 회원가입 성공")
    void registerOauthUser_Success() {
        // Mock 데이터 설정
        UserRank mockUserRank = new UserRank();
        mockUserRank.setRankName(Rank.GENERAL);

        Role mockRole = new Role();
        mockRole.setRoleName("ROLE_USER");

        User mockUser = new User();
        mockUser.setId(signUpRequest.getId());
        mockUser.setName(signUpRequest.getName());
        mockUser.setBirth(LocalDate.parse(signUpRequest.getBirth(), DateTimeFormatter.ofPattern("yyyyMMdd")));
        mockUser.setPhoneNumber(signUpRequest.getPhoneNumber());
        mockUser.setEmail(signUpRequest.getEmail());
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setSignUpDate(LocalDate.now());
        mockUser.setIsOauth(true);

        // Mock 동작 정의
        when(userRankRepository.findByRankName(Rank.GENERAL)).thenReturn(mockUserRank);
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(mockRole);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // 테스트 실행
        User result = userService.registerOauthUser(signUpRequest);

        // 검증
        assertNotNull(result);
        assertEquals(signUpRequest.getId(), result.getId());
        assertEquals(signUpRequest.getName(), result.getName());
        assertEquals(signUpRequest.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(signUpRequest.getEmail(), result.getEmail());
        assertTrue(result.getIsOauth());

        verify(userRepository, times(1)).save(any(User.class));
        verify(userRankRepository, times(1)).findByRankName(Rank.GENERAL);
        verify(userRankHistoryRepository, times(1)).save(any(UserRankHistory.class));
        verify(roleRepository, times(1)).findByRoleName("ROLE_USER");
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("오어스 회원가입 실패: UserRankNotFound")
    void registerOauthUser_UserRankNotFound_ShouldThrowException() {
        when(userRankRepository.findByRankName(Rank.GENERAL)).thenReturn(null);

        // 예외 발생 검증
        RankNotFoundException exception = assertThrows(RankNotFoundException.class,
                () -> userService.registerOauthUser(signUpRequest));

        assertEquals("해당 등급을 찾을 수 없습니다.", exception.getMessage());

        verify(userRankRepository, times(1)).findByRankName(Rank.GENERAL);
    }

    @Test
    @DisplayName("오어스 회원가입 실패: RoleNotFound")
    void registerOauthUser_RoleNotFound_ShouldThrowException() {
        UserRank mockUserRank = new UserRank();
        mockUserRank.setRankName(Rank.GENERAL);

        when(userRankRepository.findByRankName(Rank.GENERAL)).thenReturn(mockUserRank);
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(null);

        // 예외 발생 검증
        RoleNotFoundException exception = assertThrows(RoleNotFoundException.class,
                () -> userService.registerOauthUser(signUpRequest));

        assertEquals("해당 권한을 찾을 수 없습니다.", exception.getMessage());

        verify(userRankRepository, times(1)).findByRankName(Rank.GENERAL);
        verify(roleRepository, times(1)).findByRoleName("ROLE_USER");
    }


    @Test
    void testUpdateLastLogin() {
        // Given: 테스트 사용자와 초기 데이터 설정
        LocalDateTime initialLogin = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updatedLogin = LocalDateTime.of(2025, 1, 16, 12, 0);

        testUser.setLastLogin(initialLogin);

        // UserRepository에서 getUser 메서드 호출 시 user 반환하도록 설정
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When: updateLastLogin 호출
        userService.updateLastLogin(userId, updatedLogin);

        // Then: lastLogin 필드가 업데이트되었는지 검증
        assertThat(testUser.getLastLogin()).isEqualTo(updatedLogin);
    }

    @Test
    void testUpdateLastLogin_UserNotFound() {
        // Given: 존재하지 않는 사용자 ID
        LocalDateTime updatedLogin = LocalDateTime.of(2025, 1, 16, 12, 0);

        // UserRepository에서 getUser 메서드 호출 시 빈 Optional 반환
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then: 예외가 발생해야 함
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateLastLogin(userId, updatedLogin)
        );
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void testDeleteUser_ShouldChangeStatusToDeleted() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.of(testUser));

        // when
        userService.deleteUser("userId");

        // then
        verify(userRepository).save(testUser); // save가 호출되어야 한다
        assertEquals(UserStatus.DELETED, testUser.getStatus()); // 상태가 DELETED로 변경되었는지 확인
    }

    @Test
    @DisplayName("회원 탈퇴 실패: UserNotFoundException 발생")
    void testDeleteUser_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("userId"));
    }

    @Test
    @DisplayName("휴면 처리 성공")
    void testCheckAndSetSleepStatus_ShouldSetInactiveStatus_WhenUserIsInactiveFor3Months() {
        // given
        User inactiveUser = new User("userId", "John Doe", LocalDate.of(1990, 1, 1), "010-0000-0000", "john@example.com", false);
        inactiveUser.setStatus(UserStatus.ACTIVE);
        inactiveUser.setLastLogin(LocalDateTime.now().minusMonths(4));  // 4개월 전 로그인

        List<User> inactiveUsers = List.of(inactiveUser);
        when(userRepository.findByLastLoginBeforeAndStatusNot(any(LocalDateTime.class), eq(UserStatus.DELETED)))
                .thenReturn(inactiveUsers);

        // when
        userService.checkAndSetSleepStatus();

        // then
        verify(userRepository).save(inactiveUser); // save가 호출되어야 한다
        assertEquals(UserStatus.INACTIVE, inactiveUser.getStatus()); // 상태가 INACTIVE로 변경되었는지 확인
    }

    @Test
    @DisplayName("휴면 처리할 회원이 없을 경우")
    void testCheckAndSetSleepStatus_ShouldNotUpdateStatus_WhenNoInactiveUsers() {
        // given
        List<User> inactiveUsers = new ArrayList<>();
        when(userRepository.findByLastLoginBeforeAndStatusNot(any(LocalDateTime.class), eq(UserStatus.DELETED)))
                .thenReturn(inactiveUsers);

        // when
        userService.checkAndSetSleepStatus();

        // then
        verify(userRepository, never()).save(any(User.class)); // save가 호출되지 않아야 한다
    }

    @Test
    @DisplayName("휴면 해제 처리 성공")
    void testReactivateUser_ShouldActivateUser_WhenStatusIsInactiveAndVerificationIsCompleted() {
        // given
        Boolean verificationStatus = true; // 인증 완료 상태
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findById("userId")).thenReturn(Optional.of(inactiveUser));
        when(valueOperations.get(inactiveUser.getPhoneNumber() + "_verified")).thenReturn(verificationStatus);

        // when
        userService.reactivateUser("userId");

        // then
        verify(userRepository).save(inactiveUser); // save가 호출되어야 한다
        assertEquals(UserStatus.ACTIVE, inactiveUser.getStatus()); // 상태가 ACTIVE로 변경되었는지 확인
        verify(redisTemplate).delete(inactiveUser.getPhoneNumber() + "_verified"); // 인증 상태가 삭제되었는지 확인
    }

    @Test
    @DisplayName("휴면 해제 처리 실패: 인증 미완료")
    void testReactivateUser_ShouldThrowVerificationException_WhenVerificationStatusIsNull() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findById("userId")).thenReturn(Optional.of(inactiveUser));
        when(valueOperations.get(inactiveUser.getPhoneNumber() + "_verified")).thenReturn(null);

        // when & then
        assertThrows(VerificationException.class, () -> userService.reactivateUser("userId"));
    }

    @Test
    @DisplayName("휴면 해제 처리 실패: 인증 미완료")
    void testReactivateUser_ShouldThrowVerificationException_WhenVerificationStatusIsFalse() {
        // given
        Boolean verificationStatus = false; // 인증 미완료 상태
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findById("userId")).thenReturn(Optional.of(inactiveUser));
        when(valueOperations.get(inactiveUser.getPhoneNumber() + "_verified")).thenReturn(verificationStatus);

        // when & then
        assertThrows(VerificationException.class, () -> userService.reactivateUser("userId"));
    }

    @Test
    @DisplayName("휴면 해제 처리 실패: 이미 휴면 해제된 계정")
    void testReactivateUser_ShouldThrowAlreadyActiveUserException_WhenUserIsAlreadyActive() {
        // given
        inactiveUser.setStatus(UserStatus.ACTIVE); // 이미 ACTIVE 상태로 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findById("userId")).thenReturn(Optional.of(inactiveUser));

        // when & then
        assertThrows(AlreadyActiveUserException.class, () -> userService.reactivateUser("userId"));
    }

    @Test
    @DisplayName("휴면 해제 처리 실패: UserNotFoundException")
    void testReactivateUser_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.reactivateUser("userId"));
    }

    @Test
    @DisplayName("")
    void testGetPasswordAndRole_ShouldReturnPasswordAndRole_WhenUserIsActive() {
        // given
        when(userRepository.findById("testId")).thenReturn(Optional.of(testUser));

        // when
        UserDetailResponseDto result = userService.getPasswordAndRole("testId");

        // then
        assertNotNull(result);
        assertEquals(testUser.getPassword(), result.getPassword());
        assertEquals("USER", result.getRole());
    }

    @Test
    void testGetPasswordAndRole_ShouldThrowDeletedAccountException_WhenUserIsDeleted() {
        // given
        testUser.setStatus(UserStatus.DELETED); // 탈퇴 상태로 설정
        when(userRepository.findById("userId")).thenReturn(Optional.of(testUser));

        // when & then
        assertThrows(DeletedAccountException.class, () -> userService.getPasswordAndRole("userId"));
    }

    @Test
    void testGetPasswordAndRole_ShouldThrowDormantAccountException_WhenUserIsInactive() {
        // given
        testUser.setStatus(UserStatus.INACTIVE); // 휴면 상태로 설정
        when(userRepository.findById("userId")).thenReturn(Optional.of(testUser));

        // when & then
        assertThrows(DormantAccountException.class, () -> userService.getPasswordAndRole("userId"));
    }

    @Test
    void testGetPasswordAndRole_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.getPasswordAndRole("userId"));
    }

    @Test
    void testGetUserInfo_ShouldReturnUserInfoResponseDto_WhenUserExists() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.of(testUser));
        when(userRankHistoryRepository.findLatestRankNameByUserId("userId")).thenReturn(Optional.of(testRank));

        // when
        UserInfoResponseDto result = userService.getUserInfo("userId");

        // then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getBirth(), result.getBirth());
        assertEquals(testUser.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testRank, result.getRankName());
    }

    @Test
    void testGetUserInfo_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.getUserInfo("userId"));
    }

    @Test
    void testGetUserInfo_ShouldThrowRankNotFoundException_WhenRankIsNotFound() {
        // given
        when(userRepository.findById("userId")).thenReturn(Optional.of(testUser));
        when(userRankHistoryRepository.findLatestRankNameByUserId("userId")).thenReturn(Optional.empty());

        // when & then
        assertThrows(RankNotFoundException.class, () -> userService.getUserInfo("userId"));
    }

    @Test
    @DisplayName("회원정보 수정 성공")
    void updateUser_Success() {
        // Given
        String userId = "testUser";
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setName("Updated Name");
        request.setPhoneNumber("010-9999-9999");
        request.setEmail("updated@example.com");
        request.setPassword("newPassword");

        User user = new User(userId, "Original Name", LocalDate.of(1990, 1, 1), "010-1111-1111", "original@example.com", true);
        user.setPassword("oldPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User updatedUser = userService.updateUser(userId, request);

        // Then
        assertNotNull(updatedUser);
        assertEquals(request.getName(), updatedUser.getName());
        assertEquals(request.getPhoneNumber(), updatedUser.getPhoneNumber());
        assertEquals(request.getEmail(), updatedUser.getEmail());
        assertTrue(passwordEncoder.matches(request.getPassword(), updatedUser.getPassword()));

        verify(userRepository).findById(userId);
        verify(userRepository).existsByPhoneNumber(request.getPhoneNumber());
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 존재하지 않는 사용자")
    void updateUser_UserNotFound() {
        // Given
        String userId = "nonexistentUser";
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, request));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 중복된 전화번호")
    void updateUser_DuplicatePhoneNumber() {
        // Given
        String userId = "testUser";
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setPhoneNumber("010-2222-2222");

        User user = new User(userId, "Original Name", LocalDate.of(1990, 1, 1), "010-1111-1111", "original@example.com", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateUserException.class, () -> userService.updateUser(userId, request));
        verify(userRepository).findById(userId);
        verify(userRepository).existsByPhoneNumber(request.getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원정보 수정 실패 - 중복된 이메일")
    void updateUser_DuplicateEmail() {
        // Given
        String userId = "testUser";
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setEmail("duplicate@example.com");

        User user = new User(userId, "Original Name", LocalDate.of(1990, 1, 1), "010-1111-1111", "original@example.com", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateUserException.class, () -> userService.updateUser(userId, request));
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

}