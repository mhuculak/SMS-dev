import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;


public class Admin extends HttpServlet {

    private MongoInterface m_mongo;

    private String AddEntityForm(String companyid, String parent) {
	String form = "<h1>Add Entity From " + parent + " Here:</h1><br>" +
	    "<form action=\"Admin\" method=\"GET\"><br>" +
	    "<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" + 	    
	    "<input type=\"hidden\" name=\"parent\" value=\"" + parent + "\"/><br>" +
	    "<input type=\"hidden\" name=\"action\" value=\"add\"/><br>" +
	    "Name: <input type=\"text\" name=\"name\"/><br>" +
	    "Type: <select name=\"type\"/><option value=\"group\">group</option><option value=\"pool\">pool</option><option value=\"employee\">employee</option></select><br>" +
	    "email: <input type=\"text\" name=\"email\"/><br>" +
	    "<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
	return form;
    }

    private String AddAdvertismentForm(String companyid) {
	String form = "<h1>Add Advertisment:</h1><br>" +
	    "<form action=\"Admin\" method=\"GET\"><br>" +
	    "<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" +
	    "<input type=\"hidden\" name=\"action\" value=\"advertisment\"/><br>" +
	    "Advertisment Name: <input type=\"text\" name=\"name\"/><br>" +
	    "Advertisment Content: <input type=\"text\" name=\"content\"/><br>" +
	    "<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
	return form;
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();
	m_mongo = MongoInterface.getInstance();	
	response.setContentType("text/html");
	PrintWriter out = response.getWriter();
	String parent = "0"; // top level
	if (query == null) {
	    System.out.println("ERROR: no company specified");
	}
	else {
	    if (query.contains("companyid")) {
		String companyid = request.getParameter("companyid");
		if (query.contains("parent")) {
		    parent = request.getParameter("parent");
		}
		
	        if (query.contains("action")) {
		    String action = request.getParameter("action");
		    String name = request.getParameter("name");
		    String return_uri = request.getRequestURI() + "?companyid=" + companyid + "&parent=" + parent;
		    System.out.println("action = " + action + " return uri = " + return_uri);
		    if (action.equals("add")) {
			String entity_type = request.getParameter("type");			
			String email = request.getParameter("email");
			String entity_id = m_mongo.AddEntity(companyid, parent, name, email, entity_type);
			if (entity_type.equals("employee")) {
			    String subject = "You have been added to our SMS service";
			    String service_uri = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Employee?companyid=" + companyid + "&entityid=" + entity_id;
			    String  email_body = "Congradulations " + name + ", you have been added to your company's text message service." +
			     "\n\nPlease use the following link " + service_uri + " to use this service";
			    EmailSender.SendEmail(subject, email_body, email);
			}			
		    }
		    else if (action.equals("delete")) {
			String delete_entityid = request.getParameter("delete");
			System.out.println("delete entity " + delete_entityid);
			m_mongo.RemoveEntity(delete_entityid);
		    }
		    else if (action.equals("advertisment")) {
			
			String add_id = m_mongo.addNewAdvertisment(companyid, name, request.getParameter("content"));
			if (add_id == null) {
			    out.println("<h2>ERROR: advertisment with companyid = " + companyid + " and name = " + name + " already exists</h2>");
			}
			else {
			    out.println("<h2>Advertisment " + name + " was added</h2>");
			}
		    }
		    out.println("<br><br><a href=\"" + return_uri + "\">Return</a>");
		}
		else {
		    m_mongo.ShowEntities(companyid, parent, out);
		    out.println(AddEntityForm(companyid, parent));
		    out.println( AddAdvertismentForm(companyid) );
		}
	    }
	    else {
		System.out.println("ERROR: no company specified");
	    }
	}
    }
}
