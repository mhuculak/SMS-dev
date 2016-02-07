public class Entity {
    private String m_id;
    private String m_parentid;
    private String m_name;
    private String m_companyid;
    private String m_type;
    private String m_email;
    private String m_state;

    public Entity(String id, String parentid, String name, String companyid, String type, String email, String state) {
	m_id = id;
	m_parentid = parentid;
	m_name = name;
	m_companyid = companyid;
	m_type = type;
	m_email = email;
	m_state = state;
    }

    public String getID() {
	return m_id;
    }

    public String getName() {
	return m_name;
    }

    public String getType() {
	return m_type;
    }

    public String getEmail() {
	return m_email;
    }
}
