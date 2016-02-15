import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

public class SuperAdmin extends HttpServlet {

    private MongoInterface m_mongo;
    private static String m_addCompanyForm = "<h1>Add a New Company Here:</h1><br>" +
	                                     "<form action=\"SuperAdmin\" method=\"GET\"><br>" +
	                                     "Name: <input type=\"text\" name=\"name\"/><br>" +
	                                     "email: <input type=\"text\" name=\"email\"/><br>" +
	                                     "Phone: <input type=\"text\" name=\"phone\" value=\"14387949914\"/><br>" +
	                                     "ACCOUNT_SID: <input type=\"text\" name=\"sid\" value=\"ACe33e12e73a0028063395a5eb8d30cc26\"/><br>" +
	                                     "AUTH_TOKEN: <input type=\"text\" name=\"token\" value=\"8da21493e68ff088f7f27994583549e0\"/><br>" +
	                                     "<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
   

    private void ShowCompanies(List<Company> companies, PrintWriter out) {
	out.println("<h1>Company List:</h1>");
	out.println("<table cellspacing=\"30\">");
	out.println("<br><tr><td><b>Company Name</b></td><td><b>email address</b></td><td><b>Phone Number</b></td><td><b>ID</b></td><td><b>Delete It</b></td><td><b>Activate It</b></td><td><b>Configure It</b></td></tr>");
	int i;
	for ( i=0 ; i < companies.size() ; i++ ) {
	    Company company = companies.get(i);
	    out.println("<br><tr>");
	    out.println("<td>" + company.getName() + "</td>");
	    out.println("<td>" + company.getEmail() + "</td>");
	    out.println("<td>" + company.getPhone() + "</td>");
	    out.println("<td>" + company.getID() + "</td>");
	    out.println("<td><form action=\"SuperAdmin\" method=\"GET\">" +
			"<input type=\"hidden\" name=\"action\" value=\"delete\"/>" +
			"<input type=\"submit\" name=\"delete\" value=\"" + company.getID() + "\"/></form></td>");
	    out.println("<td><form action=\"SuperAdmin\" method=\"GET\">" +
			"<input type=\"hidden\" name=\"action\" value=\"activate\"/>" +
			"<input type=\"hidden\" name=\"phone\" value=\"" + company.getPhone() + "\"/>" +
			"<input type=\"submit\" name=\"activate\" value=\"" + company.getID() + "\"/></form></td>");
	    out.println("<td><a href=\"Admin?companyid=" + company.getID() + "\">Configure " + company.getName() + "</a></td>");
	    out.println("</tr><br>");
	    
	}
	out.println("</table>");
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();
	
	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	if (query == null) {
	    m_mongo = MongoInterface.getInstance(null);
	    List<Company> companies = m_mongo.getCompanies();
	    ShowCompanies(companies, out);
	    out.println(m_addCompanyForm);
	}
	else {
	    String db = request.getParameter("db");
	    Boolean unit_mode = false;
	    if (db != null && db.equals("unit")) {
		response.setContentType("text/plain");
		unit_mode = true;
	    }	    
	    m_mongo = MongoInterface.getInstance(db);
	    if (query.contains("action")) {
		String action = request.getParameter("action");
		if (action.equals("delete")) {
		    m_mongo.RemoveCompany(request.getParameter("delete"));
		}
		else if (action.equals("activate")) {
		    String companyid = request.getParameter("activate");
		    String phone = request.getParameter("phone");
		    m_mongo.ActivateCompany(phone, companyid);
		}
		if (unit_mode == false) {
		    out.println("<br><br><a href=\"" + request.getRequestURI() + "\">Return</a>");
		}
	    }	
	    else  {
		/*
		out.println("<h1>New Company:</h1>");
		out.println("<h2>Name: " + request.getParameter("name") + "</h2>");
		out.println("<h2>email: " + request.getParameter("email") + "</h2>");
		out.println("<h2>Phone: " + request.getParameter("phone") + "</h2>");
		out.println("<br><br><a href=\"" + request.getRequestURI() + "\">Return</a>");
		*/
		String name = request.getParameter("name");
		String email = request.getParameter("email");
		String phone = request.getParameter("phone");
		String sid = request.getParameter("sid");
		String token = request.getParameter("token");
		String id = m_mongo.AddCompany(name, email, phone, sid, token);
		if (unit_mode == true) {
		    out.println(id);
		}
		else {
		    out.println("<br><br><a href=\"" + request.getRequestURI() + "\">Return</a>");
		    String company_uri = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Admin?companyid=" + id;
		    String subject = "Your new SMS message service";
		    String email_body = "Congradulations " + name + ", your company can now receive text message using the following number:\n\n" + phone +
			"\n\nPlease use the following link " + company_uri + " to administer this service";
		    EmailSender.SendEmail(subject, email_body, email);
		}
	    }
	}
    }
    
}
