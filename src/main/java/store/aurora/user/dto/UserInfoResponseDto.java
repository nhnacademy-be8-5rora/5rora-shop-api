package store.aurora.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import store.aurora.user.entity.Rank;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
public class UserInfoResponseDto {
    // id, 이름, 생년월일, 전화번호, 이메일, 등급
    private String id;
    private String name;
    private LocalDate birth;
    private String phoneNumber;
    private String email;
    private Rank rankName;
}
