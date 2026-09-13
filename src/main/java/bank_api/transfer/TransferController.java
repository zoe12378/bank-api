package bank_api.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 提供轉帳 HTTP API。
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "只能從登入者自己擁有的帳戶轉出款項。")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "進行轉帳")
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        return transferService.transfer(request, authentication.getName());
    }
}
