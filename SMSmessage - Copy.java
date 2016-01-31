import java.util.*;

enum SMSmessageStatus { WAITING, RESPONDED, REDIRECTED, ERROR };
    
public class SMSmessage {
    private String m_time;       // time stamp
    private String m_type;       // customer or business i.e which side the message originated from
    private String m_content;    // the message
    private String m_from;       // originating phone number
    private String m_to;         // destination phone number
    private String m_companyid;  // id of company sending/receiving the message
    private String m_entityid;   // id of the employee sending/receiving
    private String m_name;       // name of originator
    private String m_customer;   // customer who sent or recived it
    private SMSmessageStatus m_status;     // waiting, responded, forwarded

    public static String getCurrentTime() {
      Calendar calendar = new GregorianCalendar();
      String am_pm;
      int hour = calendar.get(Calendar.HOUR);
      int minute = calendar.get(Calendar.MINUTE);
      int second = calendar.get(Calendar.SECOND);
      if(calendar.get(Calendar.AM_PM) == 0)
        am_pm = "AM";
      else
        am_pm = "PM";
 
      String CT = hour+":"+ minute +":"+ second +" "+ am_pm;
      return CT;
    }


    public SMSmessage(String content, String from, String to,
		      String companyid, String entityid) { // ctor for business originated message
	m_time = getCurrentTime();
	m_type = "business";
	m_content = content;
	m_from = from;
	m_to = to;
	m_companyid = companyid;
	m_entityid = entityid;
	m_name = m_type;
	m_status = SMSmessageStatus.WAITING;
    }
    
    public SMSmessage(String content, String from, String to) { // ctor for customer originated message
	m_time = getCurrentTime();
	m_type = "customer";
	m_content = content;
	m_from = from;
	m_to = to;
	m_companyid = null;
	m_entityid = null;
	m_name = m_type;
	m_status = SMSmessageStatus.WAITING;
    }

    public String getTime() {
	return m_time;
    }

    public void setTime(String time) {
	m_time = time;
    }

    public String getType() {
	return m_type;
    }

    public void setType(String type) {
	m_type = type;
    }

    public String getContent() {
	return m_content;
    }

    public String getCompanyID() {
	return m_companyid;
    }

    public void setCompanyID(String companyid) {
	m_companyid = companyid;
    }

    public String getEntityID() {
	return m_entityid;
    }

    public void setEntityID(String entityid) {
	m_entityid = entityid;
    }

    public String getName() {
	return m_name;
    }

    public void setName(String name) {
	m_name = name;
    }

    public void setStatus(SMSmessageStatus.WAITING;
    }
    
    public SMSmessage(String content, String from, String to) { // ctor for customer originated message
	m_time = getCurrentTime();
	m_type = "customer";
	m_content = content;
	m_from = from;
	m_to = to;
	m_companyid = null;
	m_entityid = null;
	m_name = m_type;
	m_status = SMSmessageStatus.WAITING;
    }

    public String getTime() {
	return m_time;
    }

    public void setTime(String time) {
	m_time = time;
    }

    public String getType() {
	return m_type;
    }

    public void setType(String type) {
	m_type = type;
    }

    public String getContent() {
	return m_content;
    }

    public String getCompanyID() {
	return m_companyid;
    }

    public void setCompanyID(String companyid) {
	m_companyid = companyid;
    }

    public String getEntityID() {
	return m_entityid;
    }

    public void setEntityID(String entityid) {
	m_entityid = entityid;
    }

    public String getName() {
	return m_name;
    }

    public void setName(String name) {
	m_name = name;
    }
    
    public void setStatus(String status) {
	if (status.equals("WAITING")) {
	    m_status = SMSmessageStatus.WAITING;
	}
	else if (status.equals("RESPONDED")) {
	    m_status = SMSmessageStatus.RESPONDED;
	}
	else if (status.equals("REDIRECTED")) {
	    m_status = SMSmessageStatus.REDIRECTED;
	}
	else {
	    m_status = SMSmessageStatus.ERROR;
	}
    }
    
    public void setStatus(SMSmessageStatus status) {
	m_status = status;
    }

    public SMSmessageStatus getStatus() {
	return m_status;
    }

    public void setCustomer(String customer) {
	m_customer = customer;
    }

    public String getCustomer() {
	return m_customer;
    }
    
    public String toDisplayString() {
	return m_time + " " + m_name + ":" + m_content;
    }
       
}
