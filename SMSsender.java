import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
 
import java.util.ArrayList;
import java.util.List;

public class SMSsender {
    private SMSmessage m_sms_message = null;
    
    private static final String ACCOUNT_SID = "ACe33e12e73a0028063395a5eb8d30cc26";  // FIXME: need to come from database
    private static final String AUTH_TOKEN = "8da21493e68ff088f7f27994583549e0";

    public SMSsender(SMSmessage sms_message) {
	m_sms_message = sms_message;
    }

    public String sendMessage() {
	TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
	List<NameValuePair> params = new ArrayList<NameValuePair>();
	params.add(new BasicNameValuePair("Body", m_sms_message.getContent()));
	params.add(new BasicNameValuePair("To", m_sms_message.getTo()));
	params.add(new BasicNameValuePair("From",m_sms_message.getFrom() ));
	
	try {
	    MessageFactory messageFactory = client.getAccount().getMessageFactory();
	    Message message = messageFactory.create(params);
	    return message.getSid();
	}
	catch (TwilioRestException e) {
	    System.out.println(e.getErrorMessage());
	    return null;
	}
    }
}
