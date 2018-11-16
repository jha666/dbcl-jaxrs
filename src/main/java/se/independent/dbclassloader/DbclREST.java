package se.independent.dbclassloader;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 */

@Path("/dbcl")
public class DbclREST  {

	private Log log = new Log(DbclREST.class, Level.FINE, this);


	private static final String DBCL_JNDI = "jdbc/dbcl-ds";


	private DataSourceDbClassLoader dbcl = null; 
	
		
	public DbclREST() {	
		super();

		log.info("> DbclREST()");
		dbcl = new DataSourceDbClassLoader();
		dbcl.connect(DBCL_JNDI, new Properties());
		dbcl.prepare();
		dbcl.setClasspathName("rest"); // not used
		log.info("< DbclREST()");
	}


	// =======================================================================
	// ping
	//------------------------------------------------------------------------
	@GET
	@Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
	public Response ping() {
		log.debug("> ping()");
		StringBuilder sb = new StringBuilder();
		sb.append("timestamp").append("=").append(System.currentTimeMillis()).append("\n");	
		sb.append("jndi").append("=").append(DBCL_JNDI).append("\n");
		if (dbcl != null) {
			sb.append("dbcl").append("=").append(dbcl.ping()).append("\n");
		}
		sb.append("java.vendor").append("=").append(System.getProperty("java.vendor")).append("\n");
		sb.append("java.version").append("=").append(System.getProperty("java.version")).append("\n");
		sb.append("os.arch").append("=").append(System.getProperty("os.arch")).append("\n");
		sb.append("os.name").append("=").append(System.getProperty("os.name")).append("\n");
		Response rv = Response.ok(sb.toString()).build() ; 
		log.debug("< ping() = [" + rv.getStatus() +"]");
		return rv;
    }

	
	// =======================================================================
	// QueryParam flavour
	//------------------------------------------------------------------------
	@GET
	@Path("/findClass")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response findClass(@QueryParam("binary_name") String bn, @QueryParam("classpath_name") String cp_name) {
		log.debug("> findClass(" + bn +")");
		Response rv;
				
		byte[] res = dbcl.findClassAsBytes(cp_name, bn);
		rv = res != null ? Response.ok(res).build() : Response.status(404).build();
		
		log.debug("< findClass() = [" + rv.getStatus() +"]");
		return rv;
    }
	
	   
	@GET
	@Path("/loadResource")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response loadResource(@QueryParam("name") String name, @QueryParam("path_element") String path_elm) {
		log.debug("> loadResource(" + name + ", " + path_elm +")");
		Response rv = Response.status(404).build();
		byte[] res= dbcl.findResourceAsBytes(path_elm, name);
		if (res != null) {
			rv = Response.ok(res).build();
		}
		log.debug("< loadResource() = [" + rv.getStatus() +"]");
		return rv;
    }


	@GET
	@Path("/findResources")
    @Produces(MediaType.TEXT_PLAIN)
	public Response findResources(@QueryParam("name") String name, @QueryParam("classpath_name") String cp_name) {
		log.debug("> findResources(" + name +")");
		Response rv;
		List<String> res = dbcl.findResourcesAsList(cp_name, name.trim());
		StringBuilder sb = new StringBuilder();
		for (String pe : res) {
			sb.append(pe.trim());
			sb.append(File.pathSeparator);
		}
		rv = res != null ? Response.status(200).entity(sb.toString()).build() : Response.status(404).build();
		log.debug("< findResources() = [" + rv.getStatus() +"]");
		return rv;
    }

	
	
	// =======================================================================
	// PathParam flavour
	//------------------------------------------------------------------------
	@GET
	@Path("/class/{classpath_name}/{binary_name}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getClass(@PathParam("classpath_name") String cpn, @PathParam("binary_name") String bn) {
		log.info("> getClass(" + bn +")");
		Response rv;
				
		byte[] res = dbcl.findClassAsBytes(cpn, bn);
		rv = res != null ? Response.ok(res).build() : Response.status(404).build();
		
		log.info("< getClass() = [" + rv.getStatus() +"]");
		return rv;
    }
	

	@GET
	@Path("/url/{classpath_name}/{name}")
    @Produces(MediaType.TEXT_PLAIN)
	public Response getResources(@PathParam("classpath_name") String cpn, @PathParam("name") String name) {
		log.info("> getResources(" + name +")");
		Response rv;
		List<String> res = dbcl.findResourcesAsList(cpn, name.trim());
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
	@Path("/resource/{path_element}/{name}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getResource(@PathParam("path_element") String pe, @PathParam("name") String name) {
		log.info("> getResource(" + pe + "," + name +")");
		Response rv = Response.status(404).build();
		byte[] res= dbcl.findResourceAsBytes(pe, name);
		if (res != null) {
			rv = Response.ok(res).build();
		}
		log.info("< getResource() = [" + rv.getStatus() +"]");
		return rv;
    }

}
