public class Company {
    private String m_id;
    private String m_name;
    private String m_email;
    private String m_phone;
    private String m_sid;
    private String m_auth;

    public Company(String id, String name, String email, String phone, String sid, String auth) {
	m_id = id;
	m_name = name;
	m_email = email;
	m_phone = phone;
	m_sid = sid;
	m_auth = auth;
    }

    public String getID() {
	return m_id;
    }

    public String getName() {
	return m_name;
    }

    public String getEmail() {
	return m_email;
    }

    public String getPhone() {
	return m_phone;
    }

    public String getSid() {
	return m_sid;
    }

    public String getAuth() {
	return m_auth;
    }
}
