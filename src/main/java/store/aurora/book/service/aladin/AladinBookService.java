package store.aurora.book.service.aladin;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.dto.aladin.AladinBookRequestDto;

import java.util.List;

public interface AladinBookService {
    void saveBookFromApi(AladinBookRequestDto bookDto, List<MultipartFile> additionalImages);

    List<AladinBookRequestDto> searchBooks(String query, String queryType, String searchTarget, int start);

    AladinBookRequestDto getBookDetailsByIsbn(String isbn13);
}
