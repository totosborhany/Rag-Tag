package dev.totos.rag_hub;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableAsync
public class RagHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagHubApplication.class, args);
	}

	@PostConstruct
	public void init() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}
}
