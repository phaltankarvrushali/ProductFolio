package io.swagger.database;

import io.swagger.database.entities.User;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepository
{
    @Autowired
    Session currentSession;

    @Transactional
    public User get(Long id) {

        User obj = currentSession.get(User.class, id);
        return obj;
    }

    @Transactional
    public void save(User user) {
        //Session currentSession = new CustomSessionFactory().getCurrentSession();
        Transaction t = currentSession.beginTransaction();
        currentSession.saveOrUpdate(user);
        currentSession.flush();
        t.commit();

    }

    @Transactional
    public String getUserWithUsername(String username){

        String user = null;
        try {
           // Session currentSession = new CustomSessionFactory().getCurrentSession();
            Transaction tx = currentSession.beginTransaction();
            SQLQuery query = currentSession.createSQLQuery("SELECT id FROM user where username='"+ username +"'");
            List result = query.list();
            if(!CollectionUtils.isEmpty(result))
            {
                user = result.get(0).toString();
            }
        }
        catch (Exception e){}

        return user;
    }
}