package io.swagger;

import io.swagger.configuration.Config;
import io.swagger.configuration.PersistentConfig;
import io.swagger.database.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = {"io.swagger.api"})
@EnableSwagger2
@EnableConfigurationProperties
@PropertySources({
        @PropertySource("classpath:application.properties"),
        @PropertySource("classpath:log4j.properties")
})
//@PropertySource(value = "classpath:application.properties,log4j.properties")
@EnableAutoConfiguration
@Import({Config.class, PersistentConfig.class})
public class Swagger2SpringBoot implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Swagger2SpringBoot.class);
    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    public static void main(String[] args) throws Exception {
        /*ApplicationContext context
                = new AnnotationConfigApplicationContext(
                Config.class);*/
        new SpringApplication(Swagger2SpringBoot.class).run(args);
        logger.info("Application Started");
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }
}
