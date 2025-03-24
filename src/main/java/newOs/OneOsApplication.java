package newOs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class OneOsApplication{

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(OneOsApplication.class);
		// 显式启用循环依赖支持
		application.setAllowCircularReferences(true);
		ConfigurableApplicationContext context = application.run(args);
	}
}

