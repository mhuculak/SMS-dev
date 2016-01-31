import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;


public class EmployeeServlet extends HttpServlet {

    private String FrameSet(String companyid, String entityid) {
	String frameset = "<frameset rows=\"80%,20%\"><br>" +
	    "<frame src=\"Chat?companyid=" + companyid + "&entityid=" + entityid + "\" name=\"MessageWin\"><br>" +
	    "<frame src=\"EmployeeInput?companyid=" + companyid + "&entityid=" + entityid + "\" name=\"InputWin\"><br>" +
	    "</frameset>";
	return frameset;
    }
    
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
	String query = request.getQueryString();
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
		    out.println(FrameSet(companyid, entityid));
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
