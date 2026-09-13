package bank_api.transaction;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 分頁資訊讓前端知道目前頁數、總筆數與是否還有下一頁。
 */
public record TransactionPageResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static TransactionPageResponse from(Page<AccountTransaction> transactionPage) {
        return new TransactionPageResponse(
                transactionPage.getContent().stream().map(TransactionResponse::from).toList(),
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages());
    }
}
