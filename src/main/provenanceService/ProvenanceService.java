package provenanceService;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

/**
 * Provenance service can stored the given provenance data in the database. The
 * primary interface is JSON object of this structure:<br>
 *
 * //Node<br>
 * {<br>
 * &nbsp;&nbsp;//Edges<br>
 * &nbsp;&nbsp;"adjacencies": [<br>
 * &nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"id":
 * "http://www.policygrid.org/ourspacesVRE.owl#1efaab5a-f9bf-4356-921f-e58753d21a99"
 * ,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"to":
 * "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"from":
 * "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"type": "http://openprovenance.org/ontology#Used",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"typetext": "Used"&nbsp;//Used for display<br>
 * &nbsp;&nbsp;&nbsp;},&nbsp;<br>
 * &nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"id":
 * "http://www.policygrid.org/ourspacesVRE.owl#1ee85774-cb9a-4e1e-85fe-e78988cd1b3a"
 * ,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"to":
 * "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"from":
 * "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"type":
 * "http://openprovenance.org/ontology#WasGeneratedBy",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"typetext": "WasGeneratedBy"&nbsp;//Used for display<br>
 * &nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;],<br>
 * &nbsp;&nbsp;//Node data<br>
 * &nbsp;&nbsp;"id":
 * "http://xmlns.com/foaf/0.1/#0c8d01df-1d7a-4a2c-8298-b5e6fbb1aa9c",<br>
 * &nbsp;&nbsp;"basicType": "Agent", //One of Agent, Artifact, Process<br>
 * &nbsp;&nbsp;"fullType":"http://xmlns.com/foaf/0.1/Person", //Subclass of the
 * above<br>
 * &nbsp;&nbsp;"title": "Edoardo Pignotti", <br>
 * &nbsp;&nbsp;//Further properties<br>
 * &nbsp;&nbsp;"properties": [<br>
 * &nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"name":
 * "http://www.policygrid.org/ourspacesVRE.owl#timestamp",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"value": "123456789"<br>
 * &nbsp;&nbsp;&nbsp;},&nbsp;<br>
 * &nbsp;&nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"name":
 * "http://www.policygrid.org/ourspacesVRE.owl#anything",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"to": "http://openprovenance.org/ontology#anything"<br>
 * &nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;],<br>
 * &nbsp;}&nbsp;&nbsp;&nbsp;<br>
 *
 * @author Alan Eckhardt a.e@centrum.cz
 * @version 1.0
 */
public class ProvenanceService extends javax.servlet.http.HttpServlet implements ServletContextListener, javax.servlet.Servlet {
	/** serial version UID. */
	private static final long serialVersionUID = 5117777133122453926L;
	/** Implementation of ProvService. */
	private static ProvenanceServiceImpl impl = null;

	/**
	 * Empty constructor.
	 */
	public ProvenanceService() {
	}

	/**
	 * @param request
	 *            request
	 * @param response
	 *            response
	 * @throws IOException
	 *             IOException
	 * @throws ServletException
	 *             ServletException
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		if(impl == null)
			impl = new ProvenanceServiceImpl();
		impl.initProvenance();
		PrintWriter out = response.getWriter();
		/** Action to perform. */
		String action = request.getParameter("action");

		response.setContentType("text/plain");
		response.setHeader("Cache-Control", "no-cache");
		String output = "";
		try {
			if ("addExistingResource".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				String resource = request.getParameter("resource");
				String title = request.getParameter("title");
				type = URLDecoder.decode(type, "UTF-8");
				session = URLDecoder.decode(session, "UTF-8");
				resource = URLDecoder.decode(resource, "UTF-8");
				title = URLDecoder.decode(title, "UTF-8");
				impl.addExistingResource(session, resource, type, title);
				output = "ok";
			} else if ("startSession".equals(action)) {
				output = impl.startSession();
			} else if ("clean".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				impl.clean(session);
				output = "ok";
			} else if ("addProcess".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output = impl.addProcess(session);
				else{
					type = URLDecoder.decode(type, "UTF-8");
					output = impl.addNode(session, type);
				}

			} else if ("addAgent".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output += impl.addAgent(session);
				else{
					type = URLDecoder.decode(type, "UTF-8");
					output += impl.addNode(session, type);
				}

			} else if ("addArtifact".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output = impl.addArtifact(session);
				else{
					type = URLDecoder.decode(type, "UTF-8");
					output = impl.addNode(session, type);
				}

			} else if ("addNode".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				type = URLDecoder.decode(type, "UTF-8");
				output = impl.addNode(session, type);
			} else if ("addCausalRelationship".equals(action)) {
				String from = request.getParameter("from");
				String to = request.getParameter("to");
				String relation = request.getParameter("relation");
				String session = request.getParameter("session");
				from = URLDecoder.decode(from, "UTF-8");
				to = URLDecoder.decode(to, "UTF-8");
				relation = URLDecoder.decode(relation, "UTF-8");
				session = URLDecoder.decode(session, "UTF-8");
				output = impl.addCausalRelationship(session, relation, from, to);
			} else if ("addTitle".equals(action)) {
				String title = request.getParameter("title");
				String resource = request.getParameter("resource");
				String session = request.getParameter("session");
				resource = URLDecoder.decode(resource, "UTF-8");
				session = URLDecoder.decode(session, "UTF-8");
				title = URLDecoder.decode(title, "UTF-8");
				impl.addTitle(session, resource, title);
				output = "ok";			
			} else if ("addCustomProperty".equals(action)) {
					String property = request.getParameter("property");
					String value = request.getParameter("value");
					String resource = request.getParameter("resource");
					String session = request.getParameter("session");
					resource = URLDecoder.decode(resource, "UTF-8");
					session = URLDecoder.decode(session, "UTF-8");
					value = URLDecoder.decode(value, "UTF-8");
					property = URLDecoder.decode(property, "UTF-8");
					impl.addCustomProperty(session, resource, property, value);
					output = "ok";

			} else if ("removeCausalRelationShip".equals(action)) {
				String session = request.getParameter("session");
				String relation = request.getParameter("relation");
				session = URLDecoder.decode(session, "UTF-8");
				relation = URLDecoder.decode(relation, "UTF-8");
				impl.removeCausalRelationShip(session, relation);
				output = "ok";
			} else if ("removeNode".equals(action)) {
				String session = request.getParameter("session");
				String node = request.getParameter("node");
				session = URLDecoder.decode(session, "UTF-8");
				node = URLDecoder.decode(node, "UTF-8");
				impl.removeNode(session, node);
				output = "ok";
			} else if ("getProcesses".equals(action)) {
				String[] res = impl.getSessions();
				for (String string : res) {
					output += string + ",";
				}
				// Trim last ,
				if (output.length() > 2)
					output = output.substring(0, output.length() - 1);
			} else if ("commit".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				try {
					impl.clean(session);
					impl.commit(session);
					output = "ok";
				} catch (Exception e) {
					e.printStackTrace();
					output = "Error " + e.getLocalizedMessage();
				}
			} else if ("rollback".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				impl.rollback(session);
				output = "ok";
			} else if ("getGraph".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				Graph g = impl.getGraph(session);
				output = graphToJSONString(g);
			} else if ("getSPARQL".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				Graph g = impl.getGraph(session);
				output = impl.getSPARQLProvider().getGraphSPARQL(g, true).toString();
			} else if ("getProvenance".equals(action)) {
				String resource = request.getParameter("resource");
				String session = request.getParameter("session");
				resource = URLDecoder.decode(resource, "UTF-8");
				Graph g;
				try {
					g = impl.getProvenance(resource, session);
					output = graphToJSONString(g);
				} catch (Exception e) {
					e.printStackTrace();
					output = "Error " + e.getLocalizedMessage();
				}
			} else if ("getNode".equals(action)) {
				String resource = request.getParameter("resource");
				resource = URLDecoder.decode(resource, "UTF-8");
				Node n = null;
					n = impl.getDataProvider().getNode(null, resource);
					Graph g = new Graph();
					g.addNode(n);
					output = graphToJSONString(g);
			}

		} catch (ProvenanceServiceException e) {
			e.printStackTrace();
			output = "Error " + e.getLocalizedMessage();
		}
		out.print(output);
		out.flush();
	}

	/**
	 * Initialises the ontologies.
	 */
	public static void initProvenance() {
		if(impl == null)
			impl = new ProvenanceServiceImpl();
		impl.initProvenance();
	}
	/**
	 * Transforms the given graph to JSON string.
	 *
	 * @param g
	 *            Graph to be converted.
	 * @return JSON String
	 */
	public static String graphToJSONString(final Graph g) {
		JSONArray ar = impl.getJSONProvider().getGraphJSON(g);
		if (ar == null)
			return null;
		return ar.toString();
	}

	/**
	 * @param event
	 *            Default arg.
	 */
	public void contextInitialized(final ServletContextEvent event) {
		System.out.println("Initialising Provenance Service");
		Properties.setBaseFolder(event.getServletContext().getRealPath("/"));
		if(impl == null)
			impl = new ProvenanceServiceImpl();
		impl.initProvenance();
		// Output a simple message to the server's console
		System.out.println("The Provenance Service is running");
	}

	/**
	 * @param arg0
	 *            Default arg.
	 */
	public void contextDestroyed(final ServletContextEvent arg0) {
		System.out.println("The Provenance Service - contextDestroyed");
	}
	public static ProvenanceServiceImpl getSingleton(){
		if(impl == null)
			impl = new ProvenanceServiceImpl();
		return impl;
	}
	public static ProvenanceServiceImpl getNextImpl(){
		ProvenanceServiceImpl impl = new ProvenanceServiceImpl();
		impl.initProvenance();
		return impl;
	}

}
