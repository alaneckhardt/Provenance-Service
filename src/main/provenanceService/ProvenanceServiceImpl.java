package provenanceService;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import provenanceService.provenanceModel.DataProvider;
import provenanceService.provenanceModel.JSONProvider;
import provenanceService.provenanceModel.ProvenanceModel;
import provenanceService.provenanceModel.RDFProvider;
import provenanceService.provenanceModel.SPARQLProvider;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

// TODO: Auto-generated Javadoc
/** Provenance service can stored the given provenance data in the database. The
 * primary interface is JSON object of this structure:<br>
 *
 * //Node<br>
 * {<br>
 * &nbsp;&nbsp;//Edges<br>
 * &nbsp;&nbsp;"adjacencies": [<br>
 * &nbsp;&nbsp;&nbsp; {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "id":
 * "http://www.policygrid.org/ourspacesVRE.owl#1efaab5a-f9bf-4356-921f-e58753d21a99"
 * ,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "to":
 * "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "from":
 * "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "type": "http://openprovenance.org/ontology#Used",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "typetext": "Used"&nbsp;//Used for display<br>
 * &nbsp;&nbsp;&nbsp; },&nbsp;<br>
 * &nbsp;&nbsp;&nbsp; {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "id":
 * "http://www.policygrid.org/ourspacesVRE.owl#1ee85774-cb9a-4e1e-85fe-e78988cd1b3a"
 * ,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "to":
 * "http://openprovenance.org/ontology#030210c9-4fd0-4c12-971f-659c07eac9ec",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "from":
 * "http://openprovenance.org/ontology#dbd1f00b-0772-42af-8d68-5694abbe225a",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "type":
 * "http://openprovenance.org/ontology#WasGeneratedBy",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "typetext": "WasGeneratedBy"&nbsp;//Used for display<br>
 * &nbsp;&nbsp;&nbsp; }<br>
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
 * &nbsp;&nbsp;&nbsp; {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "name":
 * "http://www.policygrid.org/ourspacesVRE.owl#timestamp",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "value": "123456789"<br>
 * &nbsp;&nbsp;&nbsp; },&nbsp;<br>
 * &nbsp;&nbsp;&nbsp; {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "name":
 * "http://www.policygrid.org/ourspacesVRE.owl#anything",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; "to": "http://openprovenance.org/ontology#anything"<br>
 * &nbsp;&nbsp;&nbsp; }<br>
 * &nbsp;&nbsp;],<br>
 * &nbsp;}&nbsp;&nbsp;&nbsp;<br>
 *
 * @author Alan Eckhardt a.e@centrum.cz 
 * * @version 1.0 */
public class ProvenanceServiceImpl {
	/** Index of last session. */
	private int lastSession = 0;

	/** Namespace of new URIs. */
	private String namespace;

	/** Contains RDF model for URI of processes. Used for validation of
	 * correctness of the model. The model is flushed into RDF repository in the
	 * commit method. */
	private Map<String, Model> sessions = null;
	/** List of the resources to delete. */
	private Map<String, Model> sessionsDelete = null;
	/** Contains mapping from subclasses to Agent, Artifact, Process. */
	private Map<String, String> basicTypes = new HashMap<String, String>();
	/**Types of edges.*/
	private List<String> properties = new ArrayList<String>();
	/**Types of nodes.*/
	private List<String> nodes = new ArrayList<String>();
	/** Creates instances of a specific provenance model, such as PROV-O or OPM.*/
	private ProvenanceModel provProvider;

	/**
	 * Public constructor.
	 */
	public ProvenanceServiceImpl() {
		initProvenance();
	}
	/**
	 * Initialises the ontologies.
	 */
	public void initProvenance() {
		if (sessions != null)
			return;

		namespace = Properties.getString("namespace");
        try {
    		//provProvider = Class.forName(Properties.getString("ProvenanceModel"));
            Class<?> cls = Class.forName(Properties.getString("ProvenanceModel"));
            Class<?>[] partypes = new Class[1];
            partypes[0] = ProvenanceServiceImpl.class;
            Constructor<?> ct = cls.getConstructor(partypes);
            Object[] arglist = new Object[1];
            arglist[0] = this;
			provProvider = (ProvenanceModel) ct.newInstance(arglist);
			getRDFProvider().init();
			getJSONProvider().init();
			getDataProvider().init(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sessions = new HashMap<String, Model>();

		sessionsDelete = new HashMap<String, Model>();
		initNodes();
		initEdges();
	}

	/**
	 * Loads edge types from ontologies.
	 */
	public void initEdges() {

		properties = new ArrayList<String>();
		Vector<String> subClasses = getDataProvider().getSubclasses(Properties.getString("edge"));
		properties.addAll(subClasses);
		for (int i = 0; i < properties.size(); i++) {
			subClasses = getDataProvider().getSubclasses(properties.get(i));
			properties.addAll(subClasses);
		}
	}
	/**
	 * Loads node types from ontologies.
	 */
	public void initNodes() {
		getBasicTypes().clear();
		getBasicTypes().put(Properties.getString("process"), "Process");
		getBasicTypes().put(Properties.getString("agent"), "Agent");
		getBasicTypes().put(Properties.getString("artifact"), "Artifact");
		Vector<String> subClasses;
		nodes = new ArrayList<String>();
		// Agents subclasses
		nodes.add(Properties.getString("agent"));
		subClasses = getDataProvider().getSubclasses(Properties.getString("agent"));
		nodes.addAll(subClasses);
		for (int i = 0; i < subClasses.size(); i++) {
			getBasicTypes().put(subClasses.get(i), "Agent");
		}

		// Artifacts subclasses
		nodes.add(Properties.getString("artifact"));
		subClasses = getDataProvider().getSubclasses(Properties.getString("artifact"));
		nodes.addAll(subClasses);
		for (int i = 0; i < subClasses.size(); i++) {
			getBasicTypes().put(subClasses.get(i), "Artifact");
		}

		// Processes subclasses
		nodes.add(Properties.getString("process"));
		subClasses = getDataProvider().getSubclasses(Properties.getString("process"));
		nodes.addAll(subClasses);
		for (int i = 0; i < subClasses.size(); i++) {
			getBasicTypes().put(subClasses.get(i), "Process");
		}
	}

	/**
	 * Returns the shape for given type.
	 *
	 * @param type Type of the resource.
	 * @return the shape
	 */
	public String getShape(final String type) {
		return getBasicTypes().get(type);
	}

	/**
	 * Gets the sessions.
	 *
	 * @return All the processes in the map.
	 */
	public String[] getSessions() {
		String[] keys = new String[sessions.keySet().size()];
		keys = sessions.keySet().toArray(keys);
		return keys;
	}

	/**
	 * Gets the sessions map.
	 *
	 * @return The map of processes.
	 */
	public Map<String, Model> getSessionsMap() {
		return sessions;
	}

	/** Starts the new session of creating provenance. Return sessionId.
	 *
	 * @return Id of the session. */
	public String startSession() {
		initProvenance();
		Model model = ModelFactory.createDefaultModel();
		model.createProperty(Properties.getString("title"));
		for (final String s : nodes) {
			Resource r = model.createResource(s);
			model.add(r, RDF.type, OWL.Class);
		}
		// Strange but true, properties in OPM are classes, too.
		for (final String s : properties) {
			Resource r = model.createResource(s);
			model.add(r, RDF.type, OWL.Class);
		}
		String id = "" + lastSession;
		sessions.put(id, model);
		lastSession++;
		return id;
	}

	/**
	 * Adds a custom property to the given resource.
	 *
	 * @param sessionId Id of the session.
	 * @param node Subject
	 * @param property Property
	 * @param value Value
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public void addCustomProperty(final String sessionId, final String node, final String property, final String value) throws ProvenanceServiceException {
		Model model = sessions.get(sessionId);
		if (model == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
		Resource res = model.createResource(node);
		Property p = model.getProperty(property);
		IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		boolean includeWarnings = false;
		IRI iri;
		iri = iriFactory.create(value);
		// Literal
		if (iri.hasViolation(includeWarnings))
			res.addProperty(p, value);
		// Resource
		else
			res.addProperty(p, model.createResource(value));

	}

	/**
	 * Gets the new uri.
	 *
	 * @return the new uri
	 */
	public String getNewURI(){
		return namespace + UUID.randomUUID().toString();
	}

	/**
	 * Create an instance of given type.
	 *
	 * @param sessionId Id of the session.
	 * @param type Type of the node
	 * @return URI of the new resource.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public String addNode(final String sessionId, final String type) throws ProvenanceServiceException {
		Model model = sessions.get(sessionId);
		if (model == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
		return provProvider.addNode(model, type);
	}

	/**
	 * Starts the process of creating provenance. Return rdfId of the new
	 * process.
	 *
	 * @param sessionId Id of the session.
	 * @return URI of the new process.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public String addProcess(final String sessionId) throws ProvenanceServiceException {
		return addNode(sessionId, Properties.getString("process"));
	}

	/**
	 * Adds an agent to the provenance graph. It should be linked to the process
	 * later.
	 *
	 * @param sessionId Id of the session.
	 * @return URI of the new artifact.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public String addAgent(final String sessionId) throws ProvenanceServiceException {
		return addNode(sessionId, Properties.getString("agent"));
	}

	/**
	 * Adds an artefact to the provenance graph. It should be linked to the
	 * process later.
	 *
	 * @param sessionId Id of the session.
	 * @return URI of the new artifact.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public String addArtifact(final String sessionId) throws ProvenanceServiceException {
		return addNode(sessionId, Properties.getString("artifact"));
	}

	/**
	 * Adds title to the given object.
	 *
	 * @param sessionId Id of the session.
	 * @param object Object to receive the title
	 * @param title Title.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public void addTitle(final String sessionId, final String object, final String title) throws ProvenanceServiceException {
		Model model = sessions.get(sessionId);
		if (model == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
		Resource res = model.getResource(object);
		Property p = model.getProperty(Properties.getString("title"));
		if (p == null) {
			model.createProperty(Properties.getString("title"));
			p = model.getProperty(Properties.getString("title"));
		}
		model.add(res, p, title);
	}

	/**
	 * Adds whole provenance graph to the given session.
	 *
	 * @param sessionId Id of the session.
	 * @param jsonGraph JSON string to be inserted.
	 */
	public void addJSONGraph(final String sessionId, final String jsonGraph) {
		JSONArray nodes = (JSONArray) JSONSerializer.toJSON(jsonGraph);

		Graph g = getJSONProvider().getGraph(nodes);
		sessions.get(sessionId).add(getRDFProvider().getGraphModel(g));
	}

	/**
	 * Adds the rdf graph.
	 *
	 * @param sessionId Id of the session.
	 * @param input Model to be inserted.
	 */
	public void addRDFGraph(final String sessionId, final Model input) {
		Model model = sessions.get(sessionId);
		model.add(input);
	}

	/**
	 * Adds the rdf graph.
	 *
	 * @param sessionId Id of the session.
	 * @param input String of RDF data to be inserted.
	 */
	public void addRDFGraph(final String sessionId, final String input) {
		Model model = sessions.get(sessionId);
		model.read(new StringReader(input), null);
	}

	/**
	 * Adds relationship to the process. Causal relationships are e.g.
	 * controlledBy, Used, wasGeneratedBy,...
	 *
	 * @param sessionId Id of the session.
	 * URI of the process to be the relation added to.
	 * @param type The type of the causal relationship.
	 * @param from The subject of the relationship - this can be artifact, agent,
	 * or other process.
	 * @param to The object of the relationship - this can be artifact, agent,
	 * or other process.
	 * @return URI of the new relationship.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public String addCausalRelationship(final String sessionId, final String type, final String from, final String to) throws ProvenanceServiceException {
		Model model = sessions.get(sessionId);

		if (model == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
		return provProvider.addCausalRelationship(model, type, from, to);
	}

	/**
	 * Gets the data provider.
	 *
	 * @return dataprovider.
	 */
	public DataProvider getDataProvider() {
		return provProvider.getDataProvider();
	}

	/**
	 * Gets the rDF provider.
	 *
	 * @return RDFprovider.
	 */
	public RDFProvider getRDFProvider() {
		return provProvider.getRDFProvider();
	}

	/**
	 * Gets the jSON provider.
	 *
	 * @return JSONprovider.
	 */
	public JSONProvider getJSONProvider() {
		return provProvider.getJSONProvider();
	}

	/**
	 * Gets the sPARQL provider.
	 *
	 * @return SPARQLprovider.
	 */
	public SPARQLProvider getSPARQLProvider() {
		return provProvider.getSPARQLProvider();
	}


	/**
	 * Sets the data provider.
	 *
	 * @param dataProvider Dataprovider to use.
	 */
	public void setDataProvider(final DataProvider dataProvider) {
		provProvider.setDataProvider(dataProvider);
	}

	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 *
	 * @param sessionId Id of the session.
	 * @param uri URI of the resource
	 * @throws ProvenanceServiceException ex
	 */
	public void addExistingResource(final String sessionId, final String uri) throws ProvenanceServiceException {
		Node n;
			n = getDataProvider().getNode(null, uri);
			addExistingResource(sessionId, uri, n.getType(), n.getTitle());
	}

	/**
	 * Adds an existing resource from the RDF store to the session graph.
	 *
	 * @param sessionId Id of the session.
	 * @param uri URI of the resource
	 * @param type Type of the resource
	 * @param title Title of the resource - for the visualisation purpose.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public void addExistingResource(final String sessionId, final String uri, final String type, final String title) throws ProvenanceServiceException {
		Model model = sessions.get(sessionId);
		if (model == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
		model.add(model.getResource(uri), RDF.type, model.getResource(type));
		if (title != null && title.length() > 0)
			addTitle(sessionId, uri, title);
	}

	/**
	 * Returns the JSON representation of the Graph associated with the given
	 * sessionId.
	 *
	 * @param sessionId Id of the session.
	 * @return the jSON graph
	 */
	public JSONArray getJSONGraph(final String sessionId) {
		Graph g = getGraph(sessionId);
		return getJSONProvider().getGraphJSON(g);
	}

	/** Returns the Graph associated with the given sessionId.
	 *
	 * @param sessionId Id of the session
	 * @return Graph associated with the given sessionId
	 */
	public Graph getGraph(final String sessionId) {
		Model model = sessions.get(sessionId);
		if (model == null)
			return null;
		/* Graph g = new Graph();
		 * //TODO verify the correctness of this
		 * for(final String nodeType : nodes){
		 * ResIterator it = model.listResourcesWithProperty(RDF.type,
		 * model.getResource(nodeType));
		 * while(it.hasNext()){
		 * Resource r = it.next();
		 * g.addNode(RDFProvider.getNode(g, r));
		 * }
		 * }
		 * for(Node n : g.getNodes()){
		 * RDFProvider.getAdjacencies(g, n, 2);
		 * } */
		return getRDFProvider().getModelGraph(model);

	}

	/**
	 * Returns the RDF model associated with the given sessionId.
	 *
	 * @param sessionId Id of the session.
	 * @return the model
	 */
	public Model getModel(final String sessionId) {
		return sessions.get(sessionId);
	}

	/** Removes the given relationship from the model.
	 *
	 * @param sessionId Id of the session.
	 * @param relationship
	 *            URI of the relationship.
	 */
	public void removeCausalRelationShip(final String sessionId, final String relationship) {
		Model model = sessions.get(sessionId);
		Resource relation = model.getResource(relationship);

		if (model.contains(relation, RDF.type))
			model.remove(relation.listProperties());
		else {
			if (sessionsDelete.get(sessionId) == null)
				sessionsDelete.put(sessionId,  ModelFactory.createDefaultModel());
			Edge e;
				e = getDataProvider().getEdge(null, relationship);
				sessionsDelete.get(sessionId).add(getRDFProvider().getEdgeModel(e));
		}
	}

	/**
	 * Gets the prov provider.
	 *
	 * @return the prov provider
	 */
	public ProvenanceModel getProvProvider() {
		return provProvider;
	}

	/**
	 * Sets the prov provider.
	 *
	 * @param provProvider the new prov provider
	 */
	public void setProvProvider(final ProvenanceModel provProvider) {
		this.provProvider = provProvider;
	}
	/** Removes the given node from the model.
	 *
	 * @param sessionId Id of the session.
	 * @param node URI of the resource.
	 */
	public void removeNode(final String sessionId, final String node) {
		Model model = sessions.get(sessionId);
		Resource res = model.getResource(node);
		if (model.contains(res, RDF.type))
			model.remove(res.listProperties());
		else {
			if (sessionsDelete.get(sessionId) == null)
				sessionsDelete.put(sessionId, ModelFactory.createDefaultModel());
			Node n;
				n = getDataProvider().getNode(null, node);
				sessionsDelete.get(sessionId).add(getRDFProvider().getNodeModel(n));
		}
	}

	/**
	 * Cleans the RDF model, leaving only the provenance stuff in the model.
	 *
	 * @param sessionId Id of the session.
	 * @throws ProvenanceServiceException the provenance service exception
	 */
	public void clean(final String sessionId) throws ProvenanceServiceException {
		try {
			Model m = sessions.get(sessionId);
			Graph g = getGraph(sessionId);
			m.removeAll();
			m.add(getRDFProvider().getGraphModel(g));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ProvenanceServiceException("Error " + e.getLocalizedMessage());
		}
	}

	/**
	 * Commit.
	 *
	 * @param sessionId Id of the session.
	 * @throws ProvenanceServiceException When the RDF model of the given process is bad or does not
	 * satisfy policies.
	 */
	public void commit(final String sessionId) throws ProvenanceServiceException {
		Model m = sessions.get(sessionId);
		if (m == null)
			throw new ProvenanceServiceException("Error - no session " + sessionId);
			getDataProvider().write(m);
			if (sessionsDelete.get(sessionId) != null) {
				getDataProvider().delete(sessionsDelete.get(sessionId));
			}
		// All ok, discard the model.
		m.close();
		sessions.remove(sessionId);
		// TODO policies handling and validation
	}

	/** Deletes the RDF model of the given process.
	 *
	 * @param sessionId Id of the session.
	 *            URI of the process */
	public void rollback(final String sessionId) {
		sessions.remove(sessionId);
		// TODO perform rollback
	}

	/**
	 * Finds the provenance graph near the given resource.
	 * The edges to and from the resource are obtained. If a process is among
	 * the neighoring nodes, then also its edges are obtained.
	 *
	 * @param resource URI of the resource
	 * @param sessionId Id of the session.
	 * @return Provenance graph near the given resource.
	 */
	public Graph getProvenance(final String resource, final String sessionId)  {
		Graph prov = getImmediateProvenance(resource, sessionId);
		Graph provAll = prov;
		for (Node n : prov.getNodes()) {
			// Load the provenance only of processes
			try {
				if (!"Process".equals(getShape(n.getType())))
					continue;
				Graph prov2 = getImmediateProvenance(n.getId(), sessionId);
				provAll = provAll.merge(prov2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return provAll;
	}

	/** Gets the edges from the given resource.
	 *
	 * @param resourceID URI of the resource
	 * @param sessionId Id of the session.
	 * @return Graph containing the given resource and all edges directing from
	 *         it. */
	public Graph getProvenanceFrom(final String resourceID, final String sessionId)  {
		Graph g = new Graph();
		Graph g2 = new Graph();
		if (sessions.containsKey(sessionId) && sessions.get(sessionId) != null)
			g2 = getRDFProvider().getModelGraph(sessions.get(sessionId));
		Node n = getDataProvider().getNode(g2, resourceID);
		g.addNode(n);
		getDataProvider().getAdjacencies(g2, n, 1);
		return g;
	}

	/** Gets the edges to the given resource.
	 *
	 * @param resourceID URI of the resource
	 * @param sessionId Id of the session.
	 * @return Graph containing the given resource and all edges directing to
	 *         it. */
	public Graph getProvenanceTo(final String resourceID, final String sessionId)  {
		Graph g = new Graph();
		Graph g2 = new Graph();
		if (sessions.containsKey(sessionId) && sessions.get(sessionId) != null)
			g2 = getRDFProvider().getModelGraph(sessions.get(sessionId));
		Node n = getDataProvider().getNode(g2, resourceID);
		g.addNode(n);
		getDataProvider().getAdjacencies(g2, n, 0);
		return g;
	}

	/**
	 * Returns the graph immediately around the given resource. Data are
	 * obtained from the underlying RDF repository.
	 *
	 * @param resource URI of the resource, whose provenance will be loaded.
	 * @param sessionId Id of the session.
	 * @return only the edges to and from the given resource.
	 */
	public Graph getImmediateProvenance(final String resource, final String sessionId)  {
		Graph l = getProvenanceTo(resource, sessionId);
		l = l.merge(getProvenanceFrom(resource, sessionId));
		for (int i = 0; i < l.size(); i++) {
			Node n = l.get(i);
			for (Edge e : n.getAdjacencies()) {
				if (!l.contains(e.getTo())) {
					l.addNode(e.getTo());
					e.getTo().addAdjacency(e);
				}
				if (!l.contains(e.getFrom())) {
					l.addNode(e.getFrom());
					e.getFrom().addAdjacency(e);
				}
			}
		}
		return l;
	}

	/**
	 * Gets the basic types.
	 *
	 * @return the basic types
	 */
	public Map<String, String> getBasicTypes() {
		return basicTypes;
	}

	/**
	 * Sets the basic types.
	 *
	 * @param basicTypes the basic types
	 */
	public void setBasicTypes(final Map<String, String> basicTypes) {
		this.basicTypes = basicTypes;
	}

	/**
	 * Sets the sessions.
	 *
	 * @param sessions the sessions
	 */
	public void setSessions(final Map<String, Model> sessions) {
		this.sessions = sessions;
	}

	/**
	 * Gets the namespace.
	 *
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Sets the namespace.
	 *
	 * @param namespace the new namespace
	 */
	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public List<String> getProperties() {
		return properties;
	}

	/**
	 * Sets the properties.
	 *
	 * @param properties the new properties
	 */
	public void setProperties(final List<String> properties) {
		this.properties = properties;
	}

	/**
	 * Gets the nodes.
	 *
	 * @return the nodes
	 */
	public List<String> getNodes() {
		return nodes;
	}

	/**
	 * Sets the nodes.
	 *
	 * @param nodes the new nodes
	 */
	public void setNodes(final List<String> nodes) {
		this.nodes = nodes;
	}
}
