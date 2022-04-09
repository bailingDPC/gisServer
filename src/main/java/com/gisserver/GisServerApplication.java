package com.gisserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @author bailing
 */
@SpringBootApplication
@ServletComponentScan
public class GisServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GisServerApplication.class, args);
	}

}
