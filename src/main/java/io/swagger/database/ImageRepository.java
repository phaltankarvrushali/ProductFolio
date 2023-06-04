package io.swagger.database;

import io.swagger.database.entities.Image;
import io.swagger.database.entities.Product;
import io.swagger.database.entities.User;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ImageRepository {
    @Autowired
    Session currentSession;

    @Transactional
    public Image get(String id) {

        Image obj = currentSession.get(Image.class, Long.valueOf(id));
        return obj;
    }

    @Transactional
    public List<Image> getByProductId(String productId) {

        List<Image> imageList = new ArrayList<>();

        try {
            // Session currentSession = new CustomSessionFactory().getCurrentSession();
            Transaction tx = currentSession.beginTransaction();
            SQLQuery query = currentSession.createSQLQuery("SELECT imageId FROM image where productId='"+ productId +"'");
            List result = query.list();

            if(!CollectionUtils.isEmpty(result))
            {
                for(Object obj : result)
                {
                    String id = obj.toString();
                    Image image = get(id);
                    imageList.add(image);
                }
            }
        }
        catch (Exception e){}

        return imageList;
    }

    @Transactional
    public void save(Image image) {
        Transaction t = currentSession.beginTransaction();
        currentSession.saveOrUpdate(image);
        currentSession.flush();
        t.commit();

    }

    @Transactional
    public void delete(Image image)
    {
        Transaction t = currentSession.beginTransaction();
        currentSession.delete(image);
        currentSession.flush();
        t.commit();

    }
}
