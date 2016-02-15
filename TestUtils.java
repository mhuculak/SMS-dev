import java.util.*;
import java.io.*;
import java.net.*;

public class TestUtils {

    private static MongoUtils m_mongo_utils = null;
    private static final String m_company_url_prefix = "http://sms-dev.mooo.com:8080/SuperAdmin?db=unit&";
    private static final String m_entity_url_prefix = "http://sms-dev.mooo.com:8080/Admin?db=unit&";
    private static final String m_employee_input_url_prefix = "http://sms-dev.mooo.com:8080/EmployeeInput?db=unit&";
    private static final String m_sms_url_prefix = "http://sms-dev.mooo.com:8080/sms?db=unit";

    public static void WipeDB() { // remove all collections
	m_mongo_utils = MongoUtils.getInstance();
	m_mongo_utils.wipeDB();
    }
    
    public static String AddCompany(String name, String phone) {
	String url = null;
	try {
	    url = m_company_url_prefix + "name=" + URLEncoder.encode(name,"UTF-8") + "&phone=" + phone + "&sid=&token=&email=";
	}
	catch (IOException ie) {
	    ie.printStackTrace();
	}
	System.out.println(url);
	HttpClient client = new HttpClient(HttpMethod.GET, url);
	int status = client.connect();
	if (status == 200) {
	    return client.getBody(); // Note: in unit test mode the companyid should be returned in the body
	}
	return null;
    }
    
    public static String DeleteCompany(String companyid) {
	return null;
    }

    public static String AddEntity(String companyid, String parent, String type, String name) {
	String url = null;
	try {
	    if (parent == null) {
		url = m_entity_url_prefix + "companyid=" + companyid + "&action=add&type=" + type +
		    "&name=" + URLEncoder.encode(name,"UTF-8") + "&email=";
	    }
	    else {
		url = m_entity_url_prefix + "companyid=" + companyid + "&parent=" + parent + "&action=add&type=" + type +
		    "&name=" + URLEncoder.encode(name,"UTF-8") + "&email=";
	    }
	}
	catch (IOException ie) {
	    ie.printStackTrace();
	}
	System.out.println(url);
	HttpClient client = new HttpClient(HttpMethod.GET, url);
	int status = client.connect();
	if (status == 200) {
	    return client.getBody(); // Note: in unit test mode the companyid should be returned in the body
	}
	return null;
    }

    public static String AddKeywords(String entityid, List<String> keywords) {
	return null;
    }

    public static void setStatus(String companyid, String entityid, String status) {
	String url = m_employee_input_url_prefix + "companyid=" + companyid + "&entityid=" + entityid + "&status=" + status;;
	System.out.println(url);
	HttpClient client = new HttpClient(HttpMethod.GET, url);
	int http_status = client.connect();
    }

    public static String sendMessage(String from, String to, String content) {
	String url = m_sms_url_prefix;
	HttpClient client = new HttpClient(HttpMethod.POST, url);
	Map<String, String> post_data = new HashMap<String, String>();
	post_data.put( "From", from );
	post_data.put( "To",  to);
	post_data.put( "Body",  content);
	client.setPostData(post_data);
	int http_status = client.connect();
	if (http_status == 200) {
	    return client.getBody();
	}
	return null;
    }

}
