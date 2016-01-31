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

public class MongoInterface {
    private static MongoInterface m_instance = null;
    private static MongoClient m_client = null;
    private static DB m_db = null;
    private static DBCollection m_companies = null;
    private static DBCollection m_counter = null;
    private static DBCollection m_entities = null;
    private static DBCollection m_messages = null;
    private static DBCollection m_customers = null;
    
    private MongoInterface() {
	try {
	    m_client = new MongoClient("localhost", 27017);
	    m_db = m_client.getDB("demo");                    
	    m_companies = m_db.getCollection("companies");
	    m_counter = m_db.getCollection("counters");
	    m_entities = m_db.getCollection("entities"); // business reps
	    m_messages = m_db.getCollection("messages");
	    m_customers = m_db.getCollection("customers");
	}
	catch (MongoException e) {
	     e.printStackTrace();
        }   
    }
    
    private static Object getNextID() {
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
    
    public static MongoInterface getInstance() {
	if (m_instance == null) {
	    m_instance = new MongoInterface(); 
	}
	return m_instance;
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

    public String getCustomerMessage(String phone) {
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
	    return CustomerStatus.valueOf(document.get("status"));
	}
	return null;
    }

    public String getCustomerRoute(String customerid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(customerid));
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("entityid");
	}
	return null;	
    }
    
    public Boolean RouteCustomerToEntity(String entityid, String phone) {
        BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("phone", phone);
	DBCursor cursor = m_customers.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("entityid", entityid);
	    updateQuery.append( "$set", newFields);
	    m_customers.update(searchQuery, updateQuery);
	    return true; // sucess
	}
	else if(cursor.count() == 0) {
	    return false; // failure
	}
	else {
	    System.out.println("ERROR: customer phone number " + phone + " matches multiple entries");
	}
	return false;
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
	    return document.get("focus").toString();
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
	return null;
    }

    public String getEntityName(String entityid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("Name").toString();
	}
	else {
	    System.out.println("ERROR: counld not find unique entity for " + entityid);
	}
	return null;	
    }
    
    public String AddEntity(String companyid, String parent, String name, String email, String type) {
	BasicDBObject document = new BasicDBObject();
	Object id = getNextID();
	document.append("_id", id);
	document.append("companyid", companyid);
	document.append("parent", parent);
	document.append("Name", name);
	document.append("email", email);
	document.append("type", type);
	document.append("state", "offline");
	m_entities.insert(document);
	return id.toString();
    }

    public void SetEntityState(String companyid, String entityid, String state) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("_id", Integer.parseInt(entityid));
	DBCursor cursor = m_entities.find(searchQuery);
	if (cursor.count() == 1) {
	    BasicDBObject updateQuery =  new BasicDBObject();
	    BasicDBObject newFields = new BasicDBObject();
	    newFields.append("state", state);
	    updateQuery.append( "$set", newFields);
	    m_entities.update(searchQuery, updateQuery);
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
    
    public String AddCompany(String name, String email, String phone) {
	BasicDBObject document = new BasicDBObject();
	Object id = getNextID();
	document.append("_id", id);
	document.append("Name", name);
	document.append("email", email);
	document.append("Phone", phone);
	m_companies.insert(document);
	return id.toString();
    }
    
    public void ShowEntities(String companyid, String parent, PrintWriter out) {
	out.println("<h1>Children of Parent " + parent + " :</h1>");
	out.println("<table cellspacing=\"30\">");
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("parent", parent);
	DBCursor cursor = m_entities.find(searchQuery);
	while (cursor.hasNext()) {
	    out.println("<br><tr>");
	    DBObject document = cursor.next();
	    out.println("<td>" + document.get("Name").toString() + "</td>");
	    String type = document.get("type").toString();
	    out.println("<td>" + type + "</td>");
            if (type.equals("employee")) {
		out.println("<td>" + document.get("email").toString() + "</td>");
	    }
	    out.println("</tr>");
	}
	out.println("</table>");
    }

    private String doFindCompany(String phone) {
	BasicDBObject searchQuery = new BasicDBObject();       
	searchQuery.put("Phone", phone);
	DBCursor cursor = m_companies.find(searchQuery);
	if (cursor.count() == 1) {
	    DBObject document = cursor.next();
	    return document.get("_id").toString();
	}
	else if (cursor.count() > 1) {
	    return "multiple";
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
	    return document.get("Name").toString();
	}
	return null;
    }
   
    public List<String> FindAvailableEntities(String companyid) {
	BasicDBObject searchQuery = new BasicDBObject();
	searchQuery.put("companyid", companyid);
	searchQuery.put("state", "available");
	DBCursor cursor = m_entities.find(searchQuery);
	List<String> available = new ArrayList<String>();
	while(cursor.hasNext()) {
	    DBObject document = cursor.next();
	    String entityid = document.get("_id").toString();
	    available.add(entityid);
	}
	return available;
    }

    public void ShowCompanies(PrintWriter out) {
	out.println("<h1>Company List:</h1>");
	out.println("<table cellspacing=\"30\">");
	out.println("<br><tr><td><b>Company Name</b></td><td><b>email address</b></td><td><b>Phone Number</b></td><td><b>ID</b></td><td><b>Delete It</b></td></tr>");
	DBCursor cursor = m_companies.find();
	while (cursor.hasNext()) {
	    DBObject document = cursor.next();
	    out.println("<br><tr>");
	    out.println("<td>" + document.get("Name").toString() + "</td>");
	    out.println("<td>" + document.get("email").toString() + "</td>");
	    out.println("<td>" + document.get("Phone").toString() + "</td>");
	    out.println("<td>" + document.get("_id").toString() + "</td>");
	    out.println("<td><form action=\"SuperAdmin\" method=\"GET\"><input type=\"submit\" name=\"delete\" value=\"" + document.get("_id").toString() + "\"/></form></td>");
	    out.println("</tr><br>");
	}
	out.println("</table>");

    }
    public void RemoveEntity(String id) {
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
	document.append("time",message.getTime());
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
	String time = document.get("time").toString();
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
	return messages;
    }
}
