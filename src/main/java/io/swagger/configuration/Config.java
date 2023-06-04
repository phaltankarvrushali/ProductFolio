package io.swagger.configuration;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.swagger.database.ImageRepository;
import io.swagger.database.ProductRepository;
import io.swagger.database.UserRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Configuration
public class Config
{
    @Value("${aws.region}")
    private String appName;

    @Value("${spring.datasource.url}")
    private String dbURL;

    @Value("${spring.datasource.username}")
    private String userName;

    @Value("${spring.datasource.password}")
    private String password;
   @Bean
    public AmazonS3 amazonS3() {
       Map<String, String> s3Properties = PropertiesUtil.getS3Properties();
       String awsRegion = s3Properties.get("aws.region");
       //String awsRegion = PropertiesUtil.getS3Properties().get("aws.region");
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(appName))
                .build();
    }

    @Bean
    public UserRepository userRepository()
    {
        return new UserRepository();
    }

    @Bean
    public ProductRepository productRepository(){
        return new ProductRepository();
    }

    @Bean
    public ImageRepository imageRepository()
    {
        return new ImageRepository();
    }
    @Bean
    public Session getCurrentSession() {

        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        Properties dbProperties = new Properties();
        dbProperties.put("hibernate.dialect","org.hibernate.dialect.MySQL5InnoDBDialect");
        dbProperties.put("hibernate.connection.url",dbURL);
        dbProperties.put("hibernate.connection.username",userName);
        dbProperties.put("hibernate.connection.password",password);
        dbProperties.put("hibernate.connection.driver_class","com.mysql.cj.jdbc.Driver");
        dbProperties.put("hibernate.connection.pool_size",3);
        dbProperties.put("hibernate.current_session_context_class","thread");
        dbProperties.put("hibernate.show_sql",false);
        dbProperties.put("hibernate.format_sql",true);
        //configuration.addAnnotatedClass(io.swagger.database.entities.User.class);
        //configuration.addAnnotatedClass(io.swagger.database.entities.Product.class);
        //configuration.addAnnotatedClass(io.swagger.database.entities.Image.class);
        configuration.setProperties(dbProperties);

        StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties())
                .configure("hibernate.cfg.xml").build();

        Metadata meta = new MetadataSources(ssr).getMetadataBuilder().build();

        SessionFactory factory = meta.getSessionFactoryBuilder().build();
        Session session = factory.openSession();

        return session;
    }

}
