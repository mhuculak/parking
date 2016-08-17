package parking.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
//
//  timer starts in the ctor and stops when stop() is invoked
//
public class Profiler {

	private String className;
	private String method;
	private String id;
	private Profiler parent;
	private List<Profiler> children;
	private long startTime;
	private long endTime;
	private long duration;
	private boolean stopped;
	private final long tomsec = 1000000;

	public Profiler(String method, Profiler parent, Object obj) {
		init(method, parent, obj );
	}

	public void addChild(Profiler child) {
		if (children == null) {
			children = new ArrayList<Profiler>();
		}
		children.add(child);
	}

	private void init(String method, Profiler parent, Object obj) {
		this.parent = parent;
		this.method = method;
		if (obj != null) {
			className = obj.getClass().getName();
			id = className+"."+method;
		}
		else {
			id = method;
		}
		if (parent != null) {
			parent.addChild(this);
		}
		startTime = System.nanoTime();
		stopped = false;
	}

	public void stop() {
		endTime = System.nanoTime();
		duration = (endTime - startTime)/tomsec;
		stopped = true;
	}

	public Long getDuration() {
		if (stopped = false) {
			endTime = System.nanoTime();
			duration = (endTime - startTime)/tomsec;
		}
		return duration;
	}

	public Map<String, Long> profile() {
		Map<String, Long> profile = new HashMap<String, Long>();
		if (method != null) {
			profile.put(id, duration);
		}
		if (children != null) {
			for (Profiler child : children) {
				Map<String, Long> childProfile = child.profile();
				for (String chid : childProfile.keySet()) {
					String cat = id+" "+chid;
					profile.put(cat, childProfile.get(chid));
				}
			}
		}
		return profile;
	}
}