package io.swagger.database;

import io.swagger.configuration.PropertiesUtil;
import org.springframework.beans.factory.annotation.Value;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseConnection {

    public java.sql.Connection con = null;
    public java.sql.Connection getConnection(){
        if (con!=null) {
            return con;
        }
        else {
            connect();
            return con;
        }
    }
    public void connect() {
        try {
            Map<String, String> dbProperties = PropertiesUtil.getDBProperties();
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(
                    dbProperties.get("spring.datasource.url"),
                    dbProperties.get("spring.datasource.username"),
                    dbProperties.get("spring.datasource.password")
            );
        } catch (SQLException e) {
            System.out.println("Unable to connect to MySql: " + e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean closeConnection(java.sql.Connection con) {
        boolean isClosed = false;
        try {
            con.close();
            isClosed = true;
            return isClosed;
        } catch (SQLException e) {
            return isClosed;
        }
    }

}
