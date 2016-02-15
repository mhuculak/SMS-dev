import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import junit.framework.*;
import static org.junit.Assert.*;

/*
  Simple company with 2 reps

  The unit tests cover the functionality of the SMSrouter. They do not cover 1) OA&M GUI 2) messages
  sent by business rep 3) email. These things still need to be verified manually

  Note: junit does not guarantee the execution order of the tests so they must be independent
 */
public class UserTest1 {
    
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
    
    @AfterClass
    public static void cleanup() {
	TestUtils.WipeDB();
    }

    @Before
    public void clearRoutes() {
	m_simon.setStatusOffline();
	m_pieman.setStatusOffline();	
    }
    /*
        This test verifies that a message is automatically routed to only avialable rep
     */

    @Test
    public void customer1Message1() {
	m_simon.setStatusAvailable();
	m_pieman.setStatusOffline();
	String customer1_message1 = "hello to Simon"; // only Simon is available
	String customer1_phone = "5145550001";
	String expected_response = ""; 
	String response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message1);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // no auto response
	assertEquals(customer1_message1, m_simon.GetMessageRoutedTo(customer1_phone)); // msg routed to simon
    }

    /*
       This test verifies that the next message goes to the other rep who is the only one avialble
     */
    
    @Test
    public void customer1Message2() {
	customer1Message1();
	m_simon.setStatusOffline();
	m_pieman.setStatusAvailable();
	String customer1_message2 = "hello to Pieman"; // only Pieman is available
	String customer1_phone = "5145550001";
	String expected_response = ""; 
	String response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message2);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // no auto response
	assertEquals(customer1_message2, m_pieman.GetMessageRoutedTo(customer1_phone)); // msg routed to pieman
    }
    /*
       This test verifies that a message is sent to the rep the customer is currently routed to
     */
    @Test
    public void customer1Message3() {
	customer1Message2();
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer1_message3 = "how are you Pieman"; // customer1 already routed to pieman
	String customer1_phone = "5145550001";
	String expected_response = ""; 
	String response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message3);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // no auto response
	assertEquals(customer1_message3, m_pieman.GetMessageRoutedTo(customer1_phone)); // msg routed to pieman
    }
    /*
      This test verifies that a customer can chose between two available agents. 
     */

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
    
     /*
      This test verifies that the a confirmation is sent went the customer makes a typo responding to a routing question
     */
   
    @Test
    public void customer2MessageTypo() {
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_message1 = "hello to Simon"; // both avail...must chose
	String customer2_phone = "5145550002";
	String expected_response = "Which of the following would you like to answer your question?"
	    +m_simon.getName()+m_pieman.getName(); 
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_message1);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // asking to choose
	String answer_typo = "Sinom";
	response = TestUtils.sendMessage(customer2_phone, m_company_phone, answer_typo ); // simon selected
	expected_response = "Do you wish to go to " + m_simon.getName() + "? (y/n)";
	assertEquals(expected_response, response); // confirm
	response = TestUtils.sendMessage(customer2_phone, m_company_phone, "y");
	assertEquals(customer2_message1, m_simon.GetMessageRoutedTo(customer2_phone)); // msg routed to simon
    }
   
    /*
      This test verifies the customer can get a help message using the #help command
     */

    @Test
    public void customer2help() {
	customer2Message1();
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_help = "#help"; // help command
	String customer2_phone = "5145550002";
	String expected_response = "Commands:#help ... displayes this message" +
		    "#go target ... route your message to target" +
		    "#list ... list available targets" +
		    "#lang LANG ... sets language support to LANG";
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_help);
	System.out.println("response is " + response);
	assertEquals(expected_response, response);
    }
    /*
      This test verifies the customer can route himself to a rep using the #go command
     */
    
    @Test
    public void customer2goPieman() {
	customer2Message1();
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_go_pieman = "#go Pieman"; // command to re-route to pieman
	String customer2_phone = "5145550002";
	String expected_response = "";
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_go_pieman);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // no response when msg is routed
	assertEquals(customer2_go_pieman, m_pieman.GetMessageRoutedTo(customer2_phone)); // msg routed to pieman
    }
    
    /*
      This test verifies that the a confirmation is sent went the customer makes a typo in the target of a #go command
     */

    @Test
    public void customer2goPiemanTypo() {
	customer2Message1();
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_go_pieman_typo = "#go Pienam"; // command to re-route to pieman
	String customer2_phone = "5145550002";
	String expected_response = "Do you wish to go to " + m_pieman.getName() + "? (y/n)";
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_go_pieman_typo);
	System.out.println("response is " + response); 
	assertEquals(expected_response, response); // confirm
	response = TestUtils.sendMessage(customer2_phone, m_company_phone, "y");	
	assertEquals(customer2_go_pieman_typo, m_pieman.GetMessageRoutedTo(customer2_phone)); // msg routed to pieman
    }
    /*
       This test verifies a message can deroute himself back to the toplevel using the #go top command
     */
    
    @Test    
    public void customer2goTop() {
	customer2Message1();
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer2_go_top = "#go top"; // command to re-route to toplevel
	String customer2_phone = "5145550002";
	String expected_response = "Which of the following would you like to answer your question?"
	    +m_simon.getName()+m_pieman.getName(); // the top level routing question
	String response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_go_top);
	System.out.println("response is " + response);
	assertEquals(expected_response, response); // asking to choose
	response = TestUtils.sendMessage(customer2_phone, m_company_phone, m_simon.getName() ); // simon selected
	assertEquals(customer2_go_top, m_simon.GetMessageRoutedTo(customer2_phone)); // msg routed to simon
    }
    /*
       This test verifies that a busy message is sent when no reps are avialable
     */
    
    @Test 
    public void customer1Message4() {
	m_simon.setStatusOffline();
	m_pieman.setStatusOffline();
	String customer1_message4 = "someone help me here!"; // all reps offline, no possible route
	String customer1_phone = "5145550001";
	String expected_response = "There are no available agents at the moment"; 
	String response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message4);
	System.out.println("response is " + response);
	assertEquals(expected_response, response);
	m_simon.setStatusAvailable(); // simon comes online so msg should go to him
	assertEquals(customer1_message4, m_simon.GetMessageRoutedTo(customer1_phone)); // msg routed to simon 
    }
     /*
       This test verifies that two messages are routed to two different available agents
     */
    @Test
    public void two_customers1() {
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer1_phone = "5145550001";
	String customer2_phone = "5145550002";
	String customer1_message = "I need to chat with someone";
	String customer2_message = "I want to chat too";
	String customer1_response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message);
	String customer2_response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_message);
	String customer1_expected_response = "Which of the following would you like to answer your question?"
	    +m_simon.getName()+m_pieman.getName(); // the top level routing question
	String customer2_expected_response = customer1_expected_response; // should be the same
	assertEquals(customer1_expected_response, customer1_response);
	assertEquals(customer2_expected_response, customer2_response);
	customer1_response = TestUtils.sendMessage(customer1_phone, m_company_phone, m_pieman.getName() ); // pieman selected
	customer2_response = TestUtils.sendMessage(customer2_phone, m_company_phone, m_simon.getName() ); // simon selected
	assertEquals(customer1_message, m_pieman.GetMessageRoutedTo(customer1_phone));
	assertEquals(customer2_message, m_simon.GetMessageRoutedTo(customer2_phone));
    }
     /*
       This test verifies that two messages are routed to the same available agent
     */
    @Test
    public void two_customers2() {
	m_simon.setStatusAvailable();
	m_pieman.setStatusAvailable();
	String customer1_phone = "5145550001";
	String customer2_phone = "5145550002";
	String customer1_message = "I need to chat with someone";
	String customer2_message = "I want to chat too";
	String customer1_response = TestUtils.sendMessage(customer1_phone, m_company_phone, customer1_message);
	String customer2_response = TestUtils.sendMessage(customer2_phone, m_company_phone, customer2_message);
	String customer1_expected_response = "Which of the following would you like to answer your question?"
	    +m_simon.getName()+m_pieman.getName(); // the top level routing question
	String customer2_expected_response = customer1_expected_response; // should be the same
	assertEquals(customer1_expected_response, customer1_response);
	assertEquals(customer2_expected_response, customer2_response);
	customer1_response = TestUtils.sendMessage(customer1_phone, m_company_phone, m_pieman.getName() ); // pieman selected
	customer2_response = TestUtils.sendMessage(customer2_phone, m_company_phone, m_pieman.getName() ); // pieman selected
	assertEquals(customer1_message, m_pieman.GetMessageRoutedTo(customer1_phone));
	assertEquals(customer2_message, m_pieman.GetMessageRoutedTo(customer2_phone));
    }
}

