import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;


/*
  DB utils used for unit testing
*/
public class MongoUtils {
    private static MongoUtils m_instance = null;
    private static MongoClient m_client = null;
    private static DB m_db = null;
    private static String m_db_name = "unit";
    
    private MongoUtils() {
	try {
	    m_client = new MongoClient("localhost", 27017);
	    m_db = m_client.getDB(m_db_name);
	}
	catch (MongoException e) {
	     e.printStackTrace();
        }   
    }

    public static MongoUtils getInstance() {
	if (m_instance == null) {
	    m_instance = new MongoUtils(); 
	}
	return m_instance;
    }

    public void wipeDB() {
	m_db.dropDatabase();
    }
    
}
