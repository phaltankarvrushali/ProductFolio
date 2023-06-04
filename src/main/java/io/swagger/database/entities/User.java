package io.swagger.database.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "user")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false)
    private String first_name = null;

    @Column(nullable = false)
    private String last_name = null;

    @Column(nullable = false)
    private String password = null;

    @Column(nullable = false)
    private String username = null;

    @Column(nullable = false)
    private String account_created = null;

    @Column(nullable = false)
    private String account_updated = null;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return first_name;
    }

    public void setFirstName(String firstName) {
        this.first_name = firstName;
    }

    public String getLastName() {
        return last_name;
    }

    public void setLastName(String lastName) {
        this.last_name = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountCreated() {
        return account_created;
    }

    public void setAccountCreated(String accountCreated) {
        this.account_created = accountCreated;
    }

    public String getAccountUpdated() {
        return account_updated;
    }

    public void setAccountUpdated(String accountUpdated) {
        this.account_updated = accountUpdated;
    }
}
