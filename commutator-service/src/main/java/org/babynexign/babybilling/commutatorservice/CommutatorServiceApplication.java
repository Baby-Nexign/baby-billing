package org.babynexign.babybilling.commutatorservice;

import org.babynexign.babybilling.commutatorservice.service.CallService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommutatorServiceApplication {

	public static void main(String[] args) {
		var ctx = SpringApplication.run(CommutatorServiceApplication.class, args);
		CallService callService = ctx.getBean(CallService.class);
		callService.generateCDRecords();
	}

}
