package store.aurora.book.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.aurora.book.dto.publisher.PublisherRequestDto;
import store.aurora.book.dto.publisher.PublisherResponseDto;
import store.aurora.book.entity.Publisher;


public interface PublisherService {

    Page<PublisherResponseDto> getAllPublishers(Pageable pageable);

    PublisherResponseDto getPublisherById(Long id);

    void createPublisher(PublisherRequestDto requestDto);

    void updatePublisher(Long id, PublisherRequestDto requestDto);

    void deletePublisher(Long id);

    Publisher getOrCreatePublisher(String name);
}
