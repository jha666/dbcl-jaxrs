package se.independent.dbclassloader;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/dbcl")
public class DbclApplication extends Application {
	
	 public Set<Class<?>> getClasses() {
	        Set<Class<?>> s = new HashSet<Class<?>>();
	        s.add(DbclREST.class);
	        return s;
	    }
}
