
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
 
import java.util.ArrayList;
import java.util.List;
 
public class Sample {
 
  // Find your Account Sid and Token at twilio.com/user/account

  public static final String ACCOUNT_SID = "ACe33e12e73a0028063395a5eb8d30cc26";
  public static final String AUTH_TOKEN = "8da21493e68ff088f7f27994583549e0";
 
  public static void main(String[] args) throws TwilioRestException {
    TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
 
    // Build a filter for the MessageList
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("Body", "this is a test message"));
    params.add(new BasicNameValuePair("To", "+15147141316")); // my cell number
    params.add(new BasicNameValuePair("From", "+14387949914")); // my Twilo number
 
    MessageFactory messageFactory = client.getAccount().getMessageFactory();
    Message message = messageFactory.create(params); // this sends a text to my cellphone
    System.out.println(message.getSid());
  }
}
