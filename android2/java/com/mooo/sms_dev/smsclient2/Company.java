package com.mooo.sms_dev.smsclient2;

import java.io.Serializable;
/**
 * Created by admin on 2016-02-20.
 */
public class Company implements Serializable {
    private String m_company_name;
    private String m_phone_number;

    public Company(String line) {
        String[] name_phone = line.split(":");
        m_phone_number = name_phone[0];
        m_company_name = name_phone[1];
    }
    public Company(String name, String phone) {
        m_company_name =name;
        m_phone_number = phone;
    }

    public String getName() {
        return m_company_name;
    }

    public void setName(String company_name) {
        m_company_name = company_name;
    }

    public String getPhone() {
        return m_phone_number;
    }

    public void setPhone(String phone_number) {
        m_phone_number = phone_number;
    }

    public String toString() {
        return m_phone_number + ":" + m_company_name;
    }
}
