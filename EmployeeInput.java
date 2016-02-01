import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

import com.twilio.sdk.TwilioRestException;

public class EmployeeInput extends HttpServlet {
    private MongoInterface m_mongo;

    private String TextInputForm(String companyid, String entityid) {
	String inform = "<form action=\"EmployeeInput\" method=\"GET\"><br>" +
	    "<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" + 	    
	    "<input type=\"hidden\" name=\"entityid\" value=\"" + entityid + "\"/><br>" + 	    
	    "Send Message: <input type=\"text\" name=\"message\" size=\"50\"/>" +
	    "</form><br>";
	return inform;
    }
    /*
          FIXME: to change status the rep must select the status from the menu and then submit. It is very is to forget to submit
                 and the rep is left in the incorrect state ===> we badly need a single click solution
     */
    private String StateInputForm(String companyid, String entityid, String state) {
	String stateform;
	if (state == null) {
	    return "<br><h2>ERROR: found state = null</h2><br>";
	}
	if (state.equals("available")) {
	    stateform =  "<form action=\"EmployeeInput\" method=\"GET\"><br>" +
		"<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" + 	    
		"<input type=\"hidden\" name=\"entityid\" value=\"" + entityid + "\"/><br>" + 	    
		"Status: <select name=\"status\"><br><option value=\"offline\">offline</option><option value=\"available\" selected>available</option></select><br>" +
		"<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
	}
	else if (state.equals("offline")) {
	   stateform =  "<form action=\"EmployeeInput\" method=\"GET\"><br>" +
	       "<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" + 	    
	       "<input type=\"hidden\" name=\"entityid\" value=\"" + entityid + "\"/><br>" + 	    
	       "Status: <select name=\"status\"><br><option value=\"offline\" selected>offline</option><option value=\"available\">available</option></select><br>" +
	       "<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>"; 
	}
	else {
	    stateform = "<br><h2>ERROR: found state = " + state + "</h2><br>";
	}
	return stateform;	
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();
	m_mongo = MongoInterface.getInstance();	
	response.setContentType("text/html");
	PrintWriter out = response.getWriter();
	if (query == null) {
	    System.out.println("ERROR: no company specified");
	}
	else {
	    if (query.contains("companyid")) {
		String companyid = request.getParameter("companyid");
		if (query.contains("entityid")) {
		    String entityid = request.getParameter("entityid");
		    if (query.contains("message")) {
			String customer = m_mongo.getEntityFocus(entityid);
			String customer_messageid = m_mongo.getCustomerMessageFromPhone(customer);
			System.out.println("entity " + entityid + " sending message to customer " + customer + " messageid " + customer_messageid);
			SMSmessage customer_message = m_mongo.getMessage(customer_messageid);
			/*
                            Note: We create the response message by swapping the "from" and "to" phone numbers of the customer's message
                         */
			SMSmessage business_response = new SMSmessage(request.getParameter("message"),
								      customer_message.getTo(), customer_message.getFrom(), 
								      companyid, entityid);
			business_response.setCustomer(customer);

			SMSsender sender = new SMSsender(business_response, m_mongo);
			String sid = sender.sendMessage(companyid);
			if (sid == null) {
			    System.out.println("Failed to send response to " + business_response.getTo());
			}
			else {
			    business_response.setSid(sid);
			    m_mongo.setMessageStatus(customer_messageid, SMSmessageStatus.RESPONDED);
			    System.out.println("response sent to " + business_response.getTo());
			}
			String name = m_mongo.getEntityName(entityid);
			business_response.setName(name);
			String business_messageid = m_mongo.addMessage(business_response);
			if (sid == null) {
			     m_mongo.setMessageStatus(business_messageid, SMSmessageStatus.ERROR);
			}

		    }
		    if (query.contains("status")) {
			String status = request.getParameter("status");
			m_mongo.SetEntityState(companyid, entityid, status);
			if (status.equals("offline")) {
			    m_mongo.removeCustomerRoutes(entityid); // when offline customers need to be routed elsewhere
			}
			else if (status.equals("available")) {
			    String waiting_customer = m_mongo.removeCustomerFromWaitingQueue(companyid);
			    while(waiting_customer != null) {
				System.out.println("route customerid " + waiting_customer + " to entity " + entityid);
				Boolean ok = m_mongo.RouteCustomerToEntity(entityid, waiting_customer);
				if (ok == false) {
				    System.out.println("ERROR: failed to route " + waiting_customer + " to entity " + entityid);
				}
				waiting_customer = m_mongo.removeCustomerFromWaitingQueue(companyid);
			    }
			}
		    }
		    
		    String state = m_mongo.GetEntityState(companyid, entityid);
		    out.println("<table><tr><td><br>");
		    out.println(TextInputForm(companyid, entityid));
		    out.println("</td><td><br>");
		    out.println(StateInputForm(companyid, entityid, state));
		    out.println("</td></tr></table><br>");
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
