package se.independent.dbclassloader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

import se.independent.dbclassloader.jaxrs.server.DbclREST;
import se.independent.dbclassloader.jaxrs.server.DbclmREST;

@ApplicationPath("/dbcl")
public class DbclApplication extends Application {
	
	 public Set<Class<?>> getClasses() {
        return Collections.emptySet();
    }
	
	 // https://stackoverflow.com/questions/20937362/what-objects-can-i-inject-using-the-context-annotation
	 @Context
	 private Configuration config;
	 
	 public Set<Object> getSingletons() {
		 HashSet<Object> rv = new HashSet<Object>();
		 
		 if (config.getProperty("dbcl.do_not_deploy") == null) {
			 DbclREST dbcl = new DbclREST();
			 dbcl.init(config);
			 
			 rv.add(dbcl);
		 }		 

		 if (config.getProperty("dbcladm.do_not_deploy") == null) {
			 DbclmREST dbclm = new DbclmREST();
			 dbclm.init(config);
		 
			 rv.add(dbclm);
		 }
	      return rv;
	 }
	 
	 public Map<String, Object> getProperties() {
		 Map<String, Object> rv = new HashMap<String,Object>();
		 rv.put("dbcladm.ds", "redis://192.168.1.36:6379/0");
		 return rv;
		 
	 }
	 
}
