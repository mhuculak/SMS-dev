package com.mooo.sms_dev.smscustomer;

/**
 * Created by admin on 2016-02-16.
 */
import java.util.*;
import java.io.*;
import java.net.*;

enum HttpMethod { GET, POST };

public class HttpClient {
    private URL m_url;                  // url
    private Map<String, String> m_post; // key value pairs to post
    private HttpMethod m_method;        // http method
    private int m_status_code;          // 200, 400, 500 etc...
    private String m_body;
    private List<String> m_lines;
    private HttpURLConnection m_connection;

    private URL createURL(String url) {
        URL Url = null;
        try {
            Url =  new URL(url);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Url;
    }

    public HttpClient(String url) {
        m_url =  createURL(url);
        m_method = HttpMethod.GET; // default
    }

    public HttpClient(HttpMethod method, String url) {
        m_url = createURL(url);
        m_method = method;
    }

    public void setPostData(Map<String, String> post) {
        m_post = post;
    }

    public String getUrl() {
        return m_url.toString();
    }

    public HttpMethod getMethod() {
        return m_method;
    }

    public int getStatusCode() {
        return m_status_code;
    }

    public String getBody() {
        return m_body;
    }

    public List<String> getLines() {
        return m_lines;
    }

    public int connect() {
        try {
            m_connection = (HttpURLConnection) m_url.openConnection();
            m_connection.setRequestMethod(m_method.toString());
            if (m_method == HttpMethod.POST && m_post != null) {
                m_connection.setDoOutput(true);
                PrintWriter out = new PrintWriter(m_connection.getOutputStream());
                Set keys = m_post.entrySet();
                Iterator itr = keys.iterator();
                while(itr.hasNext()) {
                    Map.Entry me = (Map.Entry)itr.next();
                    String key = me.getKey().toString();
                    String value = me.getValue().toString();
                    out.print(key + "=" + URLEncoder.encode(value,"UTF-8") + "&");
                }
                out.close();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(m_connection.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder(100);
            List<String> lines = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                sb.append(line);
                lines.add(line);
            }
            in.close();
            m_lines = lines;
            m_body = sb.toString();
            m_status_code = m_connection.getResponseCode();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
        return m_status_code;
    }

}
