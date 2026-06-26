package com.ewallet.dto.response;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper.
 *
 * Provides a standard envelope for all REST responses:
 * - success: boolean indicating operation outcome
 * - data:    the response payload (null on errors)
 * - message: human-readable message (null on success)
 *
 * Static factory methods simplify response construction:
 *   ApiResponse.success(data)   → 200/201 response
 *   ApiResponse.error(message)  → 4xx/5xx response
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
