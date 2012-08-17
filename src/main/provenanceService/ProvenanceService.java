package provenanceService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;

import org.openrdf.OpenRDFException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

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
 * @author AE
 * @version 1.0
 */
public class ProvenanceService extends javax.servlet.http.HttpServlet implements ServletContextListener, javax.servlet.Servlet {
	/** serial version UID. */
	private static final long serialVersionUID = 5117777133122453926L;
	/** Implementation of ProvService. */
	private static ProvenanceServiceImpl impl = new ProvenanceServiceImpl();

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
				type = URLDecoder.decode(type, "UTF-8");
				String session = request.getParameter("session");
				type = URLDecoder.decode(type, "UTF-8");
				String resource = request.getParameter("resource");
				type = URLDecoder.decode(type, "UTF-8");
				String title = request.getParameter("title");
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
				type = URLDecoder.decode(type, "UTF-8");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output = impl.addProcess(session);
				else
					output = impl.addNode(session, type);

			} else if ("addAgent".equals(action)) {
				String type = request.getParameter("type");
				type = URLDecoder.decode(type, "UTF-8");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output += impl.addAgent(session);
				else
					output += impl.addNode(session, type);

			} else if ("addArtifact".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				if (type == null)
					output = impl.addArtifact(session);
				else
					output = impl.addNode(session, type);

			} else if ("addNode".equals(action)) {
				String type = request.getParameter("type");
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
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
				String object = request.getParameter("object");
				String session = request.getParameter("session");
				object = URLDecoder.decode(object, "UTF-8");
				session = URLDecoder.decode(session, "UTF-8");
				title = URLDecoder.decode(title, "UTF-8");
				impl.addTitle(session, object, title);
				output = "ok";
			} else if ("removeCausalRelationShip".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				String relation = request.getParameter("relation");
				relation = URLDecoder.decode(relation, "UTF-8");
				impl.removeCausalRelationShip(session, relation);
				output = "ok";
			} else if ("removeNode".equals(action)) {
				String session = request.getParameter("session");
				session = URLDecoder.decode(session, "UTF-8");
				String node = request.getParameter("node");
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
					impl.commit(session);
					output = "ok";
				} catch (OpenRDFException e) {
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
				output = SPARQLProvider.getGraphSPARQL(g, true).toString();
			} else if ("getProvenance".equals(action)) {
				String resource = request.getParameter("resource");
				String session = request.getParameter("session");
				resource = URLDecoder.decode(resource, "UTF-8");
				Graph g;
				try {
					g = impl.getProvenance(resource, session);
					output = graphToJSONString(g);
				} catch (OpenRDFException e) {
					e.printStackTrace();
					output = "Error " + e.getLocalizedMessage();
				}
			} else if ("getNode".equals(action)) {
				String resource = request.getParameter("resource");
				resource = URLDecoder.decode(resource, "UTF-8");
				Node n = null;
				try {
					n = impl.getDataProvider().getNode(null, resource);
					Graph g = new Graph();
					g.addNode(n);
					output = graphToJSONString(g);
				} catch (OpenRDFException e) {
					e.printStackTrace();
					output = "Error " + e.getLocalizedMessage();
				}
			}

		} catch (ProvenanceServiceException e) {
			e.printStackTrace();
			output = "Error " + e.getLocalizedMessage();
		}
		out.print(output);
		out.flush();
	}

	/**
	 * Transforms the given graph to JSON string.
	 * 
	 * @param g
	 *            Graph to be converted.
	 * @return JSON String
	 */
	public static String graphToJSONString(final Graph g) {
		JSONArray ar = JSONProvider.getGraphJSON(g);
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
		RDFProvider.init();
		JSONProvider.init();
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

	/**
	 * Returns the shape for given type.
	 * 
	 * @param type
	 * @return
	 */
	 public static String getShape(String type) {
	  return impl.getBasicTypes().get(type);
	 }

	/**
	 * 
	 * @return All the processes in the map.
	 */
	public static String[] getSessions() {
		return impl.getSessions();
	}

	/**
	 * 
	 * @return The map of processes.
	 */
	public static Map<String, Model> getSessionsMap() {
		return impl.getSessionsMap();
	}

	/**
	 * Starts the new session of creating provenance. Return sessionId.
	 * 
	 * @return Id of the session.
	 */
	public static String startSession() {
		return impl.startSession();
	}

	/**
	 * Adds a custom property to the given resource.
	 * 
	 * @param sessionId
	 * @param node
	 * @param property
	 * @param value
	 * @return
	 * @throws ProvenanceServiceException
	 */
	public static void addCustomProperty(String sessionId, String node, String property, String value) throws ProvenanceServiceException {
		impl.addCustomProperty(sessionId, node, property, value);
	}

	/**
	 * Create an instance of given type.
	 * 
	 * @param type
	 *            Type of the node
	 * @return sessionId Id of the session.
	 * @throws ProvenanceServiceException
	 */
	public static String addNode(String sessionId, String type) throws ProvenanceServiceException {
		return impl.addNode(sessionId, type);
	}

	/**
	 * Starts the process of creating provenance. Return rdfId of the new
	 * process.
	 * 
	 * @return URI of the new process.
	 * @throws ProvenanceServiceException
	 */
	public static String addProcess(String sessionId) throws ProvenanceServiceException {
		return impl.addProcess(sessionId);
	}

	/**
	 * Adds an agent to the provenance graph. It should be linked to the process
	 * later.
	 * 
	 * @return URI of the new artifact.
	 * @throws ProvenanceServiceException
	 */
	public static String addAgent(String sessionId) throws ProvenanceServiceException {
		return impl.addAgent(sessionId);
	}

	/**
	 * Adds an artefact to the provenance graph. It should be linked to the
	 * process later.
	 * 
	 * @return URI of the new artifact.
	 * @throws ProvenanceServiceException
	 */
	public static String addArtifact(String sessionId) throws ProvenanceServiceException {
		return impl.addArtifact(sessionId);
	}

	/**
	 * Adds title to the given object.
	 * 
	 * @param sessionId
	 *            Process
	 * @param object
	 *            Object to receive the title
	 * @param title
	 *            Title.
	 * @throws ProvenanceServiceException
	 */
	public static void addTitle(String sessionId, String object, String title) throws ProvenanceServiceException {
		impl.addTitle(sessionId, object, title);
	}

	/**
	 * Adds whole provenance graph to the given session.
	 * 
	 * @param sessionId
	 *            Process
	 */
	public static void addJSONGraph(String sessionId, String jsonGraph) {
		impl.addJSONGraph(sessionId, jsonGraph);
	}

	public static void addRDFGraph(String sessionId, Model input) {
		impl.addRDFGraph(sessionId, input);
	}

	public static void addRDFGraph(String sessionId, String input) {
		impl.addRDFGraph(sessionId, input);
	}

	/**
	 * Adds relationship to the process. Causal relationships are e.g.
	 * controlledBy, Used, wasGeneratedBy,...
	 * 
	 * @param sessionId
	 *            URI of the process to be the relation added to.
	 * @param type
	 *            The type of the causal relationship.
	 * @param from
	 *            The subject of the relationship - this can be artifact, agent,
	 *            or other process.
	 * @param to
	 *            The object of the relationship - this can be artifact, agent,
	 *            or other process.
	 * @return
	 * @throws ProvenanceServiceException
	 */
	public static String addCausalRelationship(String sessionId, String type, String from, String to) throws ProvenanceServiceException {
		return impl.addCausalRelationship(sessionId, type, from, to);
	}

	public static DataProvider getDataProvider() {
		return impl.getDataProvider();
	}

	public static void setDataProvider(DataProvider dataProvider) {
		impl.setDataProvider(dataProvider);
	}

	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 * 
	 * @param sessionId
	 * @param uri
	 * @return
	 * @throws ProvenanceServiceException
	 */
	public static void addExistingResource(String sessionId, String uri) throws ProvenanceServiceException {
		impl.addExistingResource(sessionId, uri);
	}

	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 * 
	 * @param sessionId
	 * @param uri
	 *            URI of the resource
	 * @param type
	 *            Type of the resource
	 * @param title
	 *            Title of the resource - for the visualisation purpose.
	 * @return
	 * @throws ProvenanceServiceException
	 */
	public static void addExistingResource(String sessionId, String uri, String type, String title) throws ProvenanceServiceException {
		impl.addExistingResource(sessionId, uri, type, title);
	}

	/**
	 * Returns the JSON representation of the Graph associated with the given
	 * sessionId.
	 * 
	 * @param sessionId
	 * @return
	 */
	public static JSONArray getJSONGraph(String sessionId) {
		return impl.getJSONGraph(sessionId);
	}

	/**
	 * Returns the Graph associated with the given sessionId.
	 * 
	 * @param sessionId
	 * @return
	 * @throws OpenRDFException
	 */
	public static Graph getGraph(String sessionId) {
		return impl.getGraph(sessionId);

	}

	/**
	 * Returns the RDF model associated with the given sessionId.
	 * 
	 * @param sessionId
	 * @return
	 */
	public static Model getModel(String sessionId) {
		return impl.getModel(sessionId);
	}

	/**
	 * Removes the given relationship from the model.
	 * 
	 * @param sessionId
	 * @param relationship
	 *            URI of the relationship.
	 * @return
	 */
	public static void removeCausalRelationShip(String sessionId, String relationship) {
		impl.removeCausalRelationShip(sessionId, relationship);
	}

	/**
	 * Removes the given node from the model.
	 * 
	 * @param sessionId
	 * @param node
	 * @return
	 */
	public static void removeNode(String sessionId, String node) {
		impl.removeNode(sessionId, node);
	}

	/**
	 * Cleans the RDF model, leaving only the provenance stuff in the model.
	 * 
	 * @param sessionId
	 * @return
	 * @throws ProvenanceServiceException 
	 */
	public static void clean(String sessionId) throws ProvenanceServiceException {
		impl.clean(sessionId);
	}

	/**
	 * 
	 * @return true if commit was successful, false otherwise.
	 * @throws IOException
	 * @throws ProvenanceServiceException
	 * @throws Exception
	 *             When the RDF model of the given process is bad or does not
	 *             satisfy policies.
	 */
	public static void commit(String sessionId) throws OpenRDFException, IOException, ProvenanceServiceException {
		impl.commit(sessionId);
	}

	/**
	 * Deletes the RDF model of the given process.
	 * 
	 * @param sessionId
	 *            URI of the process
	 */
	public static void rollback(String sessionId) {
		impl.rollback(sessionId);
	}

	/**
	 * Finds the provenance graph near the given resource.
	 * The edges to and from the resource are obtained. If a process is among
	 * the neighoring nodes, then also its edges are obtained.
	 * 
	 * @param resource
	 * @return Provenance graph near the given resource.
	 * @throws QueryEvaluationException
	 * @throws MalformedQueryException
	 * @throws RepositoryException
	 */
	public static Graph getProvenance(String resource, String sessionId) throws OpenRDFException {
		return impl.getProvenanceFrom(resource, sessionId);
	}

	/**
	 * Gets the edges from the given resource.
	 * 
	 * @param resourceID
	 * @return Graph containing the given resource and all edges directing from
	 *         it.
	 * @throws OpenRDFException
	 */
	public static Graph getProvenanceFrom(String resourceID, String sessionId) throws OpenRDFException {
		return impl.getProvenanceFrom(resourceID, sessionId);
	}

	/**
	 * Gets the edges to the given resource.
	 * 
	 * @param resourceID
	 * @return Graph containing the given resource and all edges directing to
	 *         it.
	 * @throws OpenRDFException
	 */
	public static Graph getProvenanceTo(String resourceID, String sessionId) throws OpenRDFException {
		return impl.getProvenanceTo(resourceID, sessionId);
	}

	/**
	 * Returns the graph immediatelly around the given resource. Data are
	 * obtained from the underlying RDF repository.
	 * 
	 * @param resource
	 * @return only the edges to and from the given resource.
	 * @throws QueryEvaluationException
	 * @throws MalformedQueryException
	 * @throws RepositoryException
	 */
	public static Graph getImmediateProvenance(String resource, String sessionId) throws OpenRDFException {
		return impl.getImmediateProvenance(resource, sessionId);
	}

	public static Map<String, String> getBasicTypes() {
		return impl.getBasicTypes();
	}

	public static void setBasicTypes(Map<String, String> basicTypes) {
		impl.setBasicTypes(basicTypes);
	}

	public static void setSessions(Map<String, Model> sessions) {
		impl.setSessions(sessions);
	}

	public static String getNamespace() {
		return impl.getNamespace();
	}

	public static void setNamespace(String namespace) {
		impl.setNamespace(namespace);
	}

	public static List<String> getProperties() {
		return impl.getProperties();
	}

	public static void setProperties(List<String> properties) {
		impl.setProperties(properties);
	}

	public static List<String> getNodes() {
		return impl.getNodes();
	}

	public static void setNodes(List<String> nodes) {
		impl.setNodes(nodes);
	}

}
