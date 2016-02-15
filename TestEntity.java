import java.util.*;

public class TestEntity {
    private String m_companyid;
    private String m_entityid;
    private String m_name;
    private String m_type;
    private String m_parent;
    private String m_status;
    private List<String> m_keywords;
    private MongoInterface m_mongo;

    public TestEntity(String companyid, String parent, String name, String type) {
	m_entityid = TestUtils.AddEntity(companyid, parent, type, name);
	m_parent = parent;
	m_type = type;
	m_name = name;
	m_companyid = companyid;
	m_mongo = MongoInterface.getInstance("unit");
    }

    public void setStatusAvailable() {
	m_status = "available";
	TestUtils.setStatus(m_companyid, m_entityid, m_status);
    }

    public void setStatusOffline() {
	m_status = "offline";
	TestUtils.setStatus(m_companyid, m_entityid, m_status);
    }

    public void setKeywords(List<String> keywords) {
	m_keywords = keywords;
	TestUtils.AddKeywords(m_entityid, keywords);
    }
    
    public String GetMessageRoutedTo(String customer) {
	List<SMSmessage> messages = m_mongo.getMessages(m_companyid, m_entityid, customer);
	if (messages.size() == 0) {
	    return null;
	}
	SMSmessage last = messages.get(messages.size()-1);
	return last.getContent();
    }

    public String getID() {
	return m_entityid;
    }
    
    public String getName() {
	return m_name;
    }
}
