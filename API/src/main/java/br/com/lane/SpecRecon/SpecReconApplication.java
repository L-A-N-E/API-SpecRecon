package br.com.lane.SpecRecon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpecReconApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpecReconApplication.class, args);
	}

}
