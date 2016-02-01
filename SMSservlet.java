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

    class RouterResult {
	private CustomerStatus m_status;
	private String m_entityid;
	private String m_auto_response;

	public RouterResult(CustomerStatus status, String entityid, String auto_response) {
	    m_status = status;
	    m_entityid = entityid;
	    m_auto_response = auto_response;
	}

	public CustomerStatus getStatus() {
	    return m_status;
	}

	public String getDestination() {
	    return m_entityid;
	}

	public String getAutoResponse() {
	    return m_auto_response;
	}
    }
    
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
	    String name = m_mongo.getEntityName(available.get(i));
	    sb.append("\n" + name);
	}
	return sb.toString();
    }

    private int MatchAnswer(String s1, String s2) {
	char[] a1 = s1.toCharArray();
	char[] a2 = s2.toCharArray();
	int min = s1.length() > s2.length() ? s2.length() : s1.length();
	int max = s1.length() > s2.length() ? s1.length() : s2.length();
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
    private int parseAnswer(List<String> choices, String answer) {
	int i;
	int best = -1;
	int min = 1000000;
	for ( i=0 ; i<choices.size() ; i++) {
	    String eid = choices.get(i);
	    String name = m_mongo.getEntityName(eid);
	    int score = MatchAnswer(answer, name);
	    if (score < min) {
		min = score;
		best = i;
	    }
	}
	return best;
    }
    // Find an employee that can respond to text message sent to company phone number
    private RouterResult FindRoute(String companyid, String customerid, CustomerStatus status, String answer) {
	String route = null;
	String auto_response = "";
        CustomerStatus new_status = null;	
	List<String> available = m_mongo.FindAvailableEntities(companyid);
	if (available == null || available.size() == 0) {
	    System.out.println("ERROR: no reps available");
	    route = null;
	    new_status = CustomerStatus.WAITING;
	    auto_response = "There are no available agents at the moment";
	}
	if (status == CustomerStatus.UNKNOWN) {
	    if (available.size() == 1) {
		System.out.println("selecting only available route " + available.get(0) + " with status = " + status);
	        route = available.get(0);
		new_status =  CustomerStatus.ROUTED;
	    }
	    else if (available.size() > 1) {
		m_mongo.setCustomerStatus(customerid, status);
		System.out.println("create routing question with status = " + status);
	        auto_response = createRoutingQuestion(available);
		new_status = CustomerStatus.PENDING_ROUTING;
	    }
	}
	else if (status == CustomerStatus.PENDING_ROUTING) {
	    if (available.size() == 1) {
		System.out.println("selecting only available route " + available.get(0) + " with status = " + status);
		route = available.get(0);
		new_status =CustomerStatus.ROUTED;
	    }
	    else if (available.size() > 1) {
		String selected_route = null;
		int best = parseAnswer(available, answer);
		if (best > -1) {
		    selected_route =  available.get(best);
		    System.out.println("selecting best available route " + selected_route + " with status = " + status);
		}
		/*
		  What to do if customers response does not match anything ?
		 */
		if (selected_route == null) {
		    System.out.println("selecting first route " + selected_route + " because no best route found with status = " + status);
		    selected_route = available.get(0); // lame fallback strategy
		}
		m_mongo.setCustomerStatus(customerid, status);
		route = selected_route;
		new_status = CustomerStatus.ROUTED;
	    }
	}
	else if (status == CustomerStatus.ROUTED) {
	    System.out.println("selecting existing route with status = " + status);
	    route = m_mongo.getCustomerRoute(customerid);
	    new_status = status;
	}
	if (new_status == CustomerStatus.ROUTED) {
	    System.out.println("routing customer " + customerid + " to entity " + route); 
	    m_mongo.RouteCustomerToEntity(route, customerid);
	}
	return new RouterResult(new_status, route, auto_response);
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
	CustomerStatus customer_status = m_mongo.getCustomerStatus(customerid);
	if (customer_status == null) {
	    customer_status = CustomerStatus.UNKNOWN;
	    System.out.println("set customer status to" + customer_status.toString());
	}
	System.out.println("create new SMS message with content = " + content);
	SMSmessage customer_message = new SMSmessage(content, from, to);
	customer_message.setName(from);
	customer_message.setCustomer(from);
	String companyid = FindCompany(to);
	System.out.println("call made to " + to + " has company id " + companyid);
	customer_message.setCompanyID(companyid);
	String customer_messageid = m_mongo.addMessage(customer_message);
	/*
            If we do not call setCustomerMessage(), the message will not get routed.
            In the case of a customer in state PENDING_ROUTING, the router is asking
            questions to determine where to route. Therfore we do not route the answers
            as they are intended for the router rather than the business rep
	 */
	if (customer_status != CustomerStatus.PENDING_ROUTING) {
	    m_mongo.setCustomerMessage(customerid, customer_messageid);
	}
	RouterResult router_result = FindRoute(companyid, customerid, customer_status, content);
        System.out.println("customer status is now " + router_result.getStatus().toString());
	m_mongo.setCustomerStatus(customerid, router_result.getStatus());

	if (router_result.getStatus() == CustomerStatus.WAITING) { // nothing available
	    m_mongo.addCustomerToWaitingQueue(companyid, customerid);
	}

	System.out.println("new customer message has id " + customer_messageid);

	if (html_mode == true) {
	    out.println("<h1>reponse:</h1>");
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
