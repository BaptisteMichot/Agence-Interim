package be.agence_interim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing // Enable JPA Auditing for automatic timestamping
@EnableAsync // Enable asynchronous processing
public class AgenceInterimApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenceInterimApplication.class, args);
    }

}
