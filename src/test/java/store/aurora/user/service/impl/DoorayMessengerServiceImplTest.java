package store.aurora.user.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import store.aurora.user.client.MessageSendClient;
import store.aurora.user.dto.MessagePayload;
import store.aurora.user.service.DoorayMessengerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DoorayMessengerServiceImplTest {

    @Mock
    private MessageSendClient messageSendClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DoorayMessengerServiceImpl doorayMessengerService;

    private String phoneNumber;
    private String verificationCode;

    @BeforeEach
    void setUp() {
        phoneNumber = "01012345678";
        verificationCode = "123456";

        // mock valueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("인증 코드 전송 성공 테스트")
    void testSendVerificationCode() {
        // Given
        MessagePayload messagePayload = new MessagePayload();
        messagePayload.setBotName("5rora");
        messagePayload.setText("인증번호 : " + verificationCode);

        // When
        doorayMessengerService.sendVerificationCode(phoneNumber, verificationCode);

        // Then
        // Redis에 인증 코드 저장 확인
        verify(valueOperations, times(1)).set(eq(phoneNumber), eq(verificationCode), any());

        // 메세지 전송 확인
        verify(messageSendClient, times(1)).sendMessage(eq(messagePayload), any(), any(), any());
    }

    @Test
    @DisplayName("인증 코드 검증 성공 테스트")
    void testVerifyCodeSuccess() {
        // Given
        when(valueOperations.get(phoneNumber)).thenReturn(verificationCode);

        // When
        boolean isVerified = doorayMessengerService.verifyCode(phoneNumber, verificationCode);

        // Then
        assertThat(isVerified).isTrue();
        // 인증 성공 후 Redis에 "_verified" 값 설정 확인
        verify(valueOperations, times(1)).set(eq(phoneNumber + "_verified"), eq("true"), any());
        // 인증 코드 삭제 확인
        verify(redisTemplate, times(1)).delete(eq(phoneNumber));
    }

    @Test
    @DisplayName("인증 코드 검증 실패 테스트 (잘못된 코드 입력)")
    void testVerifyCodeFailure() {
        // Given
        String wrongCode = "654321";
        when(valueOperations.get(phoneNumber)).thenReturn(verificationCode);

        // When
        boolean isVerified = doorayMessengerService.verifyCode(phoneNumber, wrongCode);

        // Then
        assertThat(isVerified).isFalse();
        // Redis에서 인증 코드 삭제 확인은 하지 않음
        verify(redisTemplate, never()).delete(eq(phoneNumber));
    }

    @Test
    @DisplayName("인증 코드 검증 실패 테스트 (인증 코드 없음)")
    void testVerifyCodeNoCode() {
        // Given
        when(valueOperations.get(phoneNumber)).thenReturn(null);

        // When
        boolean isVerified = doorayMessengerService.verifyCode(phoneNumber, verificationCode);

        // Then
        assertThat(isVerified).isFalse();
        // Redis에서 인증 코드 삭제 확인은 하지 않음
        verify(redisTemplate, never()).delete(eq(phoneNumber));
    }
}
