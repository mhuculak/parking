package parking.util;

import parking.security.WebLogin;
import javax.servlet.http.HttpSession;

public class UserLogger extends Logger {
	private String user;

	public UserLogger(HttpSession session, LoggingTag tag, Object obj, String method) {		
		super(session, tag, obj, method);
		initUser(session);
	}

	public UserLogger(HttpSession session, LoggingTag tag, String className, String method) {		
		super(session, tag, className, method);
		initUser(session);
	}

	public UserLogger(UserLogger parent, Object obj, LoggingTag tag) {
		super( parent, obj, tag);
		this.user = parent.getUser();
	}

	public UserLogger(UserLogger parent, LoggingTag tag, String method) {
		super( parent, tag, method);
		this.user = parent.getUser();
	}

	public UserLogger(UserLogger parent, String method) {
		super(parent, method);
		this.user = parent.getUser();
	}

	public UserLogger(UserLogger parent, LoggingTag tag, String className, String method) {
		super( parent, tag, className, method);
		this.user = parent.getUser();
	}

	private void initUser(HttpSession session) {
		WebLogin login = (WebLogin)session.getAttribute("login-session");
		if (login != null) {
			user = login.getUserName();
		}		
	}

	public String getUser() {
		return user;
	}

	public void log(String content) {
		super.log(content, user);
	}

	public void error(String error) {
		super.error(error, user);
	}

}
