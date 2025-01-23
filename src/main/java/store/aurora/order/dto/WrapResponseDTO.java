package store.aurora.order.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class WrapResponseDTO {
    private Long id;
    private String name;
    private Integer amount;
}
