package bank_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger UI 的基本資訊，以及 JWT Bearer Token 的輸入欄位設定。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bankApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank API")
                        .version("v1")
                        .description("練習用銀行後端 API：JWT 驗證、帳戶所有權、安全轉帳與交易紀錄。"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
