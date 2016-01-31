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
			String customer_messageid = m_mongo.getCustomerMessage(customer);
			System.out.println("entity " + entityid + " sending message to customer " + customer + " messageid " + customer_messageid);
			SMSmessage customer_message = m_mongo.getMessage(customer_messageid);
			/*
                            Note: We create the response message by swapping the "from" and "to" phone numbers of the customer's message
                         */
			SMSmessage business_response = new SMSmessage(request.getParameter("message"),
								      customer_message.getTo(), customer_message.getFrom(), 
								      companyid, entityid);
			business_response.setCustomer(customer);

			SMSsender sender = new SMSsender(business_response);
			String sid = sender.sendMessage();
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
			m_mongo.SetEntityState(companyid, entityid, request.getParameter("status"));
		    }
		    
		    String state = m_mongo.GetEntityState(companyid, entityid);
		    out.println(TextInputForm(companyid, entityid));
		    out.println(StateInputForm(companyid, entityid, state));
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
