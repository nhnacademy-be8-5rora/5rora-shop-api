package store.aurora.user.service;

import store.aurora.user.dto.*;
import store.aurora.user.entity.User;

import java.util.List;

import java.time.LocalDateTime;

public interface UserService {
    User registerUser(SignUpRequest signUpRequest);
    User registerOauthUser(SignUpRequest signUpRequest);
    void deleteUser(String userId);
    void reactivateUser(String userId);
    void checkAndSetSleepStatus();
    User getUser(String userId);
    UserDetailResponseDto getPasswordAndRole(String userId);
    UserResponseDto getUserByUserId(String userId);
    Boolean isUserExists(String userId);
    UserInfoResponseDto getUserInfo(String userId);
    List<String> searchByMonth(int currentMonth);
    void updateLastLogin(String userId, LocalDateTime lastLogin);
    User updateUser(String userId, UserUpdateRequestDto request);
}