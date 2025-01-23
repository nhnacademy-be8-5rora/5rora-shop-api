package store.aurora.order.controller;

import feign.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import store.aurora.order.dto.WrapResponseDTO;
import store.aurora.order.entity.Wrap;
import store.aurora.order.service.WrapService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/wrap")
public class WrapController {

    private final WrapService wrapService;

    private static final String NONE_SELECT = "선택 안함";

    @GetMapping
    public ResponseEntity<List<WrapResponseDTO>> getWrapList(){
        List<WrapResponseDTO> wrapList = convertWrapEntityListToDtoList(wrapService.getWraps());
        return ResponseEntity.ok(wrapList);
    }

    @PostMapping
    public ResponseEntity<String> createWrap(@RequestBody WrapResponseDTO wrap){
        wrapService.createWrap(Wrap.builder()
                .name(wrap.getName())
                .amount(wrap.getAmount())
                .build()
        );

        return ResponseEntity.ok("Wrap 생성 완료");
    }

    @PatchMapping
    public ResponseEntity<Response> updateWrap(@RequestBody WrapResponseDTO wrap){
        wrapService.updateWrap(Wrap.builder()
                .id(wrap.getId())
                .name(wrap.getName())
                .amount(wrap.getAmount())
                .build()
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public void deleteWrap(@RequestParam Long id){
        if(id == -1){
            return;
        }
        wrapService.deleteByWrapId(id);
    }

    private List<WrapResponseDTO> convertWrapEntityListToDtoList(List<Wrap> wraps){
        List<WrapResponseDTO> wrapResponseDTOList = new ArrayList<>();

        wrapResponseDTOList.add(
                WrapResponseDTO.builder()
                        .id(-1L)
                        .name(NONE_SELECT)
                        .amount(0)
                        .build());

        for (Wrap wrap : wraps) {
            WrapResponseDTO wrapResponseDTO = WrapResponseDTO.builder()
                    .id(wrap.getId())
                    .name(wrap.getName())
                    .amount(wrap.getAmount())
                    .build();

            wrapResponseDTOList.add(wrapResponseDTO);
        }

        return wrapResponseDTOList;
    }
}
