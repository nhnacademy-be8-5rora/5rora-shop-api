package store.aurora.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import store.aurora.user.entity.Rank;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserInfoResponseDto {
    private String id;
    private String name;
    private LocalDate birth;
    private String phoneNumber;
    private String email;
    private LocalDate signUpDate;
    private Rank rankName;
    private List<String> roleNames;
}
