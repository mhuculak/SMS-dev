import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBList;

enum Command { HELP, GO, LANG };

class RouterCommand {
    private Command m_command;
    private String m_target;

    public RouterCommand(Command command, String target) {
	m_command = command;
	m_target = target;
    }

    public Command getCommand() {
	return m_command;
    }

    public String getTarget() {
	return m_target;
    }
}

class RouterResult {
    private CustomerStatus m_status;
    private String m_entityid;
    private String m_auto_response;
    
    public RouterResult(CustomerStatus status, String entityid, String auto_response) {
	m_status = status;
	m_entityid = entityid;
	m_auto_response = auto_response;
    }

    public RouterResult(CustomerStatus status, String auto_response) {
	m_status = status;
	m_entityid = null;
	m_auto_response = auto_response;
    }
    
    public CustomerStatus getStatus() {
	return m_status;
    }
    
    public String getDestination() {
	return m_entityid;
    }
    
    public String getAutoResponse() {
	return m_auto_response;
    }

    public String toString() {
	switch(m_status) {
	case PENDING_ROUTING:
	    return "status = " + m_status.toString() + " routing question = " + m_auto_response;
	case ROUTED:
	    return "status = " + m_status.toString() + " routed to = " + m_entityid;
	case WAITING:
	    return "status = " + m_status.toString() + " auto response = " + m_auto_response;
	default:
	    return "status " + m_status.toString();
	}
    }    
}

class Match {
    private String m_name;
    private String m_entityid;
    private int m_score;

    public Match(String entityid, String name, int score) {
	m_entityid = entityid;
	m_name = name;
	m_score = score;
    }

    public String getName() {
	return m_name;
    }

    public String getEntityID() {
	return m_entityid;
    }

    public int getScore() {
	return m_score;
    }
}

public class SMSrouter {
    
    private MongoInterface m_mongo;
    private String m_companyid;
    private String m_customerid;

    private final long m_idle_timeout_sec = 3600; // 1 hour
    private final int m_acceptance_threshold = 90;
    private final int m_confirmation_threshold = 50;
    
    public SMSrouter(String companyid, String customerid) {
	m_companyid = companyid;
	m_customerid = customerid;
	m_mongo = MongoInterface.getInstance();
    }

    private String createRoutingQuestion(List<String> available) {
	StringBuilder sb = new StringBuilder(100);
	sb.append("Which of the following would you like to answer your question?\n");
	int i;
	for ( i=0 ; i<available.size() ; i++) {
	    String name = m_mongo.getEntityName(available.get(i));
	    sb.append("\n" + name);
	}
	return sb.toString();
    }

    private int doMatch(String s1, String s2) {
	char[] a1 = s1.toCharArray();
	char[] a2 = s2.toCharArray();
	int min = s1.length() > s2.length() ? s2.length() : s1.length();
	int max = s1.length() > s2.length() ? s1.length() : s2.length();
	int extra = max-min; // added to score to account for difference in size
	int i;
	int score = 0;
	for ( i=0 ; i<min ; i++ ) {
	    if (Character.toLowerCase(a1[i]) != Character.toLowerCase(a2[i])) { // case independant
		score++;
	    }
	}
	return score+extra;
	
    }
    private Match parseAnswer(List<String> choices, String answer) {
	int i;
	String best_id = null;
	int best_score = 0;
	String best_answer = null; 
	for ( i=0 ; i<choices.size() ; i++) {
	    String eid = choices.get(i);
	    String name = m_mongo.getEntityName(eid);
	    int match = doMatch(answer, name);
	    int score = match2score(name, match);
	    if (score > best_score) {
		best_score = score;
		best_id = eid;
		best_answer = name;
	    }
	}
	if (best_id != null) {
	    return new Match(best_id, best_answer, best_score);
	}
	return null;
    }
    /*
          longest idle
     */    
    private RouterResult getPoolSelection(String pool_entityid) {
	String selection = null;
	String auto_response = "";
	String pool_name = m_mongo.getEntityName(pool_entityid);
	CustomerStatus status = CustomerStatus.ROUTED;
	
	Map<String, String> idle_time = m_mongo.getPoolIdleTime(m_companyid, pool_entityid);
	if (idle_time == null || idle_time.size() == 0) {
	    status = CustomerStatus.WAITING;
	    auto_response = "There are no available agents from " + pool_name + " at the moment";
	}
	else {
	    Set poolids = idle_time.entrySet();
	    Iterator itr = poolids.iterator();
	    int maxidle_sec = 0; // about 1 day
	    System.out.println("found " + idle_time.size() + " available employees in pool " + pool_name);
	    while(itr.hasNext()) {
		Map.Entry me = (Map.Entry)itr.next();
		System.out.println("Found map entry " + me.getKey().toString() + " = " + me.getValue().toString());
		int idle_msec = Integer.parseInt(me.getValue().toString());
		int idle_sec = idle_msec/1000;
		String employee_entityid = me.getKey().toString();
		System.out.println("employee " + employee_entityid + " has been idle for " + idle_sec + " sec ");
		if (maxidle_sec <= idle_sec) {
		    maxidle_sec = idle_sec;
		    selection = employee_entityid;
		}
	    }
	}
	RouterResult rr = new RouterResult(status, selection, auto_response);
	return rr;
    }
    /*
       commands                    meaning

       #                           display HELP message
       #help                       display HELP
       #go target                  GO to target
       #go nomatch                 GO to toplevel
       #lang target                set LANG to target

       FIXME1: better to use regex but java regex sucks so good luck with that
       FIXME2: add a mechanism for confirmation

     */
    private RouterCommand parseCommand(String content) {
	char[] carr = content.toCharArray();
	int len = content.length();
	int i=0;
	while(carr[i] != '#' && i<len) {
	    i++;
	}
	if ( i>= len-1) {
	    return null; // no command found
	}
	String tail = content.substring(i+1);
	String[] words = tail.split(" ");
	String cmd_found = null;
	String target = null;
	if (words.length > 0) {
	    cmd_found = words[0];
	    System.out.println("parseCommand got cmd = " + cmd_found);
	}
	if (words.length > 1) {
	    target = words[1];
	    System.out.println("parseCommand got target = " + target);
	}

	if (cmd_found != null) {
	    int best_score = 0;
	    Command best_cmd = null;
	    for (Command c : Command.values()) {
		String cmd = c.toString();
		int match = doMatch(cmd, cmd_found);
		int score = match2score(cmd, match);
		if ( score > best_score) {
		    best_score = score;
		    best_cmd = c;
		}
	    }
	    if (best_cmd != null && best_score > m_acceptance_threshold) {
		return new RouterCommand(best_cmd, target);
	    }
	    
	}
	
	return null;
    }
    
    private RouterResult ProcessRouterCommands(String content) {
	RouterCommand router_command = parseCommand(content);
	String auto_response = "";
	CustomerStatus status = CustomerStatus.UNKNOWN;
	String route = null;
	if (router_command != null) {
	    Command command = router_command.getCommand();
	    switch(command) {
	    case HELP:
		System.out.println("Customer wants HELP");
		auto_response = "Commands:\n#help ... displayes this message\n" +
		    "#go target ... route your message to target\n" +
		    "#list ... list available targets\n" +
		    "#lang LANG ... sets language support to LANG\n";
		break;
	    case GO:
		System.out.println("Customer wants to GO " + router_command.getTarget());
		{
		    List<String> available = m_mongo.FindAvailableEntities(m_companyid);
		    Match best = parseAnswer(available, router_command.getTarget());
		    if ( best != null) {
			if (best.getScore() > m_acceptance_threshold) {
			    route = best.getEntityID();
			    status = CustomerStatus.ROUTED;
			    m_mongo.RouteCustomerToEntity(route, m_customerid);
			    break;
			}
			else if (best.getScore() > m_confirmation_threshold) {
			    auto_response = "Do you wish to go to " + best.getName() + "? (y/n)";
			    status = CustomerStatus.PENDING_ROUTING;
			    break;
			}
		    }
		    if (doMatch(router_command.getTarget(), "top") == 0) { 
			m_mongo.removeCustomerRoute(m_customerid);
		    }
		}
		break;
	    case LANG:
		System.out.println("Customer wants LANG " + router_command.getTarget());
		status = CustomerStatus.UNKNOWN;
		break;
	    }
	    return new RouterResult(status, route, auto_response);
	}
	return null;
    }
    /*
         Convert match (number of mismatched letters) to score (0 to 100)
     */
    private int match2score(String keyword, int match) {
	if (keyword == null || keyword.length() == 0) {
	    return 0; // worst score
	}
	Double fmatch = 100.0*(float)match/keyword.length();
	return 100 - fmatch.intValue(); // 100 corresponds to zero mismatched letters
    }
    
    private Match getBestKeywordMatch(String entityid, BasicDBList keywords, String[] words) {
        int best_score = 0;
	String best_keyword = null;
	for (Object kwdobj : keywords) {
	    String keyword = kwdobj.toString();
	    int i;
	    for ( i=0 ; i<words.length ; i++) {
		String word = words[i];
		int match = doMatch(keyword, word);
		int score = match2score(keyword, match);
		if (score > best_score) {
		    best_score = score;
		    best_keyword = keyword;
		}
	    }
	}
	if (best_keyword != null) {	    
	    return new Match(entityid, best_keyword, best_score);
	}
	return null;
    }

    private String getConfirmation(String keyword) {
	return "Do you want to want someone with knowledge of " + keyword + "? (yes/no)";
    }
    
    private RouterResult FindKeywordResult(String content) {
	String[] words = content.split(" ");
	Map<String, BasicDBList> available_keywords = m_mongo.getAvailableKeywords(m_companyid);
	Set available = available_keywords.entrySet();
	Iterator itr = available.iterator();
	Match global_best = null;
	while(itr.hasNext()) {
	    Map.Entry me = (Map.Entry)itr.next();
	    String employee_entityid = me.getKey().toString();
	    BasicDBList keywords = (BasicDBList)me.getValue();
	    System.out.println("FindKeywordResult: entity " + employee_entityid + " has " + keywords.size() + " keywords"); 
	    Match employee_best = getBestKeywordMatch(employee_entityid, keywords, words);
	    if (global_best == null || global_best.getScore() < employee_best.getScore()) {
		global_best = employee_best;
	    }
	}
	if (global_best != null) {
	    if (global_best.getScore() > m_acceptance_threshold) {
		m_mongo.RouteCustomerToEntity(global_best.getEntityID(), m_customerid);
		return new RouterResult(CustomerStatus.ROUTED, global_best.getEntityID(), "");
	    }
	    else if (global_best.getScore() > m_confirmation_threshold) {
		String confirmation = getConfirmation(global_best.getName());
		return new RouterResult(CustomerStatus.PENDING_ROUTING, global_best.getEntityID(), confirmation);
	    }
	}
	return null;
    }

    private RouterResult ProcessNewRoute(String route, CustomerStatus status) {
	String entity_type = m_mongo.getEntityType(route);
	if (entity_type.equals("employee")) {
	    System.out.println("routing customer " + m_customerid + " to employee " + route);
	    m_mongo.RouteCustomerToEntity(route, m_customerid);
	    return new RouterResult(CustomerStatus.ROUTED, route, "");
	}
	else if (entity_type.equals("pool")) { // if our route is a pool then pick someone from the pool
	    System.out.println("get selection for " + m_customerid + " from pool " + route);
	    RouterResult pool_selection = getPoolSelection(route);
	    if (pool_selection.getStatus() == CustomerStatus.ROUTED) {
		System.out.println("routing customer " + m_customerid + " to employee " + route);
		m_mongo.RouteCustomerToEntity(pool_selection.getDestination(), m_customerid);
	    }
	    return pool_selection;
	}
	return null;
    }
    
    // Find an employee that can respond to text message sent to company phone number
    private RouterResult FindRoute(String answer, CustomerStatus customer_status) {
	RouterResult command_result = ProcessRouterCommands(answer);
	if (command_result != null) {
	    System.out.println("FindRoute returning #command, " + command_result.toString());
	    return command_result;
	}
	/*
           FIXME: keyword routing implemented this way takes precedence over previous routing
                  for example if a customer is routed to rep1 based on a keyword and in the
                  process of chating with rep1 there is a keyword match with rep2...do we want to stay with
                  rep1 or go to rep2 ?
	 */
	RouterResult keyword_result = FindKeywordResult(answer);
	if (keyword_result != null) {
	    System.out.println("FindRoute returning keyword result, " + keyword_result.toString());
	    return keyword_result;
	}
	List<String> available = m_mongo.FindAvailableEntities(m_companyid);
	if (available == null || available.size() == 0) {
	    return new RouterResult(CustomerStatus.WAITING, "There are no available agents at the moment");
	}
	switch(customer_status) {
	case UNKNOWN:
	    if (available.size() == 1) {
		System.out.println("selecting only available route " + available.get(0) + " with status = " + customer_status);
		return new RouterResult(CustomerStatus.ROUTED, available.get(0), "");
	    }
	    else if (available.size() > 1) {
		System.out.println("create routing question with status = " + customer_status);
		return new RouterResult(CustomerStatus.PENDING_ROUTING, createRoutingQuestion(available));
	    }
	    break;
	case PENDING_ROUTING:
	    if (available.size() == 1) {
		System.out.println("selecting only available route " + available.get(0) + " with status = " + customer_status);
		return new RouterResult(CustomerStatus.ROUTED, available.get(0), "");
	    }
	    else if (available.size() > 1) {
		Match best = parseAnswer(available, answer);
		if (best != null) {
		    if (best.getScore() > m_acceptance_threshold) {
			System.out.println("selecting best available route " + best.getEntityID() + " with status = " + customer_status);
			return new RouterResult(CustomerStatus.ROUTED, best.getEntityID(), "");
		    }
		    else if (best.getScore() > m_confirmation_threshold) {
			String auto_response = "Do you wish to go to " + best.getName() + "? (y/n)";
			return new RouterResult(CustomerStatus.PENDING_ROUTING, auto_response);
		    }
		}
		else {
		    System.out.println("selecting first route because no best route found with status = " + customer_status);
		    return new RouterResult(CustomerStatus.ROUTED, available.get(0), "");
		}
	    }
	    break;
	case ROUTED:
	    System.out.println("selecting existing route with status = " + customer_status);
	    return new RouterResult(CustomerStatus.ROUTED, m_mongo.getCustomerRoute(m_customerid), "");
	default:
	    System.out.println("doing nothing with status = " + customer_status);
	}

	return null;
    }

    public RouterResult doRoute(String messageid, String content, CustomerStatus customer_status) {
	/*
            If we do not call setCustomerMessage(), the message will not get routed.
            In the case of a customer in state PENDING_ROUTING, the router is asking
            questions to determine where to route. Therfore we do not route the answers
            as they are intended for the router rather than the business rep
	 */
	if (customer_status != CustomerStatus.PENDING_ROUTING) {
	    m_mongo.setCustomerMessage(m_customerid, messageid);
	}
	/*
            de-route any customers that have been idle too long and treat the message as part of a new request rather than
            as part of a current chat
         */
	SMSmessage new_message = m_mongo.getMessage(messageid);
	String customer_phone = new_message.getCustomer();
	SMSmessage previous_message = m_mongo.getPreviousMessage(customer_phone, messageid);
	if (previous_message != null) {
	    long idle_msec = SMSmessage.getTimeDifferenceMsec(previous_message, new_message);
	    long idle_sec = idle_msec/1000;
	    System.out.println("idle time is " + idle_sec + " sec");	
	    if (idle_sec > m_idle_timeout_sec) {
		customer_status = CustomerStatus.UNKNOWN;
	    }
	}

	RouterResult router_result = FindRoute(content, customer_status);

	if (router_result != null) {
	    customer_status = router_result.getStatus();
	
	    if (customer_status == CustomerStatus.WAITING) { // nothing available
		m_mongo.addCustomerToWaitingQueue(m_companyid, m_customerid);
	    }
	    else if (customer_status == CustomerStatus.ROUTED) {
		router_result = ProcessNewRoute(router_result.getDestination(), customer_status);	    
	    }
	}
	
	if (router_result != null) {
	    m_mongo.setCustomerStatus(m_customerid, router_result.getStatus());
	}
	return router_result;
    }
}

