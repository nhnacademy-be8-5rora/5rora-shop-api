package store.aurora.book.dto.series;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeriesRequestDto {
    @NotBlank(message = "시리즈 이름은 필수입니다.")
    @Size(max = 100, message = "시리즈 이름은 최대 100자까지 입력할 수 있습니다.")
    private String name;
}
