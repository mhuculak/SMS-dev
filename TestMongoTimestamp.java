import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

import java.util.*;

class TestMongoTimestamp {
    public static void main(String args[]) {
	MongoClient client;
	DB db;
	DBCollection collection = null;
	try {
	    client = new MongoClient("localhost", 27017);
	    db = client.getDB("test");
	    collection = db.getCollection("timestamptest");
	}
	catch (MongoException e) {
	     e.printStackTrace();
        }
	
	//	Date date = new GregorianCalendar();
	Date date = new Date();
	BasicDBObject document = new BasicDBObject();
	document.append("key", "mykey");
	document.append("date", date);
	document.append("tstamp", date.getTime());
	collection.insert(document);

	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("key", "mykey");
	DBCursor cursor = collection.find(searchQuery);
	while (cursor.hasNext()) {
	    DBObject doc = cursor.next();
	    Object dobj = doc.get("date");
	    String dstring = dobj.toString();
	    System.out.println("found " + dstring);
	    String tstmp = doc.get("tstamp").toString();
	    Long tmsec = Long.parseLong(tstmp);
	    Date fetched_date = new Date(tmsec);
	    System.out.println("fetched from timestamp " + fetched_date.toString());
	}
    }
}
