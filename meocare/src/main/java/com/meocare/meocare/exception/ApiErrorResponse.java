package com.meocare.meocare.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Cấu trúc JSON lỗi thống nhất. fieldErrors chỉ có giá trị với lỗi validation, các trường hợp khác là null. */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
