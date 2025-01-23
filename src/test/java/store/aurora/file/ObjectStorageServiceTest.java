package store.aurora.file;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectStorageServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private TokenManager tokenManager;
    @Mock private MultipartFile mockFile;

    @InjectMocks
    private ObjectStorageService objectStorageService;

    private static final String STORAGE_URL = "https://storage.example.com";
    private static final String CONTAINER_NAME = "test-container";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(objectStorageService, "storageUrl", "https://storage.example.com");
        ReflectionTestUtils.setField(objectStorageService, "containerName", "test-container");    }

    @Test
    @DisplayName("uploadObject - 파일이 null 또는 비어 있으면 예외 발생")
    void uploadObject_ShouldThrowException_WhenFileIsNullOrEmpty() {
        when(mockFile.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> objectStorageService.uploadObject(null))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageContaining("파일 업로드 실패");

        assertThatThrownBy(() -> objectStorageService.uploadObject(mockFile))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageContaining("파일 업로드 실패");
    }



    @Test
    @DisplayName("uploadObjectFromUrl - null 또는 빈 URL이면 예외 발생")
    void uploadObjectFromUrl_ShouldThrowException_WhenUrlIsNullOrEmpty() {
        assertThatThrownBy(() -> objectStorageService.uploadObjectFromUrl(null))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageContaining("유효하지 않은 이미지 URL");

        assertThatThrownBy(() -> objectStorageService.uploadObjectFromUrl(""))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageContaining("유효하지 않은 이미지 URL");
    }



    @Test
    @DisplayName("deleteObject - null 또는 빈 URL이면 동작하지 않음")
    void deleteObject_ShouldDoNothing_WhenUrlIsNullOrEmpty() {
        objectStorageService.deleteObject(null);
        objectStorageService.deleteObject("");

        verify(restTemplate, never()).execute(any(URI.class), eq(HttpMethod.DELETE), any(), any());
    }

  

    @Test
    @DisplayName("generateUniqueFileName - 고유한 파일명이 생성됨")
    void generateUniqueFileName_ShouldReturnUniqueName() {
        // Given
        String originalFilename = "test.jpg";

        // When
        String generatedFileName = objectStorageService.generateUniqueFileName(originalFilename);

        // Then
        assertThat(generatedFileName).contains(".");
        assertThat(FilenameUtils.getExtension(generatedFileName)).isEqualTo("jpg");
    }

    @Test
    @DisplayName("generatePublicUrl - 올바른 URL이 생성됨")
    void generatePublicUrl_ShouldReturnCorrectUrl() {
        // Given
        String objectName = UUID.randomUUID().toString() + ".jpg";

        // When
        String publicUrl = objectStorageService.generatePublicUrl(objectName);

        // Then
        assertThat(publicUrl).isEqualTo(STORAGE_URL + "/" + CONTAINER_NAME + "/" + objectName);
    }
}