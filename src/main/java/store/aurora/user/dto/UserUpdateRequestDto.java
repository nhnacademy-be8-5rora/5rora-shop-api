package store.aurora.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserUpdateRequestDto {
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
}
