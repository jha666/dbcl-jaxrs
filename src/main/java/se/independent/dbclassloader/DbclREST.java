package se.independent.dbclassloader;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 */

@Path("/dbcl")
public class DbclREST  {

	private Log log = new Log(DbclREST.class, Level.FINE, this);


	private static final String DBCL_URL = /*"java:/jdbc/dbcl-ds"; //*/ "jdbc/dbcl-ds";
		
	private static Map<String, DbClassLoader> dictionary = new HashMap<String, DbClassLoader>();

	public DbclREST() {	
		super();
	
		log.info("> DbclREST()");
		if (dictionary.get("default") == null) {
			DbClassLoader dbcl = new DataSourceDbClassLoader();
			dbcl.connect(DBCL_URL, new Properties());
			dbcl.prepare();
			dictionary.put("default", dbcl);
		}
		log.info("< DbclREST()");
	}


	// =======================================================================
	// ping
	//------------------------------------------------------------------------
	@GET
	@Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
	public Response ping(@Context HttpServletRequest req) {
		log.debug("> ping()");
		StringBuilder sb = new StringBuilder();
		sb.append("rest=").append(req.getScheme()).append("://").append(req.getServerName())
		.append(":").append(req.getServerPort())
		.append(req.getServletContext().getContextPath())
		.append(req.getServletPath()).append("\n");
		sb.append("timestamp").append("=").append(ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )).append("\n");	
		sb.append("jndi").append("=").append(DBCL_URL).append("\n");
		sb.append("java.vendor").append("=").append(System.getProperty("java.vendor")).append("\n");
		sb.append("java.version").append("=").append(System.getProperty("java.version")).append("\n");
		sb.append("os.arch").append("=").append(System.getProperty("os.arch")).append("\n");
		sb.append("os.name").append("=").append(System.getProperty("os.name")).append("\n");
		Response rv = Response.ok(sb.toString()).build() ; 
		log.debug("< ping() = [" + rv.getStatus() +"]");
		return rv;
    }

	// =======================================================================
	// 
	//------------------------------------------------------------------------
	@POST
	@Path("/{dbcl}/")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
	public Response insert(@PathParam("dbcl") String name, 
			@FormParam("uri") String uri, 
			@FormParam("username") String username, 
			@FormParam("passwd") String passwd) {
		log.info("> insert(" + name + ")");
		DbClassLoader dbcl = dictionary.get(name);
		if (dbcl == null) {
			String[] URL = uri.split(":");
		     
		    switch (URL[0]) {
		    case "jdbc":
		    	dbcl = new JDBCDbClassLoader();
		    	break;
		    	
		    case "redis":
		    	dbcl = new JedisDbClassLoader();
		    	break;
		    	
		    case "bolt":
		    	dbcl = new Neo4jDbClassLoader();
		    	break;
		    	
		    default:
				log.info("< insert()");
				return Response.status(400).entity(uri).build();
		    }
		    
		    Properties p = new Properties();
		    p.setProperty("user", username); 
		    p.setProperty("password", passwd);		
			dbcl.connect(uri, p);
			dbcl.prepare();
			
			dictionary.put(name, dbcl);
		}
		log.info("< insert()");
		return Response.status(200).entity(Integer.toHexString(dbcl.hashCode())).build();
    }
	
			   
	public static boolean isReachable(String addr, int openPort, int timeOutMillis) {
	    // Any Open port on other machine
	    // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
		
	    try {
	        try (Socket soc = new Socket()) {
	            soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
	        }
	        return true;
	    } catch (IOException ex) {
	        return false;
	    }
	}


	@DELETE
	@Path("/{dbcl}/")
    @Produces(MediaType.TEXT_PLAIN)
	public Response delete(@PathParam("dbcl") String name) {
		log.info("> delete(" + name + ")");
		if ("default".equals(name)) {
			return Response.status(200).entity(0).build();
		}
		
		DbClassLoader dbcl = dictionary.remove(name);
		if (dbcl != null) {
			dbcl.close();
		}
		
		log.info("< delete()");
		return Response.status(200).entity(dbcl == null ? "-1" : Integer.toHexString(dbcl.hashCode())).build();
	}

	@GET
	@Path("/{dbcl}/")
    @Produces(MediaType.TEXT_PLAIN)
	public Response read(@PathParam("dbcl") String name, @Context HttpServletRequest req) {
		log.info("> read(" + name + ")");	
		StringBuilder sb = new StringBuilder();
		DbClassLoader dbcl = dictionary.get(name);
		
		sb.append("rest=").append(req.getScheme()).append("://").append(req.getServerName())
		.append(":").append(req.getServerPort())
		.append(req.getServletContext().getContextPath())
		.append(req.getServletPath()).append("\n");
		
		sb.append("dbcl=").append(name).append("\n");
		sb.append("timestamp=").append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)).append("\n");	

		if (dbcl != null) {
			sb.append(dbcl.ping());
		}
		
		log.info("< read()");
		return Response.status(200).entity(sb.toString()) .build();
	}

	
	// =======================================================================
	// class/resources
	//------------------------------------------------------------------------
	@GET
	@Path("/{dbcl}/class/{classpath_name}/{binary_name}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getClass(@PathParam("dbcl") String name,
			@PathParam("classpath_name") String cpn, @PathParam("binary_name") String bn) {
		log.info("> getClass(" + bn +")");
		Response rv;
				
		DbClassLoader dbcl = dictionary.get(name);
		byte[] res = dbcl.findClass(cpn, bn);
		rv = res != null ? Response.ok(res).build() : Response.status(404).build();
		
		log.info("< getClass() = [" + rv.getStatus() +"]");
		return rv;
    }
	

	@GET
	@Path("/{dbcl}/url/{classpath_name}/{name}")
    @Produces(MediaType.TEXT_PLAIN)
	public Response getResources(@PathParam("dbcl") String n, 
			@PathParam("classpath_name") String cpn, @PathParam("name") String name) {
		log.info("> getResources(" + cpn + ", " + name +")");
		Response rv;
		DbClassLoader dbcl = dictionary.get(n);
		List<String> res = dbcl.findResources(cpn, name.trim());
		StringBuilder sb = new StringBuilder();
		for (String pe : res) {
			sb.append(pe.trim());
			sb.append(File.pathSeparator);
		}
		rv = res != null ? Response.status(200).entity(sb.toString()).build() : Response.status(404).build();
		log.info("< getResources() = [" + rv.getStatus() +"]");
		return rv;
    }
	
	@GET
	@Path("/{dbcl}/resource/{path_element}/{name}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getResource(@PathParam("dbcl") String n,
			@PathParam("path_element") String pe, @PathParam("name") String name) {
		log.info("> getResource(" + pe + "," + name +")");
		Response rv = Response.status(404).build();
		DbClassLoader dbcl = dictionary.get(n);
		byte[] res= dbcl.loadResource(pe, name); //dbcl.findResourceAsBytes(pe, name);
		if (res != null) {
			rv = Response.ok(res).build();
		}
		log.info("< getResource() = [" + rv.getStatus() +"]");
		return rv;
    }
	
}
