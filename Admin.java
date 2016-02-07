import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

import com.mongodb.BasicDBList;

public class Admin extends HttpServlet {

    private MongoInterface m_mongo;

    private String AddEntityForm(String companyid, String parent, String parent_name, String parent_type) {
	String head;
	String parentdata = "";
	if (parent != null) {
	    parentdata = "<input type=\"hidden\" name=\"entityid\" value=\"" + parent + "\"/><br>";
	}
	if (parent_name == null) {
	    head = "<h1>Add To Top Level:</h1><br>";
	}
	else {
	    if (parent_type.equals("pool")) {
		head = "<h1>Add To " + parent_name + " Staff:</h1><br>";
	    }
	    else {
		head = "<h1>Add To " + parent_name + ":</h1><br>";
	    }
	}
	String form = head +
	    "<form action=\"Admin\" method=\"GET\"><br>" +
	    "<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>" + 	    
	    parentdata +
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

    private void ShowEntities(List<Entity> entities, String companyid, String company_name,
			      String parent, String parent_name, String parent_type, String grandparent, PrintWriter out) {
	String return_uri = null;
	if (grandparent != null) {
	    return_uri = "/Admin?companyid=" + companyid + "&entityid=" + grandparent;
	}
	else {
	    return_uri = "/Admin?companyid=" + companyid;
	}
	if (parent_name == null) {
	    out.println("<h1>Top level members of " + company_name +":</h1>");
	}
	else {
	    if (parent_type.equals("pool")) {
		out.println("<h1>" + company_name + " " + parent_name + " Staff:</h1>");
	    }
	    else {
		out.println("<h1>" + company_name + " members of " + parent_name + " :</h1>");
	    }
	}
	out.println("<table cellspacing=\"30\">");
	out.println("<br><tr><td><b>Entity Name</b></td><td><b>Type</b></td><td><b>email</b></td><td><b>Delete It</b></td><td><b>Configure It</b></td></tr>");
	int i=0;
	for ( i=0 ; i<entities.size() ; i++ ) {
	    Entity entity = entities.get(i); 
	    out.println("<br><tr>");	    
	    String entity_uri;
	    if (parent == null) {
		entity_uri = "/Admin?companyid=" + companyid + "&entityid=" + entity.getID();
	    }
	    else {
		entity_uri = "/Admin?companyid=" + companyid + "&entityid=" + entity.getID() + "&parent=" + parent;
	    }
	    String type = entity.getType();
	    if (type.equals("employee")) {
		out.println("<td><a href=\"Employee?companyid=" + companyid + "&entityid=" + entity.getID() + "\">" + entity.getName() + "</a></td>");
	    }
	    else {
		out.println("<td>" + entity.getName() + "</td>");
	    }

	    out.println("<td>" + type + "</td>");
            if (type.equals("employee")) {
		out.println("<td>" + entity.getEmail() + "</td>");
	    }
	    else {
		out.println("<td></td>");
	    }
	    out.println("<td><form action=\"Admin\" method=\"GET\">" +
			"<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/>" +
			"<input type=\"hidden\" name=\"action\" value=\"delete\"/>" +
			"<input type=\"submit\" name=\"delete\" value=\"" + entity.getID() + "\"/></form></td>");
	    out.println("<td><a href=\"" + entity_uri + "\">" + entity.getName() + "</a></td>");
	    out.println("</tr>");	    
	}
	out.println("</table>");
	if (return_uri != null) {
	    out.println("<br><br><a href=\"" + return_uri + "\">Return</a>");
	}
    }

    private void editEmployee(String companyid, Entity employee, String parent, PrintWriter out) {
	String return_uri = null;
	if (parent != null) {
	    return_uri = "/Admin?companyid=" + companyid + "&entityid=" + parent;
	}
	else {
	    return_uri = "/Admin?companyid=" + companyid;
	}
	out.println("<h1>Edit " + employee.getName() + ":</h1>");
	out.println("<form action=\"Admin\" method=\"GET\"><br>");
	out.println("<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>");
	out.println("<input type=\"hidden\" name=\"entityid\" value=\"" + employee.getID() + "\"/><br>");
	out.println("<input type=\"hidden\" name=\"action\" value=\"edit\"/><br>");
	out.println("<input type=\"hidden\" name=\"type\" value=\"employee\"/><br>");
	out.println("Name: <input type=\"text\" name=\"name\" value=\"" + employee.getName() + "\"/><br>");
	out.println("email: <input type=\"text\" name=\"email\" value=\"" + employee.getEmail() + "\"/><br>");
	out.println("<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>");

	out.println("<h2>" + employee.getName() + "'s Keywords:</h2>");
	BasicDBList keywords = m_mongo.getKeywords(employee.getID());
	if (keywords != null) {
	    for (Object keyword : keywords) {
		out.println(keyword.toString() + ",");
	    }
	}
	out.println("<form action=\"Admin\" method=\"GET\"><br>");
	out.println("<input type=\"hidden\" name=\"companyid\" value=\"" + companyid + "\"/><br>");
	out.println("<input type=\"hidden\" name=\"entityid\" value=\"" + employee.getID() + "\"/><br>");
	out.println("<input type=\"hidden\" name=\"action\" value=\"keyword\"/><br>");
	out.println("Add Keyword: <input type=\"text\" name=\"keyword\"/><br>"); 
	out.println("<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>");
		    
	out.println("<br><br><a href=\"" + return_uri + "\">Return</a>");
	
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();
	m_mongo = MongoInterface.getInstance();	
	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	String entity_name = null;
	String entity_type = null;
	String entityid = null;
	String parent = null;
	
	if (query == null) {
	    System.out.println("ERROR: no company specified");
	}
	else {
	    if (query.contains("companyid")) {
		String companyid = request.getParameter("companyid");
		String company_name = m_mongo.GetCompanyName(companyid);
		if (query.contains("entityid")) {
		    entityid = request.getParameter("entityid");
		    entity_name = m_mongo.getEntityName(entityid);
		    entity_type =  m_mongo.getEntityType(entityid);
		}
		
		if (query.contains("parent")) {
		    parent = request.getParameter("parent");
		}		
		
	        if (query.contains("action")) {
		    String action = request.getParameter("action");
		    String name = request.getParameter("name");
		    String return_uri;
		    if (entityid == null) {
			return_uri = request.getRequestURI() + "?companyid=" + companyid;
		    }
		    else {
			return_uri = request.getRequestURI() + "?companyid=" + companyid + "&entityid=" + entityid;
		    }
		    System.out.println("action = " + action + " return uri = " + return_uri);
		    if (action.equals("add")) {
			String type = request.getParameter("type");			
			String email = request.getParameter("email");
			String new_entity_id = m_mongo.AddEntity(companyid, entityid, name, email, type);
			if (type.equals("employee")) {
			    String subject = "You have been added to our SMS service";
			    String service_uri = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Employee?companyid=" + companyid + "&entityid=" + new_entity_id;
			    String  email_body = "Congradulations " + name + ", you have been added to your company's text message service." +
			     "\n\nPlease use the following link " + service_uri + " to use this service";
			    EmailSender.SendEmail(subject, email_body, email);
			}			
		    }
		    else if (action.equals("edit")) {
			m_mongo.setEntityName(entityid, name);
			String email = request.getParameter("email");
			m_mongo.setEntityEmail(entityid, email);
		    }
		    else if (action.equals("keyword")) {
			String keyword = request.getParameter("keyword");
			m_mongo.addKeyword(entityid, keyword);
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
		    if (entity_type == null || entity_type.equals("pool")) {		    
			List<Entity> entities = m_mongo.GetEntities(companyid, entityid);
			ShowEntities(entities, companyid, company_name, entityid, entity_name, entity_type, parent, out);
			out.println(AddEntityForm(companyid, entityid, entity_name, entity_type));
			if (entityid.equals("0")) {
			    out.println( AddAdvertismentForm(companyid) ); // advertisments only at the top level
			}
		    }
		    else if (entity_type.equals("employee")) {
			Entity entity = m_mongo.getEntity(entityid);
			editEmployee(companyid, entity, parent, out);
		    }
		}
	    }
	    else {
		System.out.println("ERROR: no company specified");
	    }
	}
    }
}
