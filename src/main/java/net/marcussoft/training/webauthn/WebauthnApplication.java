package net.marcussoft.training.webauthn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "net.marcussoft.training.webauthn")
public class WebauthnApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebauthnApplication.class, args);
	}

}
