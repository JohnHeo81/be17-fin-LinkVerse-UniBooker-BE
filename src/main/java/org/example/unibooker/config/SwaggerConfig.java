package org.example.unibooker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniBooker API")
                        .description("B2B 클라우드 예약 관리 서비스 API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("LinkVerse Team")
                                .url("https://github.com/beyond-sw-camp/be17-fin-LinkVerse-UniBooker-BE")));
    }
}