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
	                                     "Phone: <input type=\"text\" name=\"phone\" value=\"14387949914\"<br>" +
	                                     "<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();

	m_mongo = MongoInterface.getInstance();
	
	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	if (query == null) {
	    m_mongo.ShowCompanies(out);
	    out.println(m_addCompanyForm);
	}
	else if (query.contains("delete")) {
	    m_mongo.RemoveCompany(request.getParameter("delete"));
	    out.println("<br><br><a href=\"" + request.getRequestURI() + "\">Return</a>");
	}
	else  {
	    
	    out.println("<h1>New Company:</h1>");
	    out.println("<h2>Name: " + request.getParameter("name") + "</h2>");
	    out.println("<h2>email: " + request.getParameter("email") + "</h2>");
	    out.println("<h2>Phone: " + request.getParameter("phone") + "</h2>");
	    out.println("<br><br><a href=\"" + request.getRequestURI() + "\">Return</a>");
	   
	    String name = request.getParameter("name");
	    String email = request.getParameter("email");
	    String phone = request.getParameter("phone");
	    String id = m_mongo.AddCompany(name, email, phone);
	    String company_uri = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Admin?companyid=" + id;
	    String subject = "Your new SMS message service";
	    String email_body = "Congradulations " + name + ", your company can now receive text message using the following number:\n\n" + phone +
			     "\n\nPlease use the following link " + company_uri + " to administer this service";
	    EmailSender.SendEmail(subject, email_body, email);
	}
    }
    
}
