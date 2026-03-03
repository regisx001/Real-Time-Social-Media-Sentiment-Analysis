package com.regisx001.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * The main application class for starting the Spring Boot application.
 */
@SpringBootApplication
public class CoreApplication {

	/**
	 * Main method to start the application.
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(CoreApplication.class, args);
	}

}
