package org.babynexign.babybilling.commutatorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CommutatorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommutatorServiceApplication.class, args);
	}

}
