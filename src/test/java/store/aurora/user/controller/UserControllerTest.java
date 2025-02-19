package store.aurora.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import store.aurora.point.entity.PointPolicyCategory;
import store.aurora.point.service.PointHistoryService;
import store.aurora.user.dto.*;
import store.aurora.user.entity.Rank;
import store.aurora.user.entity.User;
import store.aurora.user.exception.UserNotFoundException;
import store.aurora.user.service.DoorayMessengerService;
import store.aurora.user.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PointHistoryService pointHistoryService;

    @MockBean
    private DoorayMessengerService doorayMessengerService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    String userId = "hyewon";

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule()); // Java 8 Date/Time 지원 추가
    }

    @Test
    void getUser_ShouldReturnUser_WhenUserIdExists() throws Exception {
        // given
        UserResponseDto userResponseDto = new UserResponseDto("hyewon", "ROLE_USER");

        // Mocking UserService
        when(userService.getUserByUserId(userId)).thenReturn(userResponseDto);

        // when & then
        mockMvc.perform(get("/api/users/auth/me")
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("hyewon"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void testUpdateLastLogin_Success() throws Exception {
        // Given
        LocalDateTime lastLogin = LocalDateTime.now();

        doNothing().when(userService).updateLastLogin(userId, lastLogin);

        // When & Then
        mockMvc.perform(patch("/api/users/" + userId + "/last-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lastLogin)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updateLastLogin(userId, lastLogin);
    }

    @Test
    void testUpdateLastLogin_UserNotFound() throws Exception {
        // Given
        LocalDateTime lastLogin = LocalDateTime.now();

        doThrow(new UserNotFoundException("User not found")).when(userService).updateLastLogin(userId, lastLogin);

        // When & Then
        mockMvc.perform(patch("/api/users/" + userId + "/last-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lastLogin)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateLastLogin(userId, lastLogin);
    }


    @Test
    @DisplayName("일반 회원가입 성공")
    void testSignUp_WithNormalUser_Success() throws Exception {
        SignUpRequest request = new SignUpRequest("user123","password123","홍길동","19900101","01012345678","hong@example.com", null);

        User mockUser = new User("user123", "홍길동", LocalDate.of(1990, 1, 1),"01012345678", "hong@example.com", false);

        when(userService.registerUser(any(SignUpRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/users")
                        .param("isOauth", "false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        verify(userService,times(1)).registerUser(any(SignUpRequest.class));
        verify(pointHistoryService,times(1)).earnPoint(PointPolicyCategory.SIGNUP, mockUser);
    }

    @Test
    @DisplayName("인증코드 생성 및 전송 성공")
    void testSendCode_Success() throws Exception {
        String phoneNumber = "01012345678";

        mockMvc.perform(post("/api/users/send-verification-code")
                        .param("phoneNumber", phoneNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 전송되었습니다."));

        verify(doorayMessengerService,times(1)).sendVerificationCode(eq(phoneNumber), anyString());
    }

    @Test
    @DisplayName("인증코드 검증 성공")
    void testVerifyCode_Success() throws Exception {
        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("01012345678");
        request.setVerificationCode("123456");

        when(doorayMessengerService.verifyCode(anyString(), anyString())).thenReturn(true);  // 인증 성공 시

        mockMvc.perform(post("/api/users/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증이 완료되었습니다."));

        verify(doorayMessengerService,times(1)).verifyCode(eq(request.getPhoneNumber()), eq(request.getVerificationCode()));
    }

    @Test
    @DisplayName("인증코드 검증 실패")
    void testVerifyCode_Failure() throws Exception {
        VerificationRequest request = new VerificationRequest();
        request.setPhoneNumber("01012345678");
        request.setVerificationCode("123456");

        when(doorayMessengerService.verifyCode(anyString(), anyString())).thenReturn(false);  // 인증 실패 시

        mockMvc.perform(post("/api/users/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 인증 코드입니다. 또는 코드가 만료되었습니다."));

        verify(doorayMessengerService,times(1)).verifyCode(eq(request.getPhoneNumber()), eq(request.getVerificationCode()));
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void testDeleteUser_Success() throws Exception {
        String userId = "user123";

        mockMvc.perform(delete("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."));

        verify(userService,times(1)).deleteUser(userId);
    }

    @Test
    @DisplayName("회원탈퇴 실패: UserNotFoundException 발생")
    void testDeleteUser_UserNotFound() throws Exception {
        String userId = "user123";
        doThrow(new UserNotFoundException(userId)).when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/users/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(userService,times(1)).deleteUser(userId);
    }

    @Test
    @DisplayName("휴면해제 성공")
    void testReactivateUser_Success() throws Exception {
        String userId = "user123";

        doNothing().when(userService).reactivateUser(userId);

        mockMvc.perform(post("/api/users/reactivate")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("휴면 계정이 활성화되었습니다."));

        verify(userService,times(1)).reactivateUser(userId);
    }

    @Test
    @DisplayName("휴면해제 실패: UserNotFoundException 발생")
    void testReactivateUser_UserNotFound() throws Exception {
        String userId = "user123";
        doThrow(new UserNotFoundException(userId)).when(userService).reactivateUser(userId);

        mockMvc.perform(post("/api/users/reactivate")
                        .param("userId", userId))
                .andExpect(status().isNotFound());

        verify(userService,times(1)).reactivateUser(userId);
    }

    @Test
    @DisplayName("회원정보 수정 성공")
    void testUpdateUser_Success() throws Exception {
        // Given
        String userId = "user123";
        UserUpdateRequestDto request = new UserUpdateRequestDto("홍길동", "hong@example.com", "01012345678", "newPassword");

        when(userService.updateUser(eq(userId), any(UserUpdateRequestDto.class)))
                .thenReturn(new User());

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원정보가 수정되었습니다."));

        verify(userService, times(1)).updateUser(eq(userId), any(UserUpdateRequestDto.class));
    }

    @Test
    @DisplayName("회원정보 조회 성공")
    void testGetUserInfo_Success() throws Exception {
        // Given
        String userId = "user123";  // 요청할 userId
        UserInfoResponseDto responseDto = new UserInfoResponseDto(
                userId,
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "01012345678",
                "hong@example.com",
                LocalDate.of(2020, 1, 1),
                Rank.GENERAL,
                List.of("ROLE_USER", "ROLE_ADMIN")
        );

        // when(userService.getUserInfo(userId)) 메소드가 responseDto를 반환하도록 설정
        when(userService.getUserInfo(eq(userId))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/users/info")
                        .header("userId", userId)  // 헤더에 userId 추가
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // 상태 코드 200 OK
                .andExpect(jsonPath("$.id").value(userId))  // id 필드 검증
                .andExpect(jsonPath("$.name").value("홍길동"))  // name 필드 검증
                .andExpect(jsonPath("$.birth").value("1990-01-01"))  // birth 필드 검증
                .andExpect(jsonPath("$.phoneNumber").value("01012345678"))  // phoneNumber 필드 검증
                .andExpect(jsonPath("$.email").value("hong@example.com"))  // email 필드 검증
                .andExpect(jsonPath("$.signUpDate").value("2020-01-01"))  // signUpDate 필드 검증
                .andExpect(jsonPath("$.rankName").value("GENERAL"))  // rankName 필드 검증
                .andExpect(jsonPath("$.roleNames").isArray())  // roleNames 필드가 배열임을 검증
                .andExpect(jsonPath("$.roleNames[0]").value("ROLE_USER"))  // roleNames[0] 검증
                .andExpect(jsonPath("$.roleNames[1]").value("ROLE_ADMIN"));  // roleNames[1] 검증

    }

    @Test
    @DisplayName("회원정보 조회 실패: 유저를 찾을 수 없음")
    void testGetUserInfo_UserNotFound() throws Exception {
        // Given
        String userId = "nonexistentUser";  // 존재하지 않는 userId

        // when(userService.getUserInfo(userId)) 메소드가 예외를 던지도록 설정
        when(userService.getUserInfo(eq(userId))).thenThrow(new UserNotFoundException("User not found with id: " + userId));

        // When & Then
        mockMvc.perform(get("/api/users/info")
                        .header("userId", userId)  // 헤더에 userId 추가
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());  // 상태 코드 404 NOT FOUND

        verify(userService, times(1)).getUserInfo(eq(userId));  // userService.getUserInfo가 한 번 호출됐는지 검증
    }

}