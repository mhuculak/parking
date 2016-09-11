package parking.util;



import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.Date;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Logger {
	private TheLogger logger;
	private LoggingTag tag;
	private String className;
	private String method;
	

	private final String defaultTagMapsFile = "tagMaps.txt";

	// ctor used by servlet
	public Logger(HttpSession session, LoggingTag tag, Object obj, String method) {		
		className = obj.getClass().getName();
		init(session, tag, method);
	}

	// ctor used by app
	public Logger(File tagMapFile, LoggingTag tag, Object obj) {		
		className = obj.getClass().getName();
		init(tagMapFile, tag, null);
	}

	// ctor used within a class
	public Logger(Logger parent, Object obj, LoggingTag tag) {
		this.logger = parent.getLogger();
		className = obj.getClass().getName();
		this.tag = tag;
	}

	// ctor used within method
	public Logger(Logger parent, LoggingTag tag, String method) {
		this.logger = parent.getLogger();
		this.className = parent.getClassName();
		this.tag = tag;
		this.method = method;
	}

	// ctor used within method
	public Logger(Logger parent, String method) {
		this.logger = parent.getLogger();
		this.className = parent.getClassName();
		this.tag = parent.getTag();
		this.method = method;
	}

	// ctor used within static method

	public Logger(Logger parent, LoggingTag tag, String className, String method) {
		this.logger = parent.getLogger();
		this.className = className;
		this.tag = tag;
	}

	public Logger(LoggingTag tag, String className, String method) {
		this.logger = TheLogger.getInstance();
		this.className = className;
		this.tag = tag;
		this.method = method;
	}

	// ctor used by static method with access to session
	public Logger(HttpSession session, LoggingTag tag, String className, String method) {		
		this.className = className;
		init(session, tag, method);
	}

	private void init(HttpSession session, LoggingTag tag, String method) {
		logger = TheLogger.getInstance(session);		
		this.tag = tag;
		this.method = method;
	}

	private void init(File file, LoggingTag tag, String method) {
		logger = TheLogger.getInstance(file);
		this.tag = tag;
		this.method = method;
	}

	public TheLogger getLogger() {
		return logger;
	}

	public String getClassName() {
		return className;
	}

	public LoggingTag getTag() {
		return tag;
	}

	public void log(String content) {
		logger.log(tag, null, className, method, content);
	}

	public void log(String content, String user) {
		logger.log(tag, user, className, method, content);
	}

	public void error(String error) {
		logger.error(null, className, method, error);
	}

	public void error(String error, String user) {
		logger.error(user, className, method, error);
	}

	public void logProfile(Profiler profiler) {
		Map<String, Long> profile = profiler.profile();
		for (String key : profile.keySet()) {
			log(key+" "+profile.get(key)+" msec");
		}
	}

	public void enable(LoggingTag tag) {
		logger.enable(tag);
	}
}

class TheLogger {	
	
	private static TheLogger m_instance;
	private static Map<LoggingTag, Boolean> tagMap;
	private static final String tagMapFile = "conf/tagMaps.txt";
	private static List<LoggingTag> tagList;
	private static PrintWriter logWriter;
	private static boolean forWeb;

	static {
		tagMap = new HashMap<LoggingTag, Boolean>();
		tagList = new ArrayList<LoggingTag>(EnumSet.allOf(LoggingTag.class));                 
		for ( LoggingTag tag : tagList) {
			tagMap.put(tag, false);
		}
	}

	public TheLogger(HttpSession session) {
		readTagMapsFromSession(session, tagMapFile);
	}

	public TheLogger(File file) {
		readTagMapsFromFile(file);
	}

	public static TheLogger getInstance(HttpSession session) {
		if (m_instance == null) {
	    	m_instance = new TheLogger(session);
		}
		return m_instance;
	} 

	public static TheLogger getInstance(File file) {
		if (m_instance == null) {
	    	m_instance = new TheLogger(file);
		}
		return m_instance;
	}

	public static TheLogger getInstance() {
		return m_instance;
	}

	public static void log(LoggingTag tag, String user, String className, String method, String entry) {
		if (tagMap.get(tag)) {
			Date tStamp = new Date();
			String logLine = null;
			if (method == null) {
				method = "";
			}
			if (user == null) {
				user = "";
			}
			if (forWeb) {
				logLine = tStamp+"|LOG|"+Thread.currentThread()+"|"+user+"|"+className+"|"+method+"|"+tag+"|"+entry;
			}
			else {
				logLine = entry;
			}
			
			if (logWriter == null) {
				System.out.println(logLine);
			}
			else {
				logWriter.println(logLine);
			}
		}
	}

	public static void error(String user, String className, String method, String entry) {
		Date tStamp = new Date();
		String errorLine = null;
		if (forWeb) {
			if (user == null) {
		 		errorLine = tStamp+"|ERROR|"+Thread.currentThread()+"||"+className+"|"+method+"|"+entry;
			}
			else {
				errorLine = tStamp+"|ERROR|"+Thread.currentThread()+"|"+user+"|"+className+"|"+method+"|"+entry;
			}
		}
		else {
			errorLine = "ERROR: "+entry;
		}
		if (logWriter == null) {
			System.out.println(errorLine);
		}
		else {
			logWriter.println(errorLine);
		}
	}

	public static boolean setLogFile(String path) {
		try {
			logWriter = new PrintWriter(path, "UTF-8");
		}
		catch (Exception ex) {
			System.out.println("Caught "+ex+" tring to set Logger file to "+path);
			if (!(ex instanceof FileNotFoundException)) {
				ex.printStackTrace();
			}
		}
		if (logWriter == null) {
		 	return false;
		}
		return true;
	}

	private void readTagMapsFromSession(HttpSession session, String resource) {
		System.out.println("read "+resource+" from "+session.getServletContext().getContextPath());
		try {
			InputStream inputStream = session.getServletContext().getResourceAsStream(resource);
			readTagMapsFromStream(inputStream);
			forWeb = true;
		}
		catch (Exception ex) {
			System.out.println("Caught "+ex+" trying to read "+resource);
			if (!(ex instanceof FileNotFoundException)) {
				ex.printStackTrace();
			}
		}
	}

	public static boolean enable(LoggingTag tag) {
		if (tagMap.get(tag) != null) {
			tagMap.put(tag, true);
			return true;
		}
		return false;
	}

	public static boolean disable(LoggingTag tag) {
		if (tagMap.get(tag) != null) {
			tagMap.put(tag, false);
			return true;
		}
		return false;
	}

	private void readTagMapsFromFile(File file) {
		System.out.println("read "+file.getAbsolutePath());
		try {
			InputStream inputStream = new FileInputStream(file);
			readTagMapsFromStream(inputStream);
			forWeb = false;
		}
		catch (Exception ex) {
			System.out.println("Caught "+ex+" trying to read "+file);
			if (!(ex instanceof FileNotFoundException)) {
				ex.printStackTrace();
			}
		}
	}

	private void readTagMapsFromStream(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = reader.readLine()) != null) {
//			System.out.println(line);
            String[] stuff = line.split("\\s+"); 
            if (stuff.length == 2) {
            	LoggingTag tag = LoggingTag.valueOf(stuff[0]);           	
            	boolean value = Boolean.parseBoolean(stuff[1]);
//            	System.out.println("Got tag "+tag+" from tagMaps value = "+value);
            	tagMap.put( tag, value);
            }
            else {
            	System.out.println("Skipping line "+line+" because len = "+stuff.length);
            }
        }
	}

}