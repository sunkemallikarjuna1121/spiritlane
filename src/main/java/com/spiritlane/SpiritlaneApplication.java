package com.spiritlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpiritlaneApplication {

	private static final Logger logger =
            LoggerFactory.getLogger(SpiritlaneApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(SpiritlaneApplication.class, args);
	}

}
