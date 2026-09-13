package bank_api.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

/**
 * 集中處理 Controller 拋出的例外，讓所有 API 使用同一種錯誤 JSON 格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        int statusCode = exception.getStatusCode().value();
        HttpStatus status = HttpStatus.valueOf(statusCode);

        ApiError error = new ApiError(
                LocalDateTime.now().format(DISPLAY_TIME_FORMATTER),
                statusCode,
                status.getReasonPhrase(),
                exception.getReason(),
                request.getRequestURI());

        return ResponseEntity.status(status).body(error);
    }

    /**
     * 把 @Valid 的欄位檢查錯誤轉成和其他 API 一致的 JSON。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .collect(Collectors.joining("；"));

        ApiError error = new ApiError(
                LocalDateTime.now().format(DISPLAY_TIME_FORMATTER),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI());

        return ResponseEntity.badRequest().body(error);
    }
}
