package store.aurora.book.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.series.SeriesRequestDto;
import store.aurora.book.dto.series.SeriesResponseDto;
import store.aurora.book.entity.Series;
import store.aurora.book.exception.publisher.PublisherLinkedToBooksException;
import store.aurora.book.exception.series.SeriesAlreadyExistsException;
import store.aurora.book.exception.series.SeriesLinkedToBooksException;
import store.aurora.book.exception.series.SeriesNotFoundException;
import store.aurora.book.repository.BookRepository;
import store.aurora.book.repository.SeriesRepository;
import store.aurora.book.service.SeriesService;

@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {
    private final SeriesRepository seriesRepository;
    private final BookRepository bookRepository;

    @Override
    public Page<SeriesResponseDto> getAllSeries(Pageable pageable) {
        return seriesRepository.findAllByOrderById(pageable)
                .map(series -> new SeriesResponseDto(series.getId(), series.getName()));
    }
    @Override
    public SeriesResponseDto getSeriesById(Long id) {
        Series series = findById(id);
        return new SeriesResponseDto(series.getId(), series.getName());
    }

    @Transactional
    @Override
    public void createSeries(SeriesRequestDto requestDto) {
        validateDuplicateName(requestDto.getName());
        Series series = new Series(requestDto.getName());
        seriesRepository.save(series);
    }
    @Transactional
    @Override
    public void updateSeries(Long id, SeriesRequestDto requestDto) {
        Series series = findById(id);
        validateDuplicateName(requestDto.getName());
        series.setName(requestDto.getName());
        seriesRepository.save(series);
    }

    @Transactional
    @Override
    public void deleteSeries(Long id) {
        if (!seriesRepository.existsById(id)) {
            throw new SeriesNotFoundException("시리즈를 찾을 수 없습니다. ID: " + id);
        }
        boolean isLinkedToBooks = bookRepository.existsBySeriesId(id);
        if (isLinkedToBooks) {
            throw new SeriesLinkedToBooksException("시리즈와 연결된 책이 있어 삭제할 수 없습니다. ID: " + id);
        }
        seriesRepository.deleteById(id);
    }
    @Transactional
    @Override
    public Series getOrCreateSeries(String name) {
        return seriesRepository.findByName(name)
                .orElseGet(() -> seriesRepository.save(new Series(name)));
    }

    // 중복된 이름 예외 처리
    private void validateDuplicateName(String name) {
        if (seriesRepository.findByName(name).isPresent()) {
            throw new SeriesAlreadyExistsException("이미 존재하는 시리즈입니다: " + name);
        }
    }

    // ID로 시리즈 조회
    private Series findById(Long id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new SeriesNotFoundException("시리즈를 찾을 수 없습니다. ID: " + id));
    }
}
