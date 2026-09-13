package bank_api.common;

/**
 * 統一的 API 錯誤回應格式。
 * 前端可以依 status 與 message 顯示明確提示，而不是解析 HTML 錯誤頁。
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path) {
}
