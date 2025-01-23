package store.aurora.book.service.image.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.BookImage;
import store.aurora.book.exception.book.BookNotFoundException;
import store.aurora.book.exception.image.ImageNotFoundException;
import store.aurora.book.repository.image.BookImageRepository;
import store.aurora.file.ObjectStorageService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookImageServiceImplTest {

    @InjectMocks
    private BookImageServiceImpl bookImageService;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private BookImageRepository bookImageRepository;

    @Mock
    private MultipartFile mockFile;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("테스트 책");
    }

    @Test
    @DisplayName("processBookImages - coverUrl, coverImage, additionalImages 모두 정상 처리 및 modifyCoverUrl 검증")
    void processBookImages_ShouldProcessAllInputs_WithModifyCoverUrlCheck() {
        // Given
        String coverUrl = "https://example.com/coversum/abc.jpg";
        String expectedModifiedUrl = "https://example.com/cover500/abc.jpg"; // 기대하는 변환 결과
        MultipartFile coverImage = mock(MultipartFile.class);
        List<MultipartFile> additionalImages = List.of(mock(MultipartFile.class), mock(MultipartFile.class));

        when(coverImage.isEmpty()).thenReturn(false);
        when(objectStorageService.uploadObjectFromUrl(anyString())).thenReturn(expectedModifiedUrl);
        when(objectStorageService.uploadObject(any(MultipartFile.class))).thenReturn("https://example.com/uploaded.jpg");

        // When
        bookImageService.processBookImages(testBook, coverUrl, coverImage, additionalImages);

        // Then
        // modifyCoverUrl이 적용된 후 올바른 URL로 objectStorageService가 호출되었는지 검증
        verify(objectStorageService, times(1)).uploadObjectFromUrl(eq(expectedModifiedUrl));

        // 사용자가 직접 업로드한 커버 이미지 및 추가 이미지가 정상적으로 처리되었는지 검증
        verify(objectStorageService, times(3)).uploadObject(any(MultipartFile.class));
    }
    @Test
    @DisplayName("handleImageUpload - 파일이 null이면 업로드 하지 않음")
    void handleImageUpload_ShouldNotUpload_WhenFileIsNull() {
        // When
        bookImageService.handleImageUpload(testBook, null, true);

        // Then
        verify(objectStorageService, never()).uploadObject(any());
    }

    @Test
    @DisplayName("handleImageUpload - 파일이 비어 있으면 업로드 하지 않음")
    void handleImageUpload_ShouldNotUpload_WhenFileIsEmpty() {
        // Given
        when(mockFile.isEmpty()).thenReturn(true);

        // When
        bookImageService.handleImageUpload(testBook, mockFile, true);

        // Then
        verify(objectStorageService, never()).uploadObject(any());
    }

    @Test
    @DisplayName("handleAdditionalImages - 추가 이미지가 null이거나 비어있으면 실행되지 않음")
    void handleAdditionalImages_ShouldNotProcess_WhenEmpty() {
        // When
        bookImageService.handleAdditionalImages(testBook, Collections.emptyList());

        // Then
        verify(objectStorageService, never()).uploadObject(any());
    }

    @Test
    @DisplayName("deleteImages - imageIds가 null 또는 비어 있으면 예외 발생")
    void deleteImages_ShouldThrowException_WhenImageIdsEmpty() {
        assertThatThrownBy(() -> bookImageService.deleteImages(Collections.emptyList()))
                .isInstanceOf(ImageNotFoundException.class)
                .hasMessage("삭제할 이미지 목록이 비어 있습니다.");
    }

    @Test
    @DisplayName("deleteImages - 저장된 이미지가 없으면 예외 발생")
    void deleteImages_ShouldThrowException_WhenNoImagesFound() {
        // Given
        List<Long> imageIds = List.of(1L, 2L);
        when(bookImageRepository.findAllById(imageIds)).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookImageService.deleteImages(imageIds))
                .isInstanceOf(ImageNotFoundException.class)
                .hasMessageContaining("삭제할 이미지를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("deleteImages - Book이 null이면 예외 발생")
    void deleteImages_ShouldThrowException_WhenBookIsNull() {
        // Given
        List<Long> imageIds = List.of(1L);
        BookImage bookImage = mock(BookImage.class);
        when(bookImage.getBook()).thenReturn(null);
        when(bookImageRepository.findAllById(imageIds)).thenReturn(List.of(bookImage));

        // When & Then
        assertThatThrownBy(() -> bookImageService.deleteImages(imageIds))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("이미지에 연결된 책을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("handleImageUpload - 파일 업로드 성공 시 BookImage가 추가됨")
    void testHandleImageUpload_Success() {
        when(objectStorageService.uploadObject(mockFile)).thenReturn("https://storage.example.com/image.jpg");

        bookImageService.handleImageUpload(testBook, mockFile, false);

        verify(objectStorageService).uploadObject(mockFile);
    }


    @Test
    @DisplayName("deleteImages - 이미지 삭제 성공")
    void testDeleteImages_Success() {
        BookImage bookImage = new BookImage(testBook, "https://storage.example.com/image.jpg", false);
        when(bookImageRepository.findAllById(anyList())).thenReturn(List.of(bookImage));

        bookImageService.deleteImages(List.of(1L));

        verify(objectStorageService).deleteObject(bookImage.getFilePath());
        verify(bookImageRepository).delete(bookImage);
    }


    @Test
    @DisplayName("getThumbnail - 책의 썸네일 이미지를 정상적으로 가져옴")
    void testGetThumbnail() {
        BookImage thumbnail = new BookImage(testBook, "https://storage.example.com/thumbnail.jpg", true);
        testBook.addBookImage(thumbnail);

        assertThat(bookImageService.getThumbnail(testBook)).isEqualTo(thumbnail);
    }

    @Test
    @DisplayName("getAdditionalImages - 책의 추가 이미지만 가져옴")
    void testGetAdditionalImages() {
        BookImage additionalImage = new BookImage(testBook, "https://storage.example.com/additional.jpg", false);
        testBook.addBookImage(additionalImage);

        assertThat(bookImageService.getAdditionalImages(testBook)).containsExactly(additionalImage);
    }

    @Test
    @DisplayName("handleAdditionalImages - 여러 개의 추가 이미지 업로드 성공")
    void testHandleAdditionalImages_Success() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);

        when(file1.isEmpty()).thenReturn(false);
        when(file2.isEmpty()).thenReturn(false);
        when(objectStorageService.uploadObject(file1)).thenReturn("https://storage.example.com/image1.jpg");
        when(objectStorageService.uploadObject(file2)).thenReturn("https://storage.example.com/image2.jpg");

        bookImageService.handleAdditionalImages(testBook, List.of(file1, file2));

        verify(objectStorageService).uploadObject(file1);
        verify(objectStorageService).uploadObject(file2);
    }
}