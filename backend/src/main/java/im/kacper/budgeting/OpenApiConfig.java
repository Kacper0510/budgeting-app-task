package im.kacper.budgeting;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Budgeting API",
        version = "1.0.0",
        description = "API for managing personal finances, including accounts, transactions, and financial summaries."
    )
)
public class OpenApiConfig {}
