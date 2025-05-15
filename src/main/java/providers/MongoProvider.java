package providers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.lang.reflect.Method;

public class MongoProvider implements DataProvider {

    private MongoClient client;
    private MongoDatabase database;

    @Override
    public void createConnection(String uri) {
        client = MongoClients.create(uri);
        database = client.getDatabase("m06-uf4-p2yPVP");
    }

    @Override
    public <T> T find(Class<T> entityType, Object id) {
        try {
            String className = entityType.getName();
            DataEntity entity = ENTITY_MAPPING.get(className);
            if (entity == null) throw new RuntimeException("Entity not found: " + className);

            MongoCollection<Document> collection = database.getCollection(entity.getTableOrCollection());
            Document doc = collection.find(new Document(entity.getIdColumn(), id)).first();
            if (doc == null) return null;

            T instance = entityType.getDeclaredConstructor().newInstance();
            Method idSetter = entityType.getMethod("set" + capitalize(entity.getIdProperty()), id.getClass());
            idSetter.invoke(instance, id);

            String[] attrs = entity.getAttributes();
            for (String attr : attrs) {
                String col = attr;
                Object value = doc.get(col);
                Method setter = entityType.getMethod("set" + capitalize(attr), value.getClass());
                setter.invoke(instance, value);
            }

            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving entity", e);
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
