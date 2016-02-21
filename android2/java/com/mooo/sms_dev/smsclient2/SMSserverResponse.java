package com.mooo.sms_dev.smsclient2;

/**
 * Created by admin on 2016-02-20.
 */

import java.util.*;

public class SMSserverResponse {

    String m_result;                      // either "OK" or and error message
    CustomerStatus m_customer_status;     // same as CustomerStatus used on the server side
    String m_company_name;                // name of company
    String[] m_available;                 // list of available business reps
    String m_confirm_id;                  // id of the route to be confirmed by the client
    String m_waiting_message;             // message sent when nobody is available
    String m_message;                     // the message sent by a customer rep

    /*
        FIXME: better to use an XML formated response
     */
    public SMSserverResponse(HttpClient httpClient) {
        List<String> lines = httpClient.getLines();
        if (lines == null || lines.size() == 0) {
            m_result = "XFER ERROR";
            return;
        }
        m_result = lines.get(0);
        if (!m_result.equals("OK") || lines.size() < 3) {
            return;
        }
        m_customer_status = CustomerStatus.valueOf(lines.get(1));
        m_company_name = lines.get(2);
        switch(m_customer_status) {
            case UNKNOWN:
                break;
            case PENDING_ROUTING:
                int num_available = lines.size() - 3;
                if (num_available > 0) {
                    m_available = new String[num_available];
                    for (int i = 3; i < lines.size(); i++) {
                        int k = i - 3;
                        m_available[k] = lines.get(i);
                    }
                }
                else {
                    m_result = "ERROR: available list is empty in state " + m_customer_status;
                }
                break;
            case CONFIRM_ROUTING:
                if (lines.size()==4) {
                    m_confirm_id = lines.get(3);
                }
                else {
                    m_result = "ERROR:  found " + lines.size() + " in state " + m_customer_status;
                }
                break;
            case ROUTED:
                if (lines.size()>3) {
                    StringBuilder sb = new StringBuilder(100);
                    for ( int i=3 ; i<lines.size() ; i++) {
                        sb.append(lines.get(i));
                    }
                    m_message = sb.toString();
                }
                else {
                    m_result = "ERROR:  found " + lines.size() + " in state " + m_customer_status;
                }
                break;
            case WAITING:
                if (lines.size()==4) {
                    m_waiting_message = lines.get(3);
                }
                else {
                    m_result = "ERROR:  found " + lines.size() + " in state " + m_customer_status;
                }
                break;
            default:

        }
    }

    public String getResult() {
        return m_result;
    }

    public CustomerStatus getCustomerStatus() {
        return m_customer_status;
    }

    public String getCompanyName() {
        return m_company_name;
    }

    public String[] getAvailable() {
        return m_available;
    }

    public String getConfirmID() {
        return m_confirm_id;
    }

    public String getWaitingMessage() {
        return m_waiting_message;
    }

    public String getMessage() {
        return m_message;
    }
}
