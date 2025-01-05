package store.aurora.common.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import store.aurora.common.dto.ErrorResponseDto;
import store.aurora.common.dto.ValidationErrorResponse;
import store.aurora.common.exception.DataAlreadyExistsException;
import store.aurora.common.exception.DataNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFoundExceptions(RuntimeException e) {
        return createResponseEntity(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleAlreadyException(Exception e) {
        return createResponseEntity(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleEnumParseError(HttpMessageNotReadableException ex) {
        if (ex.getMessage().contains("Cannot deserialize value of type")) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Enum 파싱 에러: 요청 값이 Enum 타입에 맞지 않습니다." + ex.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        return createResponseEntity(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseDto> createResponseEntity(Exception e, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponseDto(
                        e.getMessage(),
                        status.value(),
                        LocalDateTime.now()
                ));
    }
}
