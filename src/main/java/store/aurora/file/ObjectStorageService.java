package store.aurora.file;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    @Value("${nhncloud.storage.url}")
    private String storageUrl;

    @Value("${nhncloud.storage.container}")
    private String containerName;

    private final RestTemplate restTemplate;
    private final TokenManager tokenManager;
    private static final String FILE_URL_FORMAT = "%s/%s/%s";
    private static final String AUTH_HEADER = "X-Auth-Token";  // SonarQube 경고 해결



    public String uploadObject(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ObjectStorageException("파일 업로드 실패: 업로드할 파일이 비어 있거나 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        // 고유한 파일 이름 생성
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

        // 업로드 URL 생성
        String url = String.format(FILE_URL_FORMAT, storageUrl, containerName, uniqueFileName);
        URI uri = URI.create(url);

        restTemplate.execute(uri, HttpMethod.PUT, request -> {
            request.getHeaders().set(AUTH_HEADER, tokenManager.getToken());

            try (InputStream inputStream = file.getInputStream()) {
                StreamUtils.copy(inputStream, request.getBody());
            }
        }, response -> {
            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new ObjectStorageException("파일 업로드 실패: 오브젝트 스토리지에서 파일을 저장하는 데 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return null;
        });

        return generatePublicUrl(uniqueFileName);
    }

    public String uploadObjectFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new ObjectStorageException("파일 업로드 실패: 유효하지 않은 이미지 URL입니다.", HttpStatus.BAD_REQUEST);
        }
        // 이미지 다운로드
        byte[] imageBytes = restTemplate.execute(URI.create(imageUrl), HttpMethod.GET, null, response -> {
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new ObjectStorageException("파일 업로드 실패: 외부 이미지 다운로드에 실패했습니다. 응답 코드: " + response.getStatusCode(), HttpStatus.NOT_FOUND);
            }
            return StreamUtils.copyToByteArray(response.getBody());
        });

        if (imageBytes == null || imageBytes.length == 0) {
            throw new ObjectStorageException("파일 업로드 실패: 다운로드된 이미지 데이터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 고유한 파일 이름 생성
        String extension = FilenameUtils.getExtension(imageUrl);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? ".jpg" : "." + extension);

        // 업로드 URL 생성
        String url = String.format(FILE_URL_FORMAT, storageUrl, containerName, uniqueFileName);
        URI uri = URI.create(url);

        // 업로드 요청
        restTemplate.execute(uri, HttpMethod.PUT, request -> {
            request.getHeaders().set(AUTH_HEADER, tokenManager.getToken());
            StreamUtils.copy(imageBytes, request.getBody());
        }, response -> {
            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new ObjectStorageException("파일 업로드 실패: 오브젝트 스토리지에서 파일을 저장하는 데 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return null;
        });

        return generatePublicUrl(uniqueFileName);
    }

    public void deleteObject(String objectUrl) {
        if (objectUrl == null || objectUrl.isEmpty()) {
            return;
        }
        // 삭제 URL 생성
        URI uri = URI.create(objectUrl);

        // REST 요청 실행
        try {
            restTemplate.execute(uri, HttpMethod.DELETE, request ->
                    request.getHeaders().set(AUTH_HEADER, tokenManager.getToken()),
            response -> {
                if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                    throw new ObjectStorageException("파일 삭제 실패: 오브젝트 스토리지에서 삭제를 수행할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);
                }
                return null;
            });
        } catch (Exception e) {
            throw new ObjectStorageException("파일 삭제 실패: 알 수 없는 오류 발생. " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public String generateUniqueFileName(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        return UUID.randomUUID() + "." + extension;
    }

    public String generatePublicUrl(String objectName) {
        return String.format(FILE_URL_FORMAT, storageUrl, containerName, objectName);
    }
}