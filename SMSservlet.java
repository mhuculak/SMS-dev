// package com.twilio;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.util.*;

import com.twilio.sdk.verbs.TwiMLResponse;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.Message;

/*
       This servlet handles an incomming text message from Twilio
 */
public class SMSservlet extends HttpServlet {

    
    private MongoInterface m_mongo;
    
    public static String getBody(HttpServletRequest request) throws IOException {

	String body = null;
	StringBuilder stringBuilder = new StringBuilder();
	BufferedReader bufferedReader = null;
	
	try {
	    InputStream inputStream = request.getInputStream();
	    if (inputStream != null) {
		bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		char[] charBuffer = new char[128];
		int bytesRead = -1;
		while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
		    stringBuilder.append(charBuffer, 0, bytesRead);
		}
	    } else {
		stringBuilder.append("");
	    }
	} catch (IOException ex) {
	    throw ex;
	} finally {
	    if (bufferedReader != null) {
		try {
		    bufferedReader.close();
		} catch (IOException ex) {
		    throw ex;
		}
	    }
	}

	body = stringBuilder.toString();
	return body;
    }

    private String FindCompany(String company_phone_number) {
	String companyid = m_mongo.FindCompany(company_phone_number);
	if (companyid == null) {
	    return null;
	}
	if (companyid.contains("multiple")) {
	    return "multiple"; // FIXME: need a disambiguation mechanism
	}
	else if (companyid != null) {
            return companyid;
	}
	return null;
    }
    private String WaitForResponse(long timeout_sec, String entityid, String messageid) { // FIXME: timeout not working
	long timeout = timeout_sec * 1000;
	long start_time = System.currentTimeMillis();
	long elapsed_time = 0;
	while (elapsed_time < timeout) {
	    long current_time = System.currentTimeMillis();
	    elapsed_time = current_time - start_time;
	    String response = m_mongo.checkForResponse(entityid, messageid);
	    if (response != null) {
		return response;
	    }
	    try {
		Thread.sleep(1000);                 //1000 milliseconds is one second.
	    } catch(InterruptedException ex) {
		//		Thread.currentThread().interrupt();
		ex.printStackTrace();
	    }
	}	
	return null;
    }
    // service() responds to both GET and POST requests.
    // You can also use doGet() or doPost()
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String body = getBody(request);
	System.out.println(body);
        String[] info = body.split("&");
	String content = "";
	String from = "";
	String to = "";
	String query = request.getQueryString();
	PrintWriter out = null;
	Boolean unit_mode = false;
	int i;
	for ( i=0 ; i<info.length ; i++ ) {
	    String[] keyval = info[i].split("=");
	    if ( keyval.length == 2) {
		String key = keyval[0];		
		String value = URLDecoder.decode(keyval[1],"UTF-8");
		if (key.equals("Body")) {
		    //		    content = value.replace("+", " ");
		    content = value;
		    //		    System.out.println("SMSservlet got content = " + content);
		}
		else if (key.equals("From")) {
		    from = value;
		    //   System.out.println("SMSservlet got text from " + from);
		}
		else if (key.equals("To")) {
		    to = value;
		    // System.out.println("SMSservlet text to " + to);
		}
	    }
	}
	String db = request.getParameter("db");
	System.out.println("query = " + query + " db = " + db);
	m_mongo = MongoInterface.getInstance(db);
	if (db != null && db.equals("unit")) {
	    response.setContentType("text/plain");
	    unit_mode = true;
	    out = response.getWriter();
	    System.out.println("SMSservlet in unit mode");
	}
	
	from = from.replace("+", "");
	to = to.replace("+", "");

	System.out.println("adding customer " + from);
     	String customerid = m_mongo.AddCustomer(from);  // for now a customer is ID'd with their phone number
	CustomerStatus customer_status = m_mongo.getCustomerStatus(customerid);
	if (customer_status == null) {
	    customer_status = CustomerStatus.UNKNOWN;
	    System.out.println("set customer status to " + customer_status.toString());
	}
	System.out.println("create new SMS message with content = " + content);
	SMSmessage customer_message = new SMSmessage(content, from, to);
	customer_message.setName(from);
	customer_message.setCustomer(from);
	String companyid = FindCompany(to);
	System.out.println("call made to " + to + " has company id " + companyid);
	customer_message.setCompanyID(companyid);
	String customer_messageid = m_mongo.addMessage(customer_message);
		
	SMSrouter router = new SMSrouter(db, companyid, customerid);
	RouterResult router_result = router.doRoute(customer_messageid, content, customer_status);	

	if (unit_mode == true) {
	    System.out.println("sending auto response " + router_result.getAutoResponse());
	    out.println(router_result.getAutoResponse());
	}
	else {
	    TwiMLResponse twiml = new TwiMLResponse();	
	    Message message = new Message(router_result.getAutoResponse());

	    try {
		twiml.append(message);
	    } catch (TwiMLException e) {
		e.printStackTrace();
	    }
	
	    response.setContentType("application/xml");
	    response.getWriter().print(twiml.toXML());
	}
    }
}
