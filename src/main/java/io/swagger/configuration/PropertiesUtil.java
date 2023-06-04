package io.swagger.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtil
{
    public static Map<String, String> getDBProperties()
    {
        Map<String, String> dbProperties = new HashMap<>();
        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            dbProperties.put("spring.datasource.dialect", prop.getProperty("spring.datasource.dialect"));
            dbProperties.put("spring.datasource.driver-class-name", prop.getProperty("spring.datasource.driver-class-name"));
            dbProperties.put("spring.datasource.url", prop.getProperty("spring.datasource.url"));
            dbProperties.put("spring.datasource.username", prop.getProperty("spring.datasource.username"));
            dbProperties.put("spring.datasource.password", prop.getProperty("spring.datasource.password"));


        } catch (IOException ex) {
        }
        return dbProperties;
    }

    public static Map<String, String> getS3Properties()
    {
        Map<String, String> s3Properties = new HashMap<>();
        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            s3Properties.put("s3.bucketName", prop.getProperty("s3.bucketName"));
            s3Properties.put("aws.region", prop.getProperty("aws.region"));
            s3Properties.put("aws.accessKey", prop.getProperty("aws.accessKey"));
            s3Properties.put("aws.secretKey", prop.getProperty("aws.secretKey"));

        } catch (IOException ex) {
        }
        return s3Properties;
    }
}
