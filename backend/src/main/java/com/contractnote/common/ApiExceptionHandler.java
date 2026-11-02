package com.contractnote.common;

import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.contractnote.ai.AiFailedException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** 오류를 화면에서 보여 줄 수 있는 {"message": ...} 형식으로 바꾼다. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String, String>> badRequest(BadRequestException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        var field = e.getBindingResult().getFieldError();
        String message = switch (field == null ? "" : field.getField()) {
            case "clientName" -> "고객 이름은 비워 둘 수 없습니다.";
            case "question" -> "질문을 입력해 주세요.";
            default -> "입력값을 확인해 주세요.";
        };
        return body(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "파일이 너무 큽니다. 파일당 20MB까지 올릴 수 있습니다.");
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    ResponseEntity<Map<String, String>> noFile(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "파일을 한 개 이상 올려 주세요.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> unreadable(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "입력값의 형식을 확인해 주세요. 날짜는 YYYY-MM-DD, 금액은 숫자로 적습니다.");
    }

    @ExceptionHandler(AiFailedException.class)
    ResponseEntity<Map<String, String>> aiFailed(AiFailedException e) {
        return body(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<Map<String, String>> aiUnauthorized(UnauthorizedException e) {
        log.error("Claude API 인증 실패", e);
        return body(HttpStatus.BAD_GATEWAY, "Claude API 키를 확인해 주세요.");
    }

    @ExceptionHandler(RateLimitException.class)
    ResponseEntity<Map<String, String>> aiRateLimited(RateLimitException e) {
        log.warn("Claude API 호출 한도 초과", e);
        return body(HttpStatus.TOO_MANY_REQUESTS, "AI 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(AnthropicException.class)
    ResponseEntity<Map<String, String>> aiError(AnthropicException e) {
        log.error("Claude API 호출 실패", e);
        return body(HttpStatus.BAD_GATEWAY, "AI 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(GenAiIOException.class)
    ResponseEntity<Map<String, String>> geminiConnection(GenAiIOException e) {
        log.error("Gemini API 연결 실패", e);
        return body(HttpStatus.GATEWAY_TIMEOUT, "AI 응답이 없습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> geminiError(ApiException e) {
        log.error("Gemini API 호출 실패: {}", e.code(), e);
        return switch (e.code()) {
            case 429 -> body(HttpStatus.TOO_MANY_REQUESTS, String.valueOf(e.getMessage()).contains("per day")
                    ? "Gemini 무료 사용량 한도에 걸렸습니다. 한국 시간 오후 5시 이후 다시 시도해 주세요. 그래도 안 되면 다음 날 다시 시도해 주세요."
                    : "AI 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
            case 400 -> body(HttpStatus.BAD_GATEWAY, "AI가 파일을 읽지 못했습니다. 파일이 손상되지 않았는지 확인해 주세요.");
            case 401, 403 -> body(HttpStatus.BAD_GATEWAY, "Gemini API 키를 확인해 주세요.");
            default -> body(HttpStatus.BAD_GATEWAY, "AI 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        };
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
