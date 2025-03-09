package newOs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;



@SpringBootApplication
public class OneOsApplication{

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(OneOsApplication.class, args);



	}
}

