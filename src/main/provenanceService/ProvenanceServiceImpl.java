package provenanceService;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

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
public class ProvenanceServiceImpl {
	/**
	 * 
	 */
	private static final long serialVersionUID = 51177771345689726L;
	private int lastSession = 0;

	private String namespace;
	
	/**Contains RDF model for URI of processes. Used for validation of correctness of the model. The model is flushed into RDF repository in the commit method.*/
	private  Map<String, Model> sessions = null;
	private  Map<String, List<String>> sessionsDelete = null;
	/**Contains mapping from subclasses to Agent, Artifact, Process.*/
	private  Map<String, String> basicTypes = new HashMap<String, String>();
	private  List<String> properties = new ArrayList<String>();
	private  List<String> nodes = new ArrayList<String>();
	private  DataProvider dataProvider = new DataProvider();
	public ProvenanceServiceImpl(){
		initProvenance();
	}

	public  void initProvenance(){
		if(sessions != null)
			return;
		RDFProvider.init();
		JSONProvider.init(); 
		this.dataProvider = new DataProvider();
		this.dataProvider.init();
		
		namespace = Properties.getString("namespace");
		sessions = new HashMap<String, Model>();
		
		sessionsDelete = new HashMap<String, List<String>>();
		initNodes();
		initEdges();
	}

	public  void initEdges(){

		properties = new ArrayList<String>();
		Vector<String> subClasses = dataProvider.getSubclasses(Properties.getString("edge"));
		properties.addAll(subClasses);
		for(int i=0;i<properties.size();i++){
			subClasses = dataProvider.getSubclasses(  properties.get(i));
			properties.addAll(subClasses);
		}
	}
	public  void initNodes(){
		getBasicTypes().clear();
		getBasicTypes().put(Properties.getString("process"), "Process");
		getBasicTypes().put(Properties.getString("agent"), "Agent");
		getBasicTypes().put(Properties.getString("artifact"), "Artifact");
		Vector<String> subClasses;
		nodes = new ArrayList<String>();
		//Agents subclasses
		nodes.add(Properties.getString("agent"));
		subClasses = dataProvider.getSubclasses(Properties.getString("agent"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Agent");
		}

		//Artifacts subclasses
		nodes.add(Properties.getString("artifact"));
		subClasses = dataProvider.getSubclasses(Properties.getString("artifact"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Artifact");
		}

		//Processes subclasses
		nodes.add(Properties.getString("process"));
		subClasses = dataProvider.getSubclasses( Properties.getString("process"));
		nodes.addAll(subClasses);
		for(int i=0;i<subClasses.size();i++){
			getBasicTypes().put(subClasses.get(i),"Process");
		}
	}

	/**
	 * Returns the shape for given type.
	 * @param type
	 * @return 
	 */
	public  String getShape(String type){
		return getBasicTypes().get(type);
	}
	
	/**
	 * 
	 * @return All the processes in the map.
	 */
	public  String[] getSessions(){
		String[] keys = new String[sessions.keySet().size()];
		keys = sessions.keySet().toArray(keys);
		return keys;
	}

	
	/**
	 * 
	 * @return The map of processes.
	 */
	public  Map<String, Model> getSessionsMap(){
		return sessions;
	}
	
	
	/**
	 * Starts the new session of creating provenance. Return sessionId.
	 * @return Id of the session.
	 */
	public  String startSession(){	
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
	public  String addCustomProperty(String sessionId, String node, String property, String value){
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
	public  String addNode(String sessionId, String type){
		Model model = sessions.get(sessionId);		
		if(model == null)
			return "Error - no such process in queue"+sessionId;
		
		String instanceId = namespace + UUID.randomUUID().toString();
		Resource res = model.createResource(instanceId);
		model.createResource(type);
		model.add(res, RDF.type, model.createResource(type));
		return res.getURI();
		
	}
	/**
	 * Starts the process of creating provenance. Return rdfId of the new process. 
	 * @return URI of the new process.
	 */
	public  String addProcess(String sessionId){		
		return addNode(sessionId, Properties.getString("process"));		
	}
	

	/**
	 * Adds an agent to the provenance graph. It should be linked to the process later.
	 * @return URI of the new artifact.
	 */
	public  String addAgent(String sessionId){
		return addNode(sessionId, Properties.getString("agent"));
	}

	
	/**
	 * Adds an artefact to the provenance graph. It should be linked to the process later.
	 * @return URI of the new artifact.
	 */
	public  String addArtifact(String sessionId){
		return addNode(sessionId,Properties.getString("artifact"));
	}
	

	/**
	 * Adds title to the given object.
	 * @param sessionId Process
	 * @param object Object to receive the title 
	 * @param title Title.
	 */
	public  String addTitle(String sessionId, String object, String title){
		//System.out.println(sessionId+","+object+","+title);
		Model model = sessions.get(sessionId);
		if(model == null)
			return "Error - no session "+sessionId;
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
	public  String addJSONGraph(String sessionId, String jsonGraph){
		JSONArray nodes = (JSONArray) JSONSerializer.toJSON(jsonGraph);
		
		Graph g = JSONProvider.getGraph(null, nodes);
		sessions.get(sessionId).add(RDFProvider.getGraphModel(g));
		return "ok";
	}
	
	
	public  String addRDFGraph(String sessionId, Model input){
		Model model = sessions.get(sessionId);
		model.add(input);
		return "ok";
	}
	public  String addRDFGraph(String sessionId, String input){
		Model model = sessions.get(sessionId);
		model.read(new StringReader(input), null);
		return "ok";
	}
	/**
	 * Adds relationship to the process. Causal relationships are e.g. controlledBy, Used, wasGeneratedBy,...
	 * @param sessionId URI of the process to be the relation added to.
	 * @param type The type of the causal relationship.
	 * @param from The subject of the relationship - this can be artifact, agent, or other process.
	 * @param to The object of the relationship - this can be artifact, agent, or other process.
	 * @return
	 */
	public  String addCausalRelationship(String sessionId,String type, String from,  String to){		
		Model model = sessions.get(sessionId);
		String relationId = namespace + UUID.randomUUID().toString();

		if(model == null)
			return "Error - no session "+sessionId;
		Resource relationship = model.createResource(relationId);
		Resource c = model.getResource(from);
		Resource e = model.getResource(to);
		Resource r = model.getResource(type);
		model.add(relationship, RDF.type, r);
		model.add(relationship, Utility.getProp("from"), c);
		model.add(relationship, Utility.getProp("to"), e);
		return relationship.getURI();
	}
	
	public  DataProvider getDataProvider() {
		return dataProvider;
	}

	public  void setDataProvider(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 * @param sessionId
	 * @param uri
	 * @return
	 */
	public  String addExistingResource(String sessionId, String uri){	
		Node n;
		try {
			n = dataProvider.getNode(null, uri);
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
	public  String addExistingResource(String sessionId, String uri, String type, String title){		
		Model model = sessions.get(sessionId);
		if(model == null)
			return "Error - no session "+sessionId;
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
	public  JSONArray getJSONGraph(String sessionId){
		Graph g = getGraph(sessionId);
		return JSONProvider.getGraphJSON(g);
	}
	/**
	 * Returns the Graph associated with the given sessionId.
	 * @param sessionId
	 * @return
	 * @throws OpenRDFException 
	 */
	public  Graph getGraph(String sessionId) {
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
	public  Model getModel(String sessionId){
		return sessions.get(sessionId);		
	}
	
	/**
	 * Removes the given relationship from the model.
	 * @param sessionId
	 * @param relationship URI of the relationship.
	 * @return
	 */
	public  String removeCausalRelationShip(String sessionId, String relationship){
		Model model = sessions.get(sessionId);
		Resource relation = model.getResource(relationship);
		
		if(model.contains(relation, RDF.type))
			model.remove(relation.listProperties());
		else{
			if(sessionsDelete.get(sessionId) == null)
				sessionsDelete.put(sessionId, new ArrayList<String>());
			sessionsDelete.get(sessionId).add(relationship);
		}
		return "ok";
	}
	/**
	 * Removes the given node from the model.
	 * @param sessionId
	 * @param node
	 * @return
	 */
	public  String removeNode(String sessionId, String node){
		Model model = sessions.get(sessionId);
		Resource res = model.getResource(node);
		if(model.contains(res, RDF.type))
			model.remove(res.listProperties());
		else{
			if(sessionsDelete.get(sessionId) == null)
				sessionsDelete.put(sessionId, new ArrayList<String>());
			sessionsDelete.get(sessionId).add(node);
		}
		return "ok";
	}
	

	/**
	 * Cleans the RDF model, leaving only the provenance stuff in the model.
	 * @param sessionId
	 * @return
	 */
	public  String clean(String sessionId){
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
	public  String commit(String sessionId) throws OpenRDFException, IOException {
		Model m = sessions.get(sessionId);
		if(m == null)
			return "Error - no session "+sessionId;
		try {
			dataProvider.write(m);
			if(sessionsDelete.get(sessionId) != null){
				dataProvider.delete(sessionsDelete.get(sessionId));
			}
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
	public  void rollback(String sessionId){
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
	public  Graph getProvenance(String resource, String sessionId) throws OpenRDFException{
		Graph prov = getImmediateProvenance(resource, sessionId);
		Graph provAll = prov;
		for(Node n:prov.getNodes()){
			//Load the provenance only of processes
			try {
				if(!"Process".equals(getShape(n.getType())))
						continue;
				Graph prov2 = getImmediateProvenance(n.getId(), sessionId);
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
	public  Graph getProvenanceFrom(String resourceID, String sessionId) throws OpenRDFException{		
		Graph g= new Graph();
		Graph g2 = new Graph();
		if(sessions.containsKey(sessionId) && sessions.get(sessionId) != null)
			g2 = RDFProvider.getModelGraph(sessions.get(sessionId));
		Node n = dataProvider.getNode(g2, resourceID);
		g.addNode(n);
		dataProvider.getAdjacencies(g2, n, 1);
		return g;
	}
	/**
	 * Gets the edges to the given resource.
	 * @param resourceID
	 * @return Graph containing the given resource and all edges directing to it.
	 * @throws OpenRDFException
	 */
	public  Graph getProvenanceTo(String resourceID, String sessionId) throws OpenRDFException{		
		Graph g = new Graph();
		Graph g2 = new Graph();
		if(sessions.containsKey(sessionId) && sessions.get(sessionId) != null)
			g2 = RDFProvider.getModelGraph(sessions.get(sessionId));
		Node n = dataProvider.getNode(g2, resourceID);
		g.addNode(n);
		dataProvider.getAdjacencies(g2, n, 0);
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
	public  Graph getImmediateProvenance(String resource, String sessionId) throws OpenRDFException {
		Graph l = getProvenanceTo(resource, sessionId);
		l = l.merge(getProvenanceFrom(resource, sessionId));
		for (int i = 0; i < l.size(); i++) {
			Node n = l.get(i);
			for(Edge e : n.getAdjacencies()){
				if(!l.contains(e.getTo())){
					l.addNode(e.getTo());
					e.getTo().addAdjacency(e);
				}
				if(!l.contains(e.getFrom())){
					l.addNode(e.getFrom());
					e.getFrom().addAdjacency(e);
				}
			}
		}
		return l;
	}
	public  Map<String, String> getBasicTypes() {
		return basicTypes;
	}

	public  void setBasicTypes(Map<String, String> basicTypes) {
		this.basicTypes = basicTypes;
	}

	public  void setSessions(Map<String, Model> sessions) {
		this.sessions = sessions;
	}
	
	public  String getNamespace() {
		return namespace;
	}

	public  void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public  List<String> getProperties() {
		return properties;
	}

	public  void setProperties(List<String> properties) {
		this.properties = properties;
	}

	public  List<String> getNodes() {
		return nodes;
	}

	public  void setNodes(List<String> nodes) {
		this.nodes = nodes;
	}
}
