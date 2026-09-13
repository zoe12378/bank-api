package bank_api.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

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
}
