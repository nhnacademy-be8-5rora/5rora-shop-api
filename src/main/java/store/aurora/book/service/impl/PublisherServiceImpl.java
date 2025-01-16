package store.aurora.book.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.publisher.PublisherRequestDto;
import store.aurora.book.dto.publisher.PublisherResponseDto;
import store.aurora.book.entity.Publisher;
import store.aurora.book.exception.publisher.PublisherAlreadyExistsException;
import store.aurora.book.exception.publisher.PublisherLinkedToBooksException;
import store.aurora.book.exception.publisher.PublisherNotFoundException;
import store.aurora.book.repository.BookRepository;
import store.aurora.book.repository.PublisherRepository;
import store.aurora.book.service.PublisherService;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    @Override
    public Page<PublisherResponseDto> getAllPublishers(Pageable pageable) {
        return publisherRepository.findAllByOrderById(pageable)
                .map(publisher -> new PublisherResponseDto(publisher.getId(), publisher.getName()));
    }

    @Override
    public PublisherResponseDto getPublisherById(Long id) {
        Publisher publisher = findById(id);
        return new PublisherResponseDto(publisher.getId(), publisher.getName());
    }

    @Transactional
    @Override
    public void createPublisher(PublisherRequestDto requestDto) {
        validateDuplicateName(requestDto.getName());
        Publisher publisher = new Publisher(requestDto.getName());
        publisherRepository.save(publisher);
    }

    @Transactional
    @Override
    public void updatePublisher(Long id, PublisherRequestDto requestDto) {
        Publisher publisher = findById(id);
        validateDuplicateName(requestDto.getName());
        publisher.setName(requestDto.getName());
        publisherRepository.save(publisher);
    }

    @Transactional
    @Override
    public void deletePublisher(Long id) {
        if (!publisherRepository.existsById(id)) {
            throw new PublisherNotFoundException("출판사를 찾을 수 없습니다. ID: " + id);
        }
        // 연결된 책이 있는지 확인
        boolean isLinkedToBooks = bookRepository.existsByPublisherId(id);
        if (isLinkedToBooks) {
            throw new PublisherLinkedToBooksException("출판사와 연결된 책이 있어 삭제할 수 없습니다. ID: " + id);
        }
        publisherRepository.deleteById(id);
    }

    @Transactional
    @Override
    public Publisher getOrCreatePublisher(String name) {
        return publisherRepository.findByName(name)
                .orElseGet(() -> publisherRepository.save(new Publisher(name)));
    }

    // 중복된 이름 예외 처리
    private void validateDuplicateName(String name) {
        if (publisherRepository.existsByName(name)) {
            throw new PublisherAlreadyExistsException("이미 존재하는 출판사입니다: " + name);
        }
    }

    // ID로 출판사 조회
    private Publisher findById(Long id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException("출판사를 찾을 수 없습니다. ID: " + id));
    }
}