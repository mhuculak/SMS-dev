import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

public class MongoInterface {
    
    private static Map<String, MongoInterface> m_instance = null; // static map allows us to share db interfaces i.e. 1 connect per process per db
    private MongoClient m_client = null;
    private DB m_db = null;
    private DBCollection m_companies = null;
    private DBCollection m_counter = null;
    private DBCollection m_entities = null;
    private DBCollection m_messages = null;
    private DBCollection m_customers = null;
    private DBCollection m_advertisments = null;
    private DBCollection m_waiting = null;
    private DBCollection m_actives = null;
    
    private MongoInterface(String db) { // private to share connections via getIntance()
	try {
	    m_client = new MongoClient("localhost", 27017);
	    m_db = m_client.getDB(db);                    
	    m_companies = m_db.getCollection("companies");
	    m_counter = m_db.getCollection("counters");
	    m_entities = m_db.getCollection("entities"); // business reps
	    m_messages = m_db.getCollection("messages");
	    m_customers = m_db.getCollection("customers");
	    m_advertisments = m_db.getCollection("advertisments");
	    m_waiting = m_db.getCollection("waiting");
	    m_actives = m_db.getCollection("actives"); // used to share phone numbers (should only be for demo purposes)
	}
	catch (MongoException e) {
	     e.printStackTrace();
        }   
    }
    
    private Object getNextID() {
	if (m_counter.count() == 0) {
	    BasicDBObject document = new BasicDBObject();
	    document.append("_id", "entries");
            document.append("seq", 0);
            m_counter.insert(document);
	}
        BasicDBObject searchQuery = new BasicDBObject("_id", "entries");
        BasicDBObject increase = new BasicDBObject("seq", 1);
        BasicDBObject updateQuery = new BasicDBObject("$inc", increase);
        DBObject result = m_counter.findAndModify(searchQuery, null, null,
            false, updateQuery, true, false);

        return result.get("seq");
    }    
    
    public static MongoInterface getInstance(String db) {
	if (db == null) {
	    db = "demo";
	}
	if (m_instance == null) {
	    m_instance = new HashMap<String, MongoInterface>();
	}	
	if (m_instance.get(db) == null) {
	    System.out.println("create Mongo DB connection to db = " + db);
	    MongoInterface db_if = new MongoInterface(db);
	    m_instance.put(db, db_if);
	}
	return m_instance.get(db);
    }

    public void addCustomerToWaitingQueue(String companyid, String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("customerid", customerid );
	DBCursor cursor = m_waiting.find(searchQuery);
	if (cursor.count() == 0) { // only care if the customer is not there
	    BasicDBObject document = new BasicDBObject();
	    Object id = getNextID();
	    document.append("_id", id);
	    document.append("customerid", customerid);
	    document.append("companyid", companyid);
	    m_waiting.insert(document);
	}	
    }

    public String removeCustomerFromWaitingQueue(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject("_id", "entries");
	DBCursor cursor = m_counter.find(searchQuery);
	long min;
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    min = Long.parseLong(document.get("seq").toString()); // min = current value of the id counter i.e. max value of any id
	    System.out.println("min value initialized to " + min);
	}
	else {
	    System.out.println("ERROR: could not find the DB counter");
	    return null;
	}
	searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid );	
	cursor = m_waiting.find(searchQuery);
	Object minobj = null;
	if (cursor.count() == 0) {
	    return null;  // empty so nothing to do
	}
	String customerid = null;
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    Object idobj = document.get("_id");
	    System.out.println("Found id = " + idobj.toString());
	    long id = Long.parseLong(idobj.toString());	    
	    if (id <= min) {
		min = id;
		minobj = idobj;
		customerid = document.get("customerid").toString();
	    }
	}
	searchQuery = new BasicDBObject();
	searchQuery.put("_id", minobj);
	m_waiting.remove(searchQuery);
	return customerid;
    }
    
    public String addNewAdvertisment(String companyid, String name, String content) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid );
	searchQuery.put("name", name);
	DBCursor cursor = m_advertisments.find(searchQuery);
	if (cursor.count() == 0) {
	    BasicDBObject document = new BasicDBObject();
	    Object id = getNextID();
	    document.append("_id", id);
	    document.append("companyid", companyid);
	    document.append("name", name);
	    document.append("content", content);
	    m_advertisments.insert(document);
	    return id.toString();
	}
	else if (cursor.count() == 1) {
	    System.out.println("ERROR: advertisment with companyid = " + companyid + " and name = " + name + " already exists");
	    return null;
	}
	return null;
    }
    
    public String AddCustomer(String phone) { // customer is identified by a phone number
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("phone", phone);
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 0) {
	    BasicDBObject document = new BasicDBObject();
	    Object id = getNextID();
	    document.append("_id", id);
	    document.append("phone", phone);
	    m_customers.insert(document);
	    return id.toString();
	}
	else if(cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("_id").toString();
	}
	else {
	    System.out.println("ERROR: customer phone number " + phone + " matches multiple entries");
	}
	return null;
    }

    public void setEmployeeMessage(String entityid, String messageid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("messageid", messageid);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
    }

    public String getEmployeeMessage(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    Object messageobj = document.get("messageid");
	    if (messageobj != null) {
		return messageobj.toString();
	    }
	}
	return null;
    }

    public void setCustomerMessage(String customerid, String messageid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("messageid", messageid);
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	}
	else {
	    System.out.println("ERROR: counld not find unique customer for " + customerid);
	}
    }

    public void setCustomerConfirmRoute(String customerid, String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("confirm", entityid);
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	}
	else {
	    System.out.println("ERROR: counld not find unique customer for " + customerid);	
	}
    }

    public String getCustomerConfirmRoute(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	   DBObject document = cursor.next();
	   Object confirmobj = document.get("confirm");
	   if (confirmobj != null) {
	       return confirmobj.toString();
	   }
	   System.out.println("ERROR: confirm not found for customer " + customerid);
	}
	return null;
    }
    
    public String getCustomerMessageFromPhone(String phone) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("phone", phone);
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	   DBObject document = cursor.next();
	   return document.get("messageid").toString();
	}
	else {
	    System.out.println("ERROR: customer phone number " + phone + " matches multiple entries");
	}
	return null;
    }
    
    public String getCustomerMessageFromId(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid) );
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	   DBObject document = cursor.next();
	   Object messobj = document.get("messageid");
	   if (messobj != null) {
	       return messobj.toString();
	   }
	   System.out.println("ERROR: messageid not found for customer " + customerid);
	}
	else {
	    System.out.println("ERROR: customer phone number " + customerid + " matches multiple entries");
	}
	return null;
    }

    public void setCustomerStatus(String customerid, CustomerStatus status) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("status", status.toString());
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	}
	else {
	    System.out.println("ERROR: counld not find unique customer for " + customerid);	
	}
    }

    public CustomerStatus getCustomerStatus(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    Object status = document.get("status");
	    if (status != null) {
		return CustomerStatus.valueOf(status.toString());
	    }
	}
	return null;
    }

    public String getCustomerRoute(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("entityid").toString();
	}
	return null;	
    }
    
    public Boolean RouteCustomerToEntity(String entityid, String customerid) {	
        BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	String messageid = getCustomerMessageFromId(customerid);
	if (messageid == null) {
	    return false;
	}
	Boolean result = setMessageEntityID(messageid, entityid);
	if (result == false) {
	    System.out.println("ERROR: failed to set entityid = " + entityid + " in message " + messageid);
	    return false;
	}
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    System.out.println("Updating customerid " + customerid + " with entityid = " + entityid);
	    newFields.append("entityid", entityid);
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	    return true; // sucess
	}
	else if(cursor.count() == 0) {
	    System.out.println("ERROR: customerid " + customerid + " did not match");
	    return false; // failure
	}
	else {
	    System.out.println("ERROR: customerid " + customerid + " matches multiple entries");
	}
	return false;
    }

    public void removeCustomerRoute(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.put("entityid", "");
	    newFields.put("status", CustomerStatus.UNKNOWN.toString());
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	}	    
    }
    
    public void removeCustomerRoutes(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("entityid", entityid);
	BasicDBObject newFields=  new BasicDBObject();
	newFields.put("entityid", "");
	newFields.put("status", CustomerStatus.UNKNOWN.toString());
	BasicDBObject updateQuery =  new BasicDBObject();
	updateQuery.append( "$set", newFields);
	m_customers.updateMulti(searchQuery, updateQuery);	
    }
    
    public List<String> FindRoutedCustomers(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("entityid", entityid);
	DBCursor cursor = m_customers.find(searchQuery);
	List<String> customers = new ArrayList<String>();
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String phone = document.get("phone").toString();
	    customers.add(phone);
	}
	return customers;
    }
    
    public void addKeyword(String entityid, String keyword) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("keywords", keyword);
	    updateQuery.append( "$push", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
    }

    public BasicDBList getKeywords(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    BasicDBList keywords = (BasicDBList)document.get("keywords");
	    return keywords;
	}
	return null;
    }

    public Map<String, BasicDBList> getAvailableKeywords(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("state", "available");
	DBCursor cursor = m_entities.find(searchQuery);
	Map<String, BasicDBList> available_keywords = new HashMap<String, BasicDBList> ();
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String entityid = document.get("_id").toString();
	    BasicDBList keywords = (BasicDBList)document.get("keywords");
	    if (keywords != null) {
		available_keywords.put(entityid, keywords);
	    }
	}
	return available_keywords;
    }
    
    public void setEntityFocus(String entityid, String customer) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("focus", customer);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
    }
     
    public String getEntityFocus(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    Object focus = document.get("focus");
	    if (focus != null) {
		return focus.toString();
	    }
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
	return null;
    }

    public String getEntityName(String entityid) {
	if (entityid.equals("0")) {
	    return null;
	}
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("name").toString();
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
	return null;	
    }

    public String getEntityType(String entityid) {
	if (entityid.equals("0")) {
	    return null;
	}
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("type").toString();
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
	return null;
    }

    public void setEntityName(String entityid, String name) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("name", name);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
	
    }

    public void setEntityEmail(String entityid, String email) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("email", email);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
	
    }
    
    
    public String AddEntity(String companyid, String parent, String name, String email, String type) {
	BasicDBObject document = new BasicDBObject();
	Object id = getNextID();
	if (parent == null) {
	    parent = "0";
	}
	document.append("_id", id);
	document.append("companyid", companyid);
	document.append("parent", parent);
	document.append("name", name);
	document.append("email", email);
	document.append("type", type);
	document.append("state", "offline");
	m_entities.insert(document);
	return id.toString();
    }
    
    public void PropogateToParent(String companyid, String parent, String propagate_state) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("parent", parent);
	DBCursor cursor = m_entities.find(searchQuery);
	int count = 0;
	int num_children = cursor.count();
	//	System.out.println("entity " + parent + " has " + num_children + " children");
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    Object stateobj = document.get("state");	    
	    String state = "offline"; // default if does not exist yet
	    if (stateobj != null) {
		state = stateobj.toString();
	    }
	    if (state.equals(propagate_state)) {
		count++;
	    }
	}
	/*
           In order to propagate an offline state to a parent, all children must be offline
         */
	if (propagate_state.equals("offline") && count == num_children) {
	    System.out.println("set state " + propagate_state + " in parent " + parent);
	    SetEntityState(companyid, parent, propagate_state);
	}
	else if (propagate_state.equals("available")) {
	    System.out.println("set state " + propagate_state + " in parent " + parent);
	    SetEntityState(companyid, parent, propagate_state);
	}
    }

    /*
         entity state recursively propagates to parents
     */
    public void SetEntityState(String companyid, String entityid, String state) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    String parent = document.get("parent").toString();
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("state", state);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	    if (parent.equals("0")) {
		System.out.println("No need to propagate state to the top level");	
	    }
	    else {
		System.out.println("propagate state " + state + " from " + entityid + " to parent " + parent);
		PropogateToParent(companyid, parent, state);
	    }
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for companyid=" + companyid + " entityid=" + entityid);
	}
	    
    }
    //    public String GetEntityState(String companyid, String entityid, PrintWriter out) {
    public String GetEntityState(String companyid, String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("_id", Integer.parseInt(entityid));
	//	out.println("<h2>GetEntityState: query = " + searchQuery.toString() + "</h2>");
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    //  out.println("<h2>GetEntityState: query returned " + document.toString() + "</h2>");
	    return document.get("state").toString();
	}
	else {
	    //	    out.println("<h2>GetEntityState: search returned " + cursor.count() + " results</h2>");
	}
	return null;
    }

    public void setEntityTimestamp(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	Date now = new Date();	
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("time", now.getTime());
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
	}
    }

    public Long getEntityTimestamp(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    Object tobj = document.get("time");
	    if (tobj != null) {
		return Long.parseLong(tobj.toString());
	    }
	}
	return 0L;
    }
    public String AddCompany(String name, String email, String phone, String account_sid, String auth_token) {
	BasicDBObject document = new BasicDBObject();
	Object id = getNextID();
	document.append("_id", id);
	document.append("name", name);
	document.append("email", email);
	document.append("phone", phone);
	document.append("account_sid", account_sid);
	document.append("auth_token", auth_token);
	m_companies.insert(document);
	return id.toString();
    }

    public String getAccountSid(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(companyid));
	DBCursor cursor = m_companies.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("account_sid").toString();
	}
	return null;
    }

    public String getAuthToken(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(companyid));
	DBCursor cursor = m_companies.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("auth_token").toString();
	}
	return null;
    }

    public Entity getEntity(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return  getEntity(document);
	}
	return null;
    }
    
    public Entity getEntity(DBObject document) {
	String entityid = document.get("_id").toString();
	String type = document.get("type").toString();
	String name = document.get("name").toString();
	String email = document.get("email").toString();
	String parent = document.get("parent").toString();
	String companyid = document.get("companyid").toString();
	Object stateobj = document.get("state");
	String state = "offline"; // default if not defined
	if (stateobj != null) {
	    state = stateobj.toString();
	}
	Entity entity = new Entity(entityid, parent, name, companyid, type, email, state);
	return entity;
    }
    
    public List<Entity> GetEntities(String companyid, String parent) {
	BasicDBObject searchQuery = new BasicDBObject();
	if (parent == null) {
	    parent = "0";
	}
	searchQuery.put("companyid", companyid);
	searchQuery.put("parent", parent);
	DBCursor cursor = m_entities.find(searchQuery);
	List<Entity> entities = new ArrayList<Entity>();
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    Entity entity = getEntity(document);
	    entities.add(entity);
	}
	return entities;
    }

    private String doFindCompany(String phone) {
	BasicDBObject searchQuery = new BasicDBObject();       
	searchQuery.put("phone", phone);
	DBCursor cursor = m_actives.find(searchQuery);
	/*
           First check for the case where companies are sharing the phone number
         */
	if (cursor.count() == 1) {
	    System.out.println("Found entry with " + phone + " in actives collection");
	    DBObject document = cursor.next();
	    return document.get("active").toString();
	}
	else if (cursor.count() == 0) {
	    System.out.println(phone + " not found in actives collection");
	    cursor = m_companies.find(searchQuery);
	    if (cursor.count() == 1) {
		DBObject document = cursor.next();
		return document.get("_id").toString();
	    }
	    else if (cursor.count() > 1) {
		return "multiple";
	    }
	}
	return null;
    }
    
    public String FindCompany(String phone) {
	String companyid = doFindCompany(phone);
	if (companyid == null) {
	    String massaged_phone = phone.replace("+", "");
	    companyid = doFindCompany(massaged_phone);
	}
	return companyid;
    }
    
    public String GetCompanyName(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(companyid));
	DBCursor cursor = m_companies.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("name").toString();
	}
	return null;
    }

    public Map<String, String> getPoolIdleTime(String companyid, String pool_entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("parent", pool_entityid);
	searchQuery.put("state", "available");
	DBCursor cursor = m_entities.find(searchQuery);
	Map<String, String> pool_idle = new HashMap<String, String>();
	Date now = new Date();
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String entityid = document.get("_id").toString();
	    Long avail_time = getEntityTimestamp(entityid);
	    Long idle = now.getTime() - avail_time;
	    pool_idle.put(entityid, idle.toString()); 
	}
	return pool_idle;
    }
    
    public List<String> FindAvailableEntities(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("state", "available");
	searchQuery.put("parent", "0"); // FIXME: we may want to route to entities not at the top level
	DBCursor cursor = m_entities.find(searchQuery);
	List<String> available = new ArrayList<String>();
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String entityid = document.get("_id").toString();
	    available.add(entityid);
	}
	return available;
    }

    public Company getCompany(DBObject document) {
       String name = document.get("name").toString();
       String email = document.get("email").toString();
       String phone = document.get("phone").toString();
       String id = document.get("_id").toString();
       String sid = document.get("account_sid").toString();
       String auth = document.get("auth_token").toString();
       Company company = new Company(id, name, email, phone, sid, auth);
       return company;
    }
    
    public List<Company> getCompanies() {
	DBCursor cursor = m_companies.find();
	List<Company> companies = new ArrayList<Company>();
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    Company company = getCompany(document);
	    companies.add(company);
	}
	return companies;
    }
    
    public void RemoveEntity(String id) {
	BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count()==1) {
	    m_entities.remove(searchQuery);
	    System.out.println("removed entity with id " + id);
	}
	else if (cursor.count()>1) {
	    System.out.println("ERROR: found multiple entities with id = " + id);
	}
	else if (cursor.count()==0) {
	    System.out.println("ERROR: could not find entity with id = " + id);
	}
    }

    public void ActivateCompany(String phone, String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("phone", phone);
	DBCursor cursor = m_actives.find(searchQuery);
	if (cursor.count()==1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("active", companyid);
	    updateQuery.append( "$set", newFields);
	    m_actives.update(searchQuery, updateQuery);
	}
	else if (cursor.count() == 0) {
	    BasicDBObject document = new BasicDBObject();
	    document.append("phone", phone);
	    document.append("active", companyid);
	    m_actives.insert(document);
	}
    }
    
    public void RemoveCompany(String id) {
	BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
	DBCursor cursor = m_companies.find(searchQuery);
	if (cursor.count()==1) {
	    m_companies.remove(searchQuery);
	}
	else {
	    System.out.println("ERROR: found multiple companies with id = " + id);
	}
    
    }

    public String addMessage(SMSmessage message) {
	BasicDBObject document = new BasicDBObject();
	Object id = getNextID();		
	document.append("_id", id);
	document.append("time",message.getTime().getTime());
	document.append("type",message.getType());
	document.append("content",message.getContent());
	document.append("from", message.getFrom());
	document.append("to", message.getTo());
	document.append("status", message.getStatus().toString());
	document.append("customer", message.getCustomer());
	
	if (message.getCompanyID() != null) {
	    document.append("companyid",message.getCompanyID());
	}
	if (message.getEntityID() != null) {
	    document.append("entityid",message.getEntityID());
	}
	if (message.getSid() != null) {
	    document.append("sid", message.getSid());
	}
	document.append("name",message.getName());
	m_messages.insert(document);
	return id.toString();
    }

    public void setMessageStatus(String messageid, SMSmessageStatus status) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(messageid));
	DBCursor cursor = m_messages.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("status", status.toString());
	    updateQuery.append( "$set", newFields);
	    m_messages.update(searchQuery, updateQuery);
	}
	else {
	    System.out.println("ERROR: could not find unique message with " + messageid);
	}
    }
    
    public String checkForResponse(String entityid, String messageid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("entityid", entityid);
	DBCursor cursor = m_messages.find(searchQuery);
	int mid = Integer.parseInt(messageid);
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String id = document.get("_id").toString();
	    int i =  Integer.parseInt(id);
	    if (i > mid) { // id always increases therfore the messages was responded to
		String response = document.get("content").toString();
		return response;
	    }
	}
	return null; // no response found
    }

    private SMSmessage getMessage(DBObject document) {
	String type = document.get("type").toString();
	Date time = new Date(Long.parseLong(document.get("time").toString()));
	String content = document.get("content").toString();
	String to = document.get("to").toString();
	String from = document.get("from").toString();
	String name = document.get("name").toString();
	String status = document.get("status").toString();
	String customer = document.get("customer").toString();
	Object companyobj = document.get("companyid");
	Object entityobj = document.get("entityid");
	Object sid = document.get("sid");
	String id = document.get("_id").toString();
	SMSmessage mess = null;
	if (companyobj != null && entityobj != null) {
	    mess = new SMSmessage(content, from, to, companyobj.toString(), entityobj.toString());
	}
	else {
	    mess = new SMSmessage(content, from, to);
	}
	// overide the default values from the SMSmessage ctor with values from DB
	mess.setTime(time);
	mess.setType(type);
	mess.setName(name);
	mess.setStatus(status);
	mess.setId(id);
	if (sid != null) {
	    mess.setSid(sid.toString());
	}
	mess.setCustomer(customer);
	
	return mess;
    }
    
    public SMSmessage getPreviousMessage(String customer, String messageid) {
	System.out.println("get message previous to " + messageid + " for customer " + customer);
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("customer", customer);
	DBCursor cursor = m_messages.find(searchQuery);
	TreeMap<String, DBObject> customer_messages = new TreeMap<String, DBObject>();
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String mid = document.get("_id").toString();
	    //	    System.out.println("add mid " + mid + " to TreeMap");
	    customer_messages.put( mid, document);
	}
	Object[] midarray = customer_messages.keySet().toArray();
	int i;
	String previousid = null;
	for ( i=midarray.length-1 ; i>=0 && previousid == null ; i--) {
	    String mid = midarray[i].toString();
	    //	    System.out.println("check mid " + mid);
	    if (midarray[i].toString().equals(messageid) && i>0) {
		//		System.out.println("mid " + mid + " equals " + messageid);
		previousid = midarray[i-1].toString();
		//		System.out.println("previous is is" + previousid);
	    }
	}
	if (previousid != null) {
	    DBObject document = customer_messages.get(previousid);
	    //	    System.out.println("returning previousid = " + previousid);
	    return getMessage(document);
	}
	return null;
    }
    
    public SMSmessage getMessage(String messageid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(messageid));
	DBCursor cursor = m_messages.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return getMessage(document);
	}
	else {
	    System.out.println("ERROR: could not find unique message with " + messageid);
	    return null;
	}
    }
    
    public List<SMSmessage> getMessages(String companyid, String entityid, String customer) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("entityid", entityid);
	searchQuery.put("customer", customer);
	DBCursor cursor = m_messages.find(searchQuery);
	List<SMSmessage> messages = new ArrayList<SMSmessage>();
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    SMSmessage mess = getMessage(document);
	    messages.add(mess);	    
	}
	//	System.out.println("getMessages for companyid " + companyid + " entityid " + entityid + " customer " + customer + " returned " + messages.size() + " messages");
	return messages;
    }

    private Boolean setMessageEntityID(String messageid, String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(messageid));
	DBCursor cursor = m_messages.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("entityid", entityid);
	    updateQuery.append( "$set", newFields);
	    m_messages.update(searchQuery, updateQuery);
	    return true;
	}
	else {
	    System.out.println("ERROR: could not find unique message with " + messageid);
	}
	return false;
    }
}
