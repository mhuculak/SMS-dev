import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import junit.framework.*;
import static org.junit.Assert.*;

public class FixmeTest1 {
    
    private static String m_companyid;
    private static TestEntity m_simon;
    private static TestEntity m_pieman;
    private static String m_company_phone;
    private static String m_company_name;
    
    @BeforeClass
    public static void companyConfig() {
	m_company_phone = "5141110001";
	m_company_name = "Simple Simon's Fair";
	m_companyid = TestUtils.AddCompany(m_company_name, m_company_phone);
	m_simon = new TestEntity(m_companyid, null, "Simon", "employee");
	m_pieman = new TestEntity(m_companyid, null, "Pieman", "employee");
    }

    @Test
    public void customer2Message1() {
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_message1 = "hello to Simon"; // both avail...must chose
	String customer2_phone = "5145550002";
	String expected_response = "Which of the following would you like to answer your question?"
	    +m_simon.getName()+m_pieman.getName(); 
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_message1);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // asking to choose
	response = TestUtils.sendMessage(customer2_phone, m_company_phone, m_simon.getName() ); // simon selected
	assertEquals(customer2_message1, m_simon.GetMessageRoutedTo(customer2_phone)); // msg routed to simon
    }
 }
