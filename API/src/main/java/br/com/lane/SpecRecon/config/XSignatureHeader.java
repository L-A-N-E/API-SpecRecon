package br.com.lane.SpecRecon.config;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
        name = "X-Signature",
        in = ParameterIn.HEADER,
        description = "Assinatura HMAC-SHA256 do payload. Obrigatório em POST/PUT/PATCH sensíveis.",
        required = true,
        schema = @Schema(type = "string", example = "Base64EncodedHMAC==")
)
public @interface XSignatureHeader {}