package store.aurora.book.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.series.SeriesRequestDto;
import store.aurora.book.dto.series.SeriesResponseDto;
import store.aurora.book.entity.Series;

import java.util.List;

public interface SeriesService {

    Page<SeriesResponseDto> getAllSeries(Pageable pageable);

    SeriesResponseDto getSeriesById(Long id);

    @Transactional
    void createSeries(SeriesRequestDto requestDto);

    @Transactional
    void updateSeries(Long id, SeriesRequestDto requestDto);

    @Transactional
    void deleteSeries(Long id);

    @Transactional
    Series getOrCreateSeries(String name);
}
