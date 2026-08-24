package be.agence_interim;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@NullMarked
@SpringBootApplication
@EnableJpaAuditing // Enable JPA Auditing for automatic timestamping
@EnableAsync // Enable asynchronous processing
@EnableScheduling // Politique de conservation (RetentionJob), si elle est activée
public class AgenceInterimApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenceInterimApplication.class, args);
    }

}
