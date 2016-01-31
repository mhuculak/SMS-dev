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
	m_mongo = MongoInterface.getInstance();	
	response.setContentType("text/html");
	response.setIntHeader("Refresh", 5);
	String curr_time = SMSmessage.getCurrentTime();
	PrintWriter out = response.getWriter();
	out.println("<h1>Current Time is: " + curr_time + "</h1>");
	int i;
	if (query == null) {
	    System.out.println("ERROR: no company specified");
	}
	else {
	    if (query.contains("companyid")) {
		String companyid = request.getParameter("companyid");
		if (query.contains("entityid")) {
		    String entityid = request.getParameter("entityid");
		    String customer = null;
		    String focus = m_mongo.getEntityFocus(entityid);
		    List<String>customers = m_mongo.FindRoutedCustomers(entityid);
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
			if (focus.equals(customers.get(i))) {
			    out.println("<tr><td><a href=\"" + uri + "\"><b>" + customers.get(i) + "</b></td></tr><br>");
			}
			else {
			    out.println("<tr><td><a href=\"" + uri + "\">" + customers.get(i) + "</td></tr><br>");
			}
		    }
		    out.println("</table>");
		    out.println("</td><td width=\"80%\"><br>");

		    
		    
		    if (customer != null) {			
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

	
