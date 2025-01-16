package store.aurora.book.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.aurora.book.dto.series.SeriesRequestDto;
import store.aurora.book.dto.series.SeriesResponseDto;
import store.aurora.book.entity.Series;
import store.aurora.book.service.SeriesService;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    // 모든 시리즈 조회 (페이징)
    @GetMapping
    public ResponseEntity<Page<SeriesResponseDto>> getAllSeries(Pageable pageable) {
        Page<SeriesResponseDto> series = seriesService.getAllSeries(pageable);
        return ResponseEntity.ok(series);
    }

    // 특정 시리즈 조회
    @GetMapping("/{id}")
    public ResponseEntity<SeriesResponseDto> getSeriesById(@PathVariable Long id) {
        SeriesResponseDto series = seriesService.getSeriesById(id);
        return ResponseEntity.ok(series);
    }

    // 시리즈 생성
    @PostMapping
    public ResponseEntity<Void> createSeries(@Valid @RequestBody SeriesRequestDto requestDto) {
        seriesService.createSeries(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 상태 코드만 반환
    }

    // 시리즈 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSeries(@PathVariable Long id, @Valid @RequestBody SeriesRequestDto requestDto) {
        seriesService.updateSeries(id, requestDto);
        return ResponseEntity.noContent().build(); // 상태 코드만 반환
    }

    // 시리즈 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long id) {
        seriesService.deleteSeries(id);
        return ResponseEntity.noContent().build(); // 상태 코드만 반환
    }
}
