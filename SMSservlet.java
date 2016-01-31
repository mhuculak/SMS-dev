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
    private String createRoutingQuestion(List<String> available) {
	StringBuilder sb = new StringBuilder(100);
	sb.append("Which of the following would you like to answer your question?\n");
	int i;
	for ( i=0 ; i<available.size() ; i++) {
	    sb.append("\n" + available.get(i));
	}
	return sb.toString();
    }

    private int MatchAnswer(String s1, String s2) {
	char[] a1 = s1.toCharArray();
	char[] a2 = s2.toCharArray();
	int min = s1.size() > s2.size() ? s2.size() : s1.size();
	int max = s1.size() > s2.size() ? s1.size() : s2.size();
	int extra = max-min; // added to score to account for difference in size
	int i;
	int score = 0;
	for ( i=0 ; i<min ; i++ ) {
	    if (Character.toLowerCase(a1[i]) != Character.toLowerCase(a2[i])) { // case independant
		score++;
	    }
	}
	return score+extra;
	
    }
    // Find an employee that can respond to text message sent to company phone number
    private String FindRoute(String companyid, String customerid, CustomerStatus status, String answer) {
	List<String> available = m_mongo.FindAvailableEntities(companyid);
	if (status == CustomerStatus.UNKNOWN) {
	    return createRoutingQuestion(available);
	}
	else if (status == CustomerStatus.PENDING_ROUTING) {	   
	    if (available != null && available.size() > 0) {
		int i;
		int best = -1;
		int min = 1000000;
		for ( i=0 ; i<available.size() ; i++) {
		    String eid = available.get(i);
		    String name = m_mongo.getEntityName(eid);
		    int score = MatchAnswer(answer, name);
		    if (score < min) {
			min = score;
			best = i;
		    }
		}
		if (best > -1) {
		    return available.get(best);
		}
		/*
		  What to do if customers response does not match anything ?
		 */
		return avaiable.get(0); // lame fallback strategy
	    }
	    else {
		return null;
	    }
	}
	else if (status == CustomerStatus.ROUTED) {
	    return m_mongo.getCustomerRoute(customerid);
	}
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
    /*
         FIXME: this method has a race condition caused by the fact that the Twilio side will timeout relatively quickly
                (after about 12 seconds). If the business side does not respond within this window we need to 
                send the response as are regular outgoing "unsolicited" message which exposes us to the chance
                the response can be sent twice
     */    
    // service() responds to both GET and POST requests.
    // You can also use doGet() or doPost()
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String body = getBody(request);
        String[] info = body.split("&");
	String content = "";
	String from = "";
	String to = "";
	String query = request.getQueryString();
	PrintWriter out = null;
	int i;
	for ( i=0 ; i<info.length ; i++ ) {
	    String[] keyval = info[i].split("=");
	    if ( keyval.length == 2) {
		String key = keyval[0];		
		String value = URLDecoder.decode(keyval[1],"UTF-8");
		if (key.equals("Body")) {
		    //		    content = value.replace("+", " ");
		    content = value;
		}
		else if (key.equals("From")) {
		    from = value;
		}
		else if (key.equals("To")) {
		    to = value;
		}
	    }
	}
	m_mongo = MongoInterface.getInstance();

	Boolean html_mode = false; // html mode is used for debugging
	if (query != null && query.contains("content")) {
	    html_mode = true;
	    query = request.getQueryString();
	    response.setContentType("text/html");
	    out = response.getWriter();
	    content = request.getParameter("content");
	    to = request.getParameter("to");
	    from = request.getParameter("from");
        }
	else {
	    from = from.replace("+", "");
	    to = to.replace("+", "");
	}
	System.out.println("adding customer " + from);
     	String customerid = m_mongo.AddCustomer(from);  // for now a customer is ID'd with their phone number
	String customer_status = m_mongo.getCustomerStatus(customerid);
	if (customer_status == null) {
	    customer_status = CustomerStatus.UNKNOWN;
	    m_mongo.setCustomerStatus(customer_status);
	}
	SMSmessage customer_message = new SMSmessage(content, from, to);
	customer_message.setName(from);
	customer_message.setCustomer(from);
	String companyid = FindCompany(to);
	System.out.println("call made to " + to + " has company id " + companyid);
	customer_message.setCompanyID(companyid);
	String destination_entityid = FindRoute(companyid, customerid, customer_status, content);
	if (destination_entityid == null) {
	    destination_entityid = "no route found";
	}
	else {
	    System.out.println("found route to entity " + destination_entityid);
	    Boolean routed_ok  = m_mongo.RouteCustomerToEntity(destination_entityid, from);
	    if (routed_ok) {
		System.out.println("message successfully routed to entity " + destination_entityid);
		customer_message.setEntityID(destination_entityid);
	    }
	}
        String customer_messageid = m_mongo.addMessage(customer_message);
	m_mongo.setCustomerMessage(customerid, customer_messageid);
	System.out.println("new customer message has id " + customer_messageid);
	long timeout_sec = 12; 
	String business_response = WaitForResponse(timeout_sec, destination_entityid, customer_messageid);
	if (business_response == null) { // timeout
	    /*

            Note: in the case of a timeout, EmployeeInput will send the response so do nothing

	    business_response = timeout_sec + "sec timeout expired";
	    SMSmessage timeout_response_message = new SMSmessage(business_response, companyid, destination_entityid );
	    m_mongo.addMessage(timeout_response_message);

	    */
	    System.out.println("entity did not respond after " + timeout_sec + " sec timeout, sending null response");
	    m_mongo.setMessageStatus(customer_messageid, SMSmessageStatus.TIMEOUT);
	}
	else {
	    System.out.println("entity responded before timeout with " + business_response);
	}
	if (html_mode == true) {
	    out.println("<h1>reponse:</h1>");
	    out.println(business_response);
	}
	else {
	    TwiMLResponse twiml = new TwiMLResponse();
	    // Message message = new Message(business_response);
	    Message message = new Message("");  // send nothing cuz EmployeeInput sends the message
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
