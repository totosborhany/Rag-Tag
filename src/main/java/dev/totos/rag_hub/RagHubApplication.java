package dev.totos.rag_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RagHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagHubApplication.class, args);
	}

}
