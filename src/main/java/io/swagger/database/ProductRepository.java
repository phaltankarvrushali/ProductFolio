package io.swagger.database;

import io.swagger.database.entities.Product;
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
public class ProductRepository
{
    @Autowired
    Session currentSession;

    @Transactional
    public Product get(Long productId)
    {

        Product obj = currentSession.get(Product.class, productId);
        return obj;
    }

    @Transactional
    public void save(Product product) {
        Transaction t = currentSession.beginTransaction();
        currentSession.saveOrUpdate(product);
        currentSession.flush();
        t.commit();

    }

    @Transactional
    public void delete(Product product)
    {
        Transaction t = currentSession.beginTransaction();
        currentSession.delete(product);
        currentSession.flush();
        t.commit();

    }

    @Transactional
    public String getProductWithsku(String sku){

        String product = null;
        try {
            // Session currentSession = new CustomSessionFactory().getCurrentSession();
            Transaction tx = currentSession.beginTransaction();
            SQLQuery query = currentSession.createSQLQuery("SELECT id FROM product where sku='"+ sku +"'");
            List result = query.list();
            if(!CollectionUtils.isEmpty(result))
            {
                product = result.get(0).toString();
            }
        }
        catch (Exception e){}

        return product;
    }
}
