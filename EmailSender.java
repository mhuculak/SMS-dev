import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class EmailSender {
    final static String m_from = "smsdev@yahoo.ca";
    final static String m_password = "welcome1234";
    final static String m_smtp_server = "smtp.mail.yahoo.com";
    
    public static void SendEmail(String subject, String body, String to) {
	Properties props = new Properties();	
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", m_smtp_server);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(m_from, m_password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(m_from));
            message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));                    
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

	    //            System.out.println("Mail sent succesfully!");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
