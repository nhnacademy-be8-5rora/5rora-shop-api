//package store.aurora.file;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.*;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//import store.aurora.key.KeyConfig;
//import store.aurora.key.KeyManagerException;
//import store.aurora.key.KeyManagerJsonParsingException;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeParseException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class TokenManagerTest {
//
//    @Mock private RestTemplate restTemplate;
//    @Mock private KeyConfig keyConfig;
//
//    @InjectMocks
//    private TokenManager tokenManager;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        tokenManager = new TokenManager(restTemplate, keyConfig);
//    }
//
//    @Test
//    @DisplayName("getToken - 기존 토큰이 유효하면 그대로 반환")
//    void getToken_ShouldReturnExistingToken_WhenTokenIsValid() {
//        // Given
//        ReflectionTestUtils.setField(tokenManager, "token", "existing-valid-token");
//        ReflectionTestUtils.setField(tokenManager, "tokenExpiry", LocalDateTime.now().plusMinutes(10));
//
//        // When
//        String token = tokenManager.getToken();
//
//        // Then
//        assertThat(token).isEqualTo("existing-valid-token"); // 기존 토큰 반환
//        verify(restTemplate, never()).postForObject(anyString(), any(), any());
//    }
//
//    @Test
//    @DisplayName("getToken - 기존 토큰이 만료되었을 때 새 토큰 요청")
//    void getToken_ShouldRefreshToken_WhenTokenIsExpired() {
//        // Given
//        ReflectionTestUtils.setField(tokenManager, "token", "existing-valid-token");
//        ReflectionTestUtils.setField(tokenManager, "tokenExpiry", LocalDateTime.now().plusMinutes(1));
//
//        // Mock Key Manager에서 반환하는 JSON 데이터
//        String keyManagerResponse = """
//            {
//                "tenantId": "test-tenant",
//                "username": "test-user",
//                "password": "test-pass"
//            }
//            """;
//        when(keyConfig.keyStore(anyString())).thenReturn(keyManagerResponse);
//
//        // Mock API 응답
//        String apiResponse = """
//            {
//                "access": {
//                    "token": {
//                        "id": "new-token",
//                        "expires": "2024-12-31T23:59:59Z"
//                    }
//                }
//            }
//            """;
//        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(apiResponse);
//
//        // When
//        String token = tokenManager.getToken();
//
//        // Then
//        assertThat(token).isEqualTo("new-token"); // 새 토큰 반환
//        verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
//    }
//
//    @Test
//    @DisplayName("refreshToken - 네트워크 오류 발생 시 예외 발생")
//    void refreshToken_ShouldThrowException_WhenNetworkErrorOccurs() {
//        // Given
//        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
//                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
//
//        // When & Then
//        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenManager, "refreshToken"))
//                .isInstanceOf(TokenRefreshException.class)
//                .hasMessageContaining("토큰 요청이 실패했습니다");
//
//        verify(restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
//    }
//
//    @Test
//    @DisplayName("refreshToken - 응답이 비어 있으면 예외 발생")
//    void refreshToken_ShouldThrowException_WhenResponseIsEmpty() {
//        // Given
//        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn("");
//
//        // When & Then
//        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenManager, "refreshToken"))
//                .isInstanceOf(TokenRefreshException.class)
//                .hasMessageContaining("토큰 응답이 비어 있습니다.");
//    }
//
//    @Test
//    @DisplayName("refreshToken - 응답에서 만료 시간이 잘못된 경우 예외 발생")
//    void refreshToken_ShouldThrowException_WhenExpiryFormatIsInvalid() {
//        // Given
//        String invalidApiResponse = """
//            {
//                "access": {
//                    "token": {
//                        "id": "new-token",
//                        "expires": "INVALID_FORMAT"
//                    }
//                }
//            }
//            """;
//        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
//                .thenReturn(invalidApiResponse);
//
//        // When & Then
//        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenManager, "refreshToken"))
//                .isInstanceOf(TokenRefreshException.class)
//                .hasMessageContaining("토큰 만료 시간 파싱에 실패했습니다.");
//    }
//
//    @Test
//    @DisplayName("loadCredentials - Key Manager에서 올바른 인증 정보를 가져옴")
//    void loadCredentials_ShouldLoadValidCredentials() {
//        // Given
//        String keyManagerResponse = """
//        {
//            "tenantId": "test-tenant",
//            "username": "test-user",
//            "password": "test-pass"
//        }
//        """;
//        doReturn(keyManagerResponse).when(keyConfig).keyStore(anyString()); // ✅ 인자 검사 완화
//
//        // When
//        ReflectionTestUtils.invokeMethod(tokenManager, "loadCredentials");
//
//        // Then
//        verify(keyConfig, times(1)).keyStore(anyString());
//        assertThat(ReflectionTestUtils.getField(tokenManager, "tenantId")).isEqualTo("test-tenant");
//        assertThat(ReflectionTestUtils.getField(tokenManager, "username")).isEqualTo("test-user");
//        assertThat(ReflectionTestUtils.getField(tokenManager, "password")).isEqualTo("test-pass");
//    }
//
//    @Test
//    @DisplayName("loadCredentials - Key Manager 응답이 잘못된 경우 예외 발생")
//    void loadCredentials_ShouldThrowException_WhenKeyManagerResponseIsInvalid() {
//        // Given
//        String invalidJson = "{ \"invalid\": \"data\" }";
//        when(keyConfig.keyStore(anyString())).thenReturn(invalidJson);
//
//        // When & Then
//        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenManager, "loadCredentials"))
//                .isInstanceOf(KeyManagerException.class)
//                .hasMessageContaining("Key Manager에서 인증 정보가 누락되었습니다");
//    }
//
//    @Test
//    @DisplayName("loadCredentials - JSON 파싱 오류 발생 시 예외 발생")
//    void loadCredentials_ShouldThrowException_WhenJsonParsingFails() {
//        // Given
//        when(keyConfig.keyStore(anyString())).thenReturn("INVALID_JSON");
//
//        // When & Then
//        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenManager, "loadCredentials"))
//                .isInstanceOf(KeyManagerJsonParsingException.class)
//                .hasMessageContaining("Key Manager에서 인증 정보를 JSON으로 변환하는 데 실패했습니다");
//    }
//}