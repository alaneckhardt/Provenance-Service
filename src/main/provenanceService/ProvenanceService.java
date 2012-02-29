package provenanceService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

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
 * Provenance service can stored the given provenance data in the database. The primary interface is JSON object of this structure:<br>
 *
 * //Node<br>
 * {<br>
&nbsp;&nbsp;//Edges<br>
&nbsp;&nbsp;"adjacencies": [<br>
&nbsp;&nbsp;&nbsp;  {<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "id": "http://www.policygrid.org/ourspacesVRE.owl#1efaab5a-f9bf-4356-921f-e58753d21a99",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "to": "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "from": "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "type": "http://openprovenance.org/ontology#Used",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "typetext": "Used"&nbsp;//Used for display<br>
&nbsp;&nbsp;&nbsp;  },&nbsp;<br>
&nbsp;&nbsp;&nbsp;  {<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "id": "http://www.policygrid.org/ourspacesVRE.owl#1ee85774-cb9a-4e1e-85fe-e78988cd1b3a",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "to": "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "from": "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "type": "http://openprovenance.org/ontology#WasGeneratedBy",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "typetext": "WasGeneratedBy"&nbsp;//Used for display<br>
&nbsp;&nbsp;&nbsp;  }<br>
&nbsp;&nbsp;],<br>
&nbsp;&nbsp;//Node data<br>
&nbsp;&nbsp;"id": "http://xmlns.com/foaf/0.1/#0c8d01df-1d7a-4a2c-8298-b5e6fbb1aa9c",<br> 
&nbsp;&nbsp;"basicType": "Agent", //One of Agent, Artifact, Process<br>
&nbsp;&nbsp;"fullType":"http://xmlns.com/foaf/0.1/Person", //Subclass of the above<br>
&nbsp;&nbsp;"title": "Edoardo Pignotti",  <br>
&nbsp;&nbsp;//Further properties<br>
&nbsp;&nbsp;"properties": [<br>
&nbsp;&nbsp;&nbsp;  {<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "name": "http://www.policygrid.org/ourspacesVRE.owl#timestamp",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "value": "123456789"<br>
&nbsp;&nbsp;&nbsp;  },&nbsp;<br>
&nbsp;&nbsp;&nbsp;  {<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "name": "http://www.policygrid.org/ourspacesVRE.owl#anything",<br>
&nbsp;&nbsp;&nbsp;&nbsp;  "to": "http://openprovenance.org/ontology#anything"<br>
&nbsp;&nbsp;&nbsp;  }<br>
&nbsp;&nbsp;],<br>
&nbsp;}&nbsp;&nbsp;&nbsp;<br>
 * 
 * @author AE
 * @version 1.0
 */
public class ProvenanceService  extends javax.servlet.http.HttpServlet implements ServletContextListener, javax.servlet.Servlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5117777133122453926L;
	private static int lastSession = 0;

	private static String namespace;
	
	/**Contains RDF model for URI of processes. Used for validation of correctness of the model. The model is flushed into RDF repository in the commit method.*/
	private static Map<String, Model> sessions = null;
	/**Contains mapping from subclasses to Agent, Artifact, Process.*/
	private static Map<String, String> basicTypes = new HashMap<String, String>();
	private static List<String> properties = new ArrayList<String>();
	private static List<String> nodes = new ArrayList<String>();
	public ProvenanceService(){
	}

	public static void initProvenance(){
		if(sessions != null)
			return;
		RDFProvider.init();
		JSONProvider.init();
		
		namespace = Properties.getString("namespace");
		sessions = new HashMap<String, Model>();
		getBasicTypes().put("http://openprovenance.org/ontology#Process", "Process");
		getBasicTypes().put("http://openprovenance.org/ontology#Agent", "Agent");
		getBasicTypes().put("http://openprovenance.org/ontology#Artifact", "Artifact");
		Vector<String> subClasses = RDFProvider.getSubclasses(  "http://openprovenance.org/ontology#Edge");
		properties.addAll(subClasses);
		for(int i=0;i<properties.size();i++){
			subClasses = RDFProvider.getSubclasses(  properties.get(i));
			properties.addAll(subClasses);
		}
		
		//Can't use the easy way - we want to show the correct shapes and therefore know the superclass
		/*subClasses = ont.getSubclassListFull("general",  "http://openprovenance.org/ontology#Node");
		nodes.addAll(subClasses);
		for(int i=0;i<nodes.size();i++){
			subClasses = ont.getSubclassListFull("general",  nodes.get(i));
			nodes.addAll(subClasses);
		}
		properties.add("http://openprovenance.org/ontology#Used");
		properties.add("http://openprovenance.org/ontology#WasControlledBy");
		properties.add("http://openprovenance.org/ontology#WasTriggeredBy");
		properties.add("http://openprovenance.org/ontology#WasGeneratedBy");
		properties.add("http://openprovenance.org/ontology#WasDerivedFrom");
		*/
		//Agents subclasses
		nodes.add(Properties.getString("agent"));
		subClasses = RDFProvider.getSubclasses(Properties.getString("agent"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Agent");
		}

		//Artifacts subclasses
		nodes.add(Properties.getString("artifact"));
		subClasses = RDFProvider.getSubclasses(Properties.getString("artifact"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Artifact");
		}

		//Processes subclasses
		nodes.add(Properties.getString("process"));
		subClasses = RDFProvider.getSubclasses( Properties.getString("process"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Process");
		}					
	}
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{		
		initProvenance();
		PrintWriter out = response.getWriter();
		/**Action to perform.*/
		String action = request.getParameter("action");
        
        response.setContentType("text/plain");
		response.setHeader("Cache-Control", "no-cache");
		String output = "";

		if("addExistingResource".equals(action)){	
			String type = request.getParameter("type");
			type = URLDecoder.decode(type, "UTF-8");
			String session = request.getParameter("session");
			type = URLDecoder.decode(type, "UTF-8");
			String resource = request.getParameter("resource");
			type = URLDecoder.decode(type, "UTF-8");
			String title = request.getParameter("title");
			title = URLDecoder.decode(title, "UTF-8");				
			output = addExistingResource(session, resource, type, title);
		}
		else if("startSession".equals(action)){
			output = startSession();			
		}
		else if("clean".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			output = clean(session);			
		}
		else if("addProcess".equals(action)){
			String type = request.getParameter("type");
			type = URLDecoder.decode(type, "UTF-8");
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			if(type==null)
				output = addProcess(session);
			else
				output = addNode(session, type);
			
		}
		else if("addAgent".equals(action)){
			String type = request.getParameter("type");
			type = URLDecoder.decode(type, "UTF-8");
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			if(type==null)
				output += addAgent(session);
			else
				output += addNode(session, type);
			
		}
		else if("addArtifact".equals(action)){
			String type = request.getParameter("type");
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			if(type==null)
				output = addArtifact(session);
			else
				output = addNode(session, type);
					
		}
		else if("addNode".equals(action)){
			String type = request.getParameter("type");
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			output = addNode(session, type);					
		}
		else if("addCausalRelationship".equals(action)){
			String cause = request.getParameter("cause");
			String effect = request.getParameter("effect");
			String relation = request.getParameter("relation");
			String session = request.getParameter("session");
			cause = URLDecoder.decode(cause, "UTF-8");
			effect = URLDecoder.decode(effect, "UTF-8");
			relation = URLDecoder.decode(relation, "UTF-8");
			session = URLDecoder.decode(session, "UTF-8");
			output = addCausalRelationship(session, relation,  cause, effect);			
		}
		else if("addTitle".equals(action)){
			String title = request.getParameter("title");
			String object = request.getParameter("object");
			String session = request.getParameter("session");
			object = URLDecoder.decode(object, "UTF-8");
			session = URLDecoder.decode(session, "UTF-8");
			title = URLDecoder.decode(title, "UTF-8");
			session = URLDecoder.decode(session, "UTF-8");
			output = addTitle(session, object, title);			
		}		
		else if("removeCausalRelationShip".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			String relation = request.getParameter("relation");
			relation = URLDecoder.decode(session, "UTF-8");
			output = removeCausalRelationShip(session, relation);			
		}		
		else if("removeNode".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			String node = request.getParameter("node");
			node = URLDecoder.decode(node, "UTF-8");
			output = removeNode(session, node);			
		}		
		else if("getProcesses".equals(action)){
			String[] res = getSessions();	
			for (String string : res) {
				output += string+",";
			}
			//Trim last ,
			if(output.length()>2)
				output = output.substring(0,output.length()-1);
		}		
		else if("commit".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			try {
				output = commit(session);
			} catch (OpenRDFException e) {
				e.printStackTrace();
				output = "Error "+e.getLocalizedMessage();
			}	
		}		
		else if("rollback".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");
			rollback(session);
			output = "";	
		}			
		else if("getGraph".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");				
			Graph g = getGraph(session);
			output = graphToJSONString(g);	
		}	
		else if("getSPARQL".equals(action)){
			String session = request.getParameter("session");
			session = URLDecoder.decode(session, "UTF-8");	
			Graph g = getGraph(session);
			output = SPARQLProvider.getGraphSPARQL(g).toString();	
		}
		else if("getProvenance".equals(action)){
			String resource = request.getParameter("resource");
			resource = URLDecoder.decode(resource, "UTF-8");
			Graph g;
			try {
				g = getProvenance(resource);
				output = graphToJSONString(g);		
			} catch (OpenRDFException e) {
				e.printStackTrace();
				output = "Error "+e.getLocalizedMessage();
			}		
		}
		else if("getNode".equals(action)){
			String resource = request.getParameter("resource");
			resource = URLDecoder.decode(resource, "UTF-8");
			Node n = null;
			try {
				n = RDFProvider.getNode(null, resource);
				Graph g = new Graph();
				g.addNode(n);
				output = graphToJSONString(g);	
			} catch (OpenRDFException e) {
				e.printStackTrace();
				output = "Error "+e.getLocalizedMessage();
			} 			
		}
		
		out.print(output);
		out.flush();
	}

	/**
	 * Returns the shape for given type.
	 * @param type
	 * @return 
	 */
	public static String getShape(String type){
		return getBasicTypes().get(type);
	}
	
	/**
	 * 
	 * @return All the processes in the map.
	 */
	public static String[] getSessions(){
		String[] keys = new String[sessions.keySet().size()];
		keys = sessions.keySet().toArray(keys);
		return keys;
	}

	
	/**
	 * 
	 * @return The map of processes.
	 */
	public static Map<String, Model> getSessionsMap(){
		return sessions;
	}
	
	/**
	 * Transforms the given graph to JSON string.
	 * @param g
	 * @return
	 */
	public static String graphToJSONString(Graph g){
		JSONArray ar = JSONProvider.getGraphJSON(g);
		if(ar == null)
			return null;
		return ar.toString();
	}
	
	/**
	 * Starts the new session of creating provenance. Return sessionId.
	 * @return Id of the session.
	 */
	public static String startSession(){	
		initProvenance();
		Model model = ModelFactory.createDefaultModel();		
		model.createProperty(Properties.getString("title"));
		for(String s : nodes){
			Resource r = model.createResource(s);
			model.add(r, RDF.type, OWL.Class);
		}
		//Strange but true, properties in OPM are classes, too.
		for(String s : properties){
			Resource r = model.createResource(s);
			model.add(r, RDF.type, OWL.Class);
		}
		String id = ""+lastSession;
		sessions.put(id, model);
		lastSession++;
		return id;		
	}
	
	/**
	 * Adds a custom property to the given resource.
	 * @param sessionId
	 * @param node
	 * @param property
	 * @param value
	 * @return
	 */
	public static String addCustomProperty(String sessionId, String node, String property, String value){
		Model model = sessions.get(sessionId);		
		if(model == null)
			return "Error - no such process in queue"+sessionId;
		Resource res = model.createResource(node);
		Property p = model.getProperty(property);
		IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		boolean includeWarnings = false;
		IRI iri;
		iri = iriFactory.create(value); 
		//Literal
		if (iri.hasViolation(includeWarnings)) 
			res.addProperty(p, value);
		//Resource
		else
			res.addProperty(p, model.createResource(value));
		return "ok";
		
	}
	/**
	 * Create an instance of given type.	
	 * @param type Type of the process
	 * @return sessionId Id of the session.
	 */
	public static String addNode(String sessionId, String type){
		Model model = sessions.get(sessionId);		
		if(model == null)
			return "Error - no such process in queue"+sessionId;
		
		String instanceId = "http://www.policygrid.org/ourspacesVRE.owl#" + UUID.randomUUID().toString();
		Resource res = model.createResource(instanceId);
		model.createResource(type);
		model.add(res, RDF.type, model.createResource(type));
		return res.getURI();
		
	}
	/**
	 * Starts the process of creating provenance. Return rdfId of the new process. 
	 * @return URI of the new process.
	 */
	public static String addProcess(String sessionId){		
		return addNode(sessionId, "http://openprovenance.org/ontology#Process");		
	}
	

	/**
	 * Adds an agent to the provenance graph. It should be linked to the process later.
	 * @return URI of the new artifact.
	 */
	public static String addAgent(String sessionId){
		return addNode(sessionId, "http://openprovenance.org/ontology#Agent");
	}

	
	/**
	 * Adds an artefact to the provenance graph. It should be linked to the process later.
	 * @return URI of the new artifact.
	 */
	public static String addArtifact(String sessionId){
		return addNode(sessionId,"http://openprovenance.org/ontology#Artifact");
	}
	

	/**
	 * Adds title to the given object.
	 * @param sessionId Process
	 * @param object Object to receive the title 
	 * @param title Title.
	 */
	public static String addTitle(String sessionId, String object, String title){
		System.out.println(sessionId+","+object+","+title);
		Model model = sessions.get(sessionId);
		if(model == null)
			return "Error - no such process in queue "+sessionId;
		Resource res = model.getResource(object);
		Property p = model.getProperty(Properties.getString("title"));
		if(p==null){
			model.createProperty(Properties.getString("title"));
			p = model.getProperty(Properties.getString("title"));
		}
		model.add(res, p, title);
		return "ok";
	}
	
	/**
	 * Adds whole provenance graph to the given session.
	 * @param sessionId Process
	 */
	public static String addJSONGraph(String sessionId, String jsonGraph){
		JSONArray nodes = (JSONArray) JSONSerializer.toJSON(jsonGraph);
		
		Graph g = JSONProvider.getGraph(null, nodes);
		sessions.get(sessionId).add(RDFProvider.getGraphModel(g));
		return "ok";
	}
	
	
	public static String addRDFGraph(String sessionId, Model input){
		Model model = sessions.get(sessionId);
		model.add(input);
		return "ok";
	}
	public static String addRDFGraph(String sessionId, String input){
		Model model = sessions.get(sessionId);
		model.read(new StringReader(input), null);
		return "ok";
	}
	/**
	 * Adds relationship to the process. Causal relationships are e.g. controlledBy, Used, wasGeneratedBy,...
	 * @param sessionId URI of the process to be the relation added to.
	 * @param type The type of the causal relationship.
	 * @param cause The subject of the relationship - this can be artifact, agent, or other process.
	 * @param effect The object of the relationship - this can be artifact, agent, or other process.
	 * @return
	 */
	public static String addCausalRelationship(String sessionId,String type, String cause,  String effect){		
		Model model = sessions.get(sessionId);
		String relationId = "http://www.policygrid.org/ourspacesVRE.owl#" + UUID.randomUUID().toString();

		if(model == null)
			return "Error - no such process in queue "+sessionId;
		Resource relationship = model.createResource(relationId);
		Resource c = model.getResource(cause);
		Resource e = model.getResource(effect);
		Resource r = model.getResource(type);
		model.add(relationship, RDF.type, r);
		model.add(relationship, RDFProvider.getProp("to"), c);
		model.add(relationship, RDFProvider.getProp("from"), e);
		return relationship.getURI();
	}
	
	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 * @param sessionId
	 * @param uri
	 * @return
	 */
	public static String addExistingResource(String sessionId, String uri){	
		Node n;
		try {
			n = RDFProvider.getNode(null, uri);
			return addExistingResource(sessionId, uri, n.getType(), n.getTitle());
		} catch (org.openrdf.OpenRDFException e) {
			e.printStackTrace();
			return "Error" + e.getLocalizedMessage();
		}
	}
	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 * @param sessionId
	 * @param uri URI of the resource
	 * @param type Type of the resource
	 * @param title Title of the resource - for the visualisation purpose.
	 * @return
	 */
	public static String addExistingResource(String sessionId, String uri, String type, String title){		
		Model model = sessions.get(sessionId);
		if(model == null)
			return "Error - no such process in queue "+sessionId;
		model.add(model.getResource(uri), RDF.type, model.getResource(type));
		if(title != null && title.length()>0)
			addTitle(sessionId, uri, title);
		return "ok";
	}
	

	/**
	 * Returns the JSON representation of the Graph associated with the given sessionId.
	 * @param sessionId
	 * @return
	 */
	public static JSONArray getJSONGraph(String sessionId){
		Graph g = getGraph(sessionId);
		return JSONProvider.getGraphJSON(g);
	}
	/**
	 * Returns the Graph associated with the given sessionId.
	 * @param sessionId
	 * @return
	 * @throws OpenRDFException 
	 */
	public static Graph getGraph(String sessionId) {
		Model model = sessions.get(sessionId);
		if(model == null)
			return null;
		/*Graph g = new Graph();
		//TODO verify the correctness of this
		for(String nodeType : nodes){
			ResIterator it = model.listResourcesWithProperty(RDF.type, model.getResource(nodeType));
			while(it.hasNext()){
				Resource r = it.next();
				g.addNode(RDFProvider.getNode(g, r));
			}
		}
		for(Node n : g.getNodes()){
			RDFProvider.getAdjacencies(g, n, 2);
		}*/
		return RDFProvider.getModelGraph(model);		
		
	}

	/**
	 * Returns the RDF model associated with the given sessionId.
	 * @param sessionId
	 * @return
	 */
	public static Model getModel(String sessionId){
		return sessions.get(sessionId);		
	}
	
	/**
	 * Removes the given relationship from the model.
	 * @param sessionId
	 * @param relationship URI of the relationship.
	 * @return
	 */
	public static String removeCausalRelationShip(String sessionId, String relationship){
		Model model = sessions.get(sessionId);
		Resource relation = model.getResource(relationship);
		model.remove(relation.listProperties());		
		return "ok";
	}
	/**
	 * Removes the given node from the model.
	 * @param sessionId
	 * @param node
	 * @return
	 */
	public static String removeNode(String sessionId, String node){
		Model model = sessions.get(sessionId);
		Resource res = model.getResource(node);
		model.remove(res.listProperties());
		return "ok";
	}
	

	/**
	 * Cleans the RDF model, leaving only the provenance stuff in the model.
	 * @param sessionId
	 * @return
	 */
	public static String clean(String sessionId){
		try{
			Model m = sessions.get(sessionId);
			Graph g = getGraph(sessionId);
			m.removeAll();
			m.add(RDFProvider.getGraphModel(g));
		}
		catch(Exception e){
			e.printStackTrace();
			return "Error " + e.getLocalizedMessage();
		}
		return "ok";
	}
	
	
	/**
	 * 
	 * @return true if commit was successful, false otherwise.
	 * @throws IOException 
	 * @throws Exception When the RDF model of the given process is bad or does not satisfy policies.
	 */
	public static String commit(String sessionId) throws OpenRDFException, IOException {
		Model m = sessions.get(sessionId);
		try {
			RDFProvider.write(m);
		} catch (OpenRDFException e) {
			e.printStackTrace();
			throw e;
		}
		//All ok, discard the model.
		m.close();
		sessions.remove(sessionId);
		//TODO policies handling and validation
		
		return "ok";
	}
	
	/**
	 * Deletes the RDF model of the given process.
	 * @param sessionId URI of the process
	 */
	public static void rollback(String sessionId){
		sessions.remove(sessionId);
		//TODO perform rollback
	}
	
	/**
	 * Finds the provenance graph near the given resource. 
	 * The edges to and from the resource are obtained. If a process is among the neighoring nodes, then also its edges are obtained. 
	 * @param resource
	 * @return Provenance graph near the given resource.
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws RepositoryException 
	 */
	public static Graph getProvenance(String resource) throws OpenRDFException{
		Graph prov = getImmediateProvenance(resource);
		Graph provAll = prov;
		for(Node n:prov.getNodes()){
			//Load the provenance only of processes
			try {
				if(!"Process".equals(getShape(n.getType())))
						continue;
				Graph prov2 = getImmediateProvenance(n.getId());
				provAll = provAll.merge(prov2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
		return provAll;
	}
	

	/**
	 * Gets the edges from the given resource.
	 * @param resourceID
	 * @return Graph containing the given resource and all edges directing from it.
	 * @throws OpenRDFException
	 */
	public static Graph getProvenanceFrom(String resourceID) throws OpenRDFException{		
		Graph g= new Graph();
		Node n = RDFProvider.getNode(g, resourceID);
		g.addNode(n);
		RDFProvider.getAdjacencies(g, n, 1);
		return g;
	}
	/*private static Node processOneLine(BindingSet bindingSet, Node res, boolean from){
			String edge = bindingSet.getValue("edge").stringValue();
		   String etype = bindingSet.getValue("etype").stringValue();
		   String o = bindingSet.getValue("o").stringValue();
		   String otype = bindingSet.getValue("otype").stringValue();
		   String otitle = "";
		   if(bindingSet.hasBinding("otitle"))
			   otitle  = bindingSet.getValue("otitle").stringValue();
		   
		   if("Agent".equals(basicTypes.get(otype))){
				String firstname = "", surname = "";
				try {
					firstname = rdf.getFoafPropertyFromFoafID(o, FOAF.firstName.toString());
					surname = rdf.getFoafPropertyFromFoafID(o, FOAF.surname.toString());
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
				String fullname = firstname + " " + surname;
				otitle = fullname;
			}
		   Edge e = new Edge();	
		   Node n = new Node(o);
		   if(from){
			   e.setFrom(res);
			   e.setTo(n);			   
		   }
		   else{
			   e.setFrom(n);	
			   e.setTo(res);		   
		   }
		   e.setId(edge);
		   e.setType(etype);
		   n.setTitle(otitle);
		   n.setType(otype);	
		   n.addAdjacency(e);
		   res.addAdjacency(e);
		   return n;
	}*/
	/**
	 * Gets the edges to the given resource.
	 * @param resourceID
	 * @return Graph containing the given resource and all edges directing to it.
	 * @throws OpenRDFException
	 */
	public static Graph getProvenanceTo(String resourceID) throws OpenRDFException{		
		Graph g = new Graph();

		Node n = RDFProvider.getNode(g, resourceID);
		g.addNode(n);
		RDFProvider.getAdjacencies(g, n, 0);
		return g;
	}
	
	/**
	 * Returns the graph immediatelly around the given resource. Data are obtained from the underlying RDF repository.
	 * @param resource
	 * @return only the edges to and from the given resource.
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws RepositoryException 
	 */
	public static Graph getImmediateProvenance(String resource) throws OpenRDFException {
		Graph l = getProvenanceTo(resource);
		l = l.merge(getProvenanceFrom(resource));
		for (int i = 0; i < l.size(); i++) {
			Node n = l.get(i);
			for(Edge e : n.getAdjacencies()){
				if(!l.contains(e.getTo())){
					l.addNode(e.getTo());
				}
				if(!l.contains(e.getFrom())){
					l.addNode(e.getFrom());
				}
			}
		}
		return l;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("The Provenance Service - contextDestroyed");
	}

	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Initialising Provenance Service");
		Properties.setBaseFolder(event.getServletContext().getRealPath("/"));
		RDFProvider.init();
		JSONProvider.init();
		initProvenance();
		// Output a simple message to the server's console
		System.out.println("The Provenance Service is running");
	}

	public static Map<String, String> getBasicTypes() {
		return basicTypes;
	}

	public static void setBasicTypes(Map<String, String> basicTypes) {
		ProvenanceService.basicTypes = basicTypes;
	}

	public static String getNamespace() {
		return namespace;
	}

	public static void setNamespace(String namespace) {
		ProvenanceService.namespace = namespace;
	}

	public static List<String> getProperties() {
		return properties;
	}

	public static void setProperties(List<String> properties) {
		ProvenanceService.properties = properties;
	}

	public static List<String> getNodes() {
		return nodes;
	}

	public static void setNodes(List<String> nodes) {
		ProvenanceService.nodes = nodes;
	}
}