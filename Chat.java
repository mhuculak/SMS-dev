import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

/*
      To do:

        1. relies on auto refresh: want event from SMSservlet when new message arrives
        2. relies on auto refresh: want event from EmployeeInput when new message arrives
        3. Chat window needs to show to scroll so latest message is always visible (instead of the oldest message)
 */
public class Chat extends HttpServlet {
    private MongoInterface m_mongo;

     public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();

	response.setContentType("text/html");
	response.setIntHeader("Refresh", 5);
	Date curr = new Date();
	String curr_time = curr.toString();
	PrintWriter out = response.getWriter();

	int i;
	if (query == null) {
	    System.out.println("ERROR: no company specified");
	}
	else {
	    String db = request.getParameter("db");
	    m_mongo = MongoInterface.getInstance(db);	
	    if (query.contains("companyid")) {
		String companyid = request.getParameter("companyid");
		if (query.contains("entityid")) {
		    String entityid = request.getParameter("entityid");
		    String name = m_mongo.getEntityName(entityid);
		    out.println("<h1>" + name + "'s Chat Window</h1>");
		    String customer = null;
		    String focus = m_mongo.getEntityFocus(entityid);
		    List<String>customers = m_mongo.FindRoutedCustomers(entityid);
		    /*
		    if (focus != null) {
			out.println("<h2>Chat window with companyid = " + companyid + " entityid = " + entityid + " focus = " + focus + " num customers = " + customers.size() + "</h2>");
		    }
		    else {
		        System.out.println("focus is null");
			out.println("<h2>Chat window with companyid = " + companyid + " entityid = " + entityid + "  num customers = " + customers.size() + "</h2>");
		    }
		    */
		    if (query.contains("customer")) {
			customer = request.getParameter("customer"); // this happens when the customer is explicitly selected
			focus = customer;
		    }
		    else if (focus != null) {
			customer = focus;
		    }
		    else if (customers.size() == 1) {
			customer = customers.get(0);  // when only one customer we simply use it by default
		    }
		    else if (customers.size() > 1) {
			customer = customers.get(0); // use first customer by default when more than one
		    }
		    m_mongo.setEntityFocus(entityid, customer);

                    out.println("<table border=\"1\" width=\"100%\">");
		    out.println("<tr><td width=\"20%\"><b>Customer</b></td><td width=\"80%\"><b>Messages</b></td></tr>");
		    out.println("<tr><td width=\"20%\"><br>");
		    out.println("<table><br>");
		    
		    for ( i=0 ; i<customers.size() ; i++ ) {
			String uri = "/Chat?companyid=" + companyid + "&entityid=" + entityid + "&customer=" + customers.get(i);
			String customer_messageid = m_mongo.getCustomerMessageFromPhone(customers.get(i));
			SMSmessage customer_message = m_mongo.getMessage(customer_messageid);
			//  System.out.println("Customer message is " + customer_messageid + " message status is " + customer_message.getStatus());
			String customer_html = "";
			if (focus.equals(customers.get(i))) {
			    customer_html = "<b>" + customers.get(i) + "</b>";
			}
			else {
			    customer_html = customers.get(i);
			}
			if (customer_message.getStatus() == SMSmessageStatus.WAITING) {
			    customer_html = "<blink>" + customer_html + "</blink>"; // FIXME: <blink> does not work
			}
			out.println("<tr><td><a href=\"" + uri + "\">" + customer_html + "</td></tr><br>");
		    }
		    out.println("</table>");
		    out.println("</td><td width=\"80%\"><br>");
		    
		    if (customer != null) {
			//			System.out.println("display messages for customer " + customer);
			List<SMSmessage> messages = m_mongo.getMessages(companyid, entityid, customer);
			out.println("<table><br>");
			for ( i=0 ; i<messages.size() ; i++ ) {
			    SMSmessage mess = messages.get(i);
			    if (mess.getType().contains("business")) {
				out.println("<tr><td><font color=\"blue\">" + mess.toDisplayString() + "</font></td></tr><br>");
			    }
			    else {
				out.println("<tr><td><font color=\"red\">" + mess.toDisplayString() + "</font></td></tr><br>");
			    }
			}
		    }
		    
		    out.println("</table>");
		    out.println("</td></tr></table>");
				
		}
		else {
		    System.out.println("ERROR: no entity specified");
		}
	    }
	    else {
		System.out.println("ERROR: no company specified");
	    }
	}
    }
}

	
