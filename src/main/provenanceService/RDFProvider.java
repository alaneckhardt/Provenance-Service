package provenanceService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * Class providing functions for manipulation with RDF. Conversions between Graph and RDF and querying the underlying RDF repository as well.
 * @author AE
 *
 */
public class RDFProvider {
	private static RepositoryConnection con;
	private static OntModel ontologies;
	/**List of custom properties to load.*/
	private static List<String> customProperties;
	@SuppressWarnings("unchecked")
	public static void init(){
		System.setProperty("http.proxyHost", Properties.getString("proxyhost"));
		System.setProperty("http.proxyPort", Properties.getString("proxyport"));
		//customProperties = new ArrayList<String>();
		customProperties = Properties.getValues().getList("customProperties");
		
		String path = Properties.getString("ontologiesDirectory");
		path = Properties.getBaseFolder()+path;
		//File d = new File(path);
		/*String[] onts = d.list(new FilenameFilter() {			
			public boolean accept(File arg0, String name) {
				return name.endsWith(".owl"); 
			}
		});*/
		List<String> ontologiesNames = Properties.getValues().getList("ontologies");		
		ontologies = ModelFactory.createOntologyModel();
		if(ontologiesNames != null){
			for(String s : ontologiesNames){
				Model m = ModelFactory.createOntologyModel();
				FileInputStream in;
				try {
					in = new FileInputStream(path+s);
					m = (OntModel) m.read(in,"");
					in.close();
					ontologies.add(m);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	public static void write(Model m) throws OpenRDFException, IOException {
		connect();
		//TODO policies handling and validation
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.write(out);
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		getCon().add(in, Properties.getString("url"), RDFFormat.RDFXML);
		disconnect();		
	}
	

	public static Vector<String> getSubclasses(String type) {
		Vector<String> classList = new Vector<String>();
		OntClass c = ontologies.getOntClass(type);
		if(c == null)
			return classList;
		try {
			//Do it write because of iterators possible conflict.
			ontologies.enterCriticalSection(Lock.WRITE);
			for (Iterator<OntClass> it = c.listSubClasses(false); it.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection() ;
		}
		return classList;
	}

	public static Vector<String> getSuperclasses( String resourceType) {
		Vector<String> classList = new Vector<String>();
		//Do it write because of iterators possible conflict.
		ontologies.enterCriticalSection(Lock.WRITE);
		try {
			OntClass c = ontologies.getOntClass(resourceType);
			for (Iterator<OntClass> it = c.listSuperClasses(false); it.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection() ;
		}
		return classList;
	}

	
	/**
	 * Returns iterator over all restrictions that are superclasses of given class.
	 * @param label Label of ontology to be searched.
	 * @param oclass Name of the class.
	 * @return Iterator over all restrictions that are superclasses of given class.
	 */
	public static Iterator<Restriction> getRestrictionsOnClass(String oclass){
		OntClass c = ontologies.getOntClass(oclass);		
		ExtendedIterator<OntClass> l = c.listSuperClasses();
		List<Restriction> restr = new ArrayList<Restriction>();
		System.out.println(oclass);
		while(l.hasNext()){
			OntClass sc = l.next();
			//We take only restrictions
			if(sc.isRestriction()){
				Restriction r = sc.asRestriction();
				restr.add(r);
			}
		}		
		return restr.iterator();
	}

	private static String getEntityDescription(String resource) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		//For persons, we have to get the name instead of the title.
		if(ProvenanceService.getBasicTypes()!= null && "Agent".equals(ProvenanceService.getShape(getProperty(resource, Properties.getString("type"))))){
			String firstname = getProperty(resource, Properties.getString("firstname"));
			String surname = getProperty(resource, Properties.getString("surname"));
			String fullname = firstname + " " + surname;
			return fullname;
		}
		else
			return getProperty(resource, Properties.getString("title"));
	}
	/**
	 * Loads the adjacencies of the given node
	 * @param g
	 * @param n
	 * @param to 0=to,1=from,2=both
	 */
	public static void getAdjacencies(Graph g, Node n, int to){		
		try {
			List<String> adjacencies = null;
			if(to==0)
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("cause"));
			else if(to==1)
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("effect"));
			else if(to==2){
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("cause"));
				adjacencies.addAll(getPropertiesTo(n.getId(), Properties.getString("effect")));
			}
			else
				return;
			for(String s : adjacencies){
				Edge e = getEdge(g, s);
				if(e != null && e.getTo() != null && e.getFrom() != null)
					n.getAdjacencies().add(e);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Loads the custom properties from the RDF repository.
	 * @param n
	 * @throws OpenRDFException
	 */
	protected static void loadCustomProperties(Node n, Resource res) throws OpenRDFException{		
		for (String prop : customProperties) {			
			String val = null;
			if(res == null)
				val = getProperty(n.getId(), prop);
			else{
				Statement t = res.getProperty(res.getModel().getProperty(prop));
				if(t != null)
					val = t.getString();
			}
			if(val != null && !("".equals(val)))
				n.addProperty(prop, val);
		}
	}
	/**
	 * Gets the properties of the node from RDF repository. Does not load the adjacencies though, in order to avoid greedy crawl of the whole graph.
	 * @param g
	 * @param resource
	 * @return The Node with filled properties without the adjacencies.
	 * @throws OpenRDFException 
	 */
	public static Node getNode(Graph g, String resource) throws OpenRDFException{		
		Node node = null;
		if(resource == null || resource.equals(""))
			return null;
		if(g != null)
			node = g.getNode(resource);
		//Node is in the graph
		if(node != null)
			return node;
		
		node = new Node(resource);
		node.setType(getProperty(resource, Properties.getString("type")));
		node.setTitle(getEntityDescription(resource));
		node.setBasicType(ProvenanceService.getShape(node.getType()));
		node.setAdjacencies(new ArrayList<Edge>());
		loadCustomProperties(node, null);	
		//if(node.getType() == null || node.getType().equals(""))
		//	return null;
		return node;			
	}
	
	/**
	 * Return Node from given Resource
	 * @param g
	 * @param res
	 * @return
	 */
	public static Node getNode(Graph g, Resource res){		
		Node node = null;
		if(res == null)
			return null;
		if(g != null)
			node = g.getNode(res.getURI());
		//Node is in the graph
		if(node != null)
			return node;
		
		node = new Node(res.getURI());
		Statement t = res.getProperty(res.getModel().getProperty(Properties.getString("title")));
		if(t != null)
			node.setTitle(t.getString());
		t = res.getProperty(RDF.type);
		if(t != null){
			node.setType(t.getObject().toString());
			node.setBasicType(ProvenanceService.getShape(node.getType()));
		}
		try {
			loadCustomProperties(node, res);
		} catch (OpenRDFException e) {
			//This shoudln't happen at all.
			e.printStackTrace();
		}	
		return node;			
	}
	
	/**
	 * Gets the Edge with identifier from given resource.
	 * @param g
	 * @param edge
	 * @return
	 * @throws OpenRDFException
	 */
	public static Edge getEdge(Graph g, Resource edge) throws OpenRDFException {
		Edge e = new Edge(edge.getURI());
		Statement t = edge.getProperty(RDF.type);
		if(t != null)
			e.setType(t.getObject().toString());
		
		if (g != null)
			e.setFrom(g.getNode(edge.getProperty(getProp("cause")).getResource().getURI()));
		if (e.getFrom() == null) {
			e.setFrom(getNode(g, edge.getProperty(getProp("cause")).getResource()));
		}
		
		if (g != null)
			e.setTo(g.getNode(edge.getProperty(getProp("effect")).getResource().getURI()));
		if (e.getTo() == null) {
			e.setTo(getNode(g, edge.getProperty(getProp("effect")).getResource()));
		}
		
		//Add the edge only if everything's all right
		if(e.getFrom() != null && e.getTo() != null){
			e.getFrom().getAdjacencies().add(e);
			e.getTo().getAdjacencies().add(e);
		}
		else 
			return null;
		return e;
	}
		
	/**
	 * Finds the edge in the RDF repository. 
	 * @param g
	 * @param resource
	 * @return New Edge object.
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public static Edge getEdge(Graph g, String resource) throws OpenRDFException{
		if(resource == null || resource.equals(""))
			return null;
		Edge edge = new Edge(resource);
		edge.setType(getProperty(resource, Properties.getString("type")));
		String from = getProperty(resource, Properties.getString("effect"));
		if (g != null)
			edge.setFrom(g.getNode(from));
		if (edge.getFrom() == null) {
			edge.setFrom(getNode(g, from));
		}

		String to = getProperty(resource, Properties.getString("cause"));
		if (g != null)
			edge.setTo(g.getNode(to));
		if (edge.getTo() == null) {
			edge.setTo(getNode(g, to));
		}
		if ((edge.getType() == null || edge.getType().equals(""))
				&& (edge.getFrom() == null || edge.getTo() == null))
			return null;
		return edge;			
	}
	/**
	 * Return the RDF representation of a node.
	 * @param n
	 * @return
	 */
	public static Model getNodeModel(Node n){
		Model m = ModelFactory.createDefaultModel();
		Resource r = m.createResource(n.getId());
		if(n.getType() != null)
			m.add(r, RDF.type, m.createResource(n.getType()));
		if(n.getTitle() != null)
			m.add(r, getProp("title"), n.getTitle());
		for (String prop : n.getProperties().keySet()) {
			Property p = m.getProperty(prop);
			Object value = n.getProperties().get(prop);
			IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
			boolean includeWarnings = false;
			IRI iri;
			iri = iriFactory.create(value.toString()); 
			//Literal
			if (iri.hasViolation(includeWarnings)) 
				r.addProperty(p, value.toString());
			//Resource
			else
				r.addProperty(p, m.createResource(value.toString()));
			
		}
		for (Edge e : n.getAdjacencies()) {
			Resource edge = m.createResource(e.getId());
			if(e.getType() != null)
				m.add(edge, RDF.type, m.createResource(e.getType()));
			
			Resource n1 = m.createResource(e.getFrom().getId());
			Resource n2 = m.createResource(e.getTo().getId());
			if(e.getFrom().getType() != null)
				m.add(n1, RDF.type, m.createResource(e.getFrom().getType()));			
			if(e.getFrom().getTitle() != null)
				m.add(n1, getProp("title"), e.getFrom().getTitle());
			
			if(e.getTo().getType() != null)
				m.add(n2, RDF.type, m.createResource(e.getTo().getType()));
			if(e.getTo().getTitle() != null)
				m.add(n2, getProp("title"), e.getTo().getTitle());
			
			m.add(edge, getProp("effect"), n1);
			m.add(edge, getProp("cause"), n2);					
		}
		return m;
	}
	public static Property getProp(String prop){
		return ResourceFactory.createProperty(Properties.getString(prop));
	}

	/**
	 * Return the RDF representation of an edge.
	 * @param n
	 * @return
	 */
	public static Model getEdgeModel(Edge e){
		Model m = ModelFactory.createDefaultModel();
		Resource edge = m.createResource(e.getId());
		m.add(edge, RDF.type, m.createResource(e.getType()));
		Resource n1 = m.createResource(e.getFrom().getId());
		Resource n2 = m.createResource(e.getTo().getId());
		m.add(edge, getProp("effect"), n1);
		m.add(edge, getProp("cause"), n2);			
		return m;
	}
	/**
	 * Return the RDF representation of a graph.
	 * @param g
	 * @return
	 */
	public static Model getGraphModel(Graph g){
		Model m = ModelFactory.createDefaultModel();
		for(Node n : g.getNodes()){			
			m.add(getNodeModel(n));
		}
		return m;
	}

	/**
	 * Return the Graph representation of a rdf model.
	 * @param m
	 * @return
	 */
	public static Graph getModelGraph(Model m){
		Graph g = new Graph();
		for(String nodeType : ProvenanceService.getNodes()){
			ResIterator it = m.listResourcesWithProperty(RDF.type, m.getResource(nodeType));
			while(it.hasNext()){
				Resource r = it.next();
				g.addNode(RDFProvider.getNode(g, r));
			}
		}
		for(String edgeType : ProvenanceService.getProperties()){
			ResIterator it = m.listResourcesWithProperty(RDF.type, m.getResource(edgeType));
			while(it.hasNext()){
				Resource r = it.next();
				try {
					g.addEdge(RDFProvider.getEdge(g, r));
				} catch (OpenRDFException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return g;
	}
	

	/**
	 * Inserts node into the RDF repository.
	 * @param n
	 * @return
	 */
	public static int insertNode(Node n){
		Model m = getNodeModel(n);
		try {
			write(m);
		} catch (Exception e) {
			return -1;
		}
		//All ok, discard the model.
		m.close();		
		return 0;
	}
	/**
	 * Inserts node into the RDF repository.
	 * @param e
	 * @return
	 */
	public static int insertEdge(Edge e){
		Model m = getEdgeModel(e);
		try {
			write(m);
		} catch (Exception ex) {
			return -1;
		}
		//All ok, discard the model.
		m.close();		
		return 0;
	}
	/**
	 * Inserts graph into the RDF repository
	 * @param g
	 * @return
	 */
	public static int insertGraph(Graph g){
		Model m = getGraphModel(g);
		try {
			write(m);
		} catch (Exception ex) {
			return -1;
		}
		//All ok, discard the model.
		m.close();		
		return 0;
	}
		
	/**
	 * Gets the subject's property from RDF repository.
	 * @param subject
	 * @param property
	 * @return
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	private static String getProperty(String subject, String property) throws RepositoryException, MalformedQueryException, QueryEvaluationException{
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <"+subject+"> <"+property+"> ?y } ");
		return executeSparqlQuery(qry);
	}
	/**
	 * Gets list of subject's properties from RDF repository.
	 * @param subject
	 * @param property
	 * @return
	 * @throws RepositoryException
	 * @throws QueryEvaluationException
	 * @throws MalformedQueryException
	 */
	@SuppressWarnings("unused")
	private static List<String> getProperties(String subject, String property) throws RepositoryException, QueryEvaluationException, MalformedQueryException{
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <"+subject+"> <"+property+"> ?y } ");
		return executeArraySparqlQuery(qry);
	}
	
	private static List<String> getPropertiesTo(String subject, String property) throws RepositoryException, QueryEvaluationException, MalformedQueryException{
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { ?y <"+property+"> <"+subject+">  } ");
		return executeArraySparqlQuery(qry);
	}
	
	/**
	 * Executes given query on the RDF repository.
	 * @param qry
	 * @return
	 * @throws RepositoryException 
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 */
	private static List<String> executeArraySparqlQuery(StringBuffer qry) throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		List<String> resource = new ArrayList<String>();		
		String query = qry.toString();		
			connect();	
			TupleQuery output = getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult result = output.evaluate();		
			while (result.hasNext()) 
			{
				   BindingSet bindingSet = result.next();
				   Value value = bindingSet.getValue("y");
				   resource.add(value.stringValue());
			}
			result.close();
			disconnect();	
		return resource;
	}
	/**
	 * Executes given query on the RDF repository.
	 * @param qry
	 * @return
	 * @throws RepositoryException 
	 * @throws MalformedQueryException 
	 * @throws QueryEvaluationException 
	 */
	private static String executeSparqlQuery(StringBuffer qry) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String resource = null;		
		String query = qry.toString();		
			connect();	
			TupleQuery output = getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult result = output.evaluate();		
			while (result.hasNext()) 
			{
				   BindingSet bindingSet = result.next();
				   Value value = bindingSet.getValue("y");
				   resource = value.stringValue();
			}
			result.close();
			disconnect();	
		return resource;
	}
	
	/**
	 * Connects to the repository
	 * @throws RepositoryException
	 */
	public static void connect() throws RepositoryException
	{
		Repository rep = new HTTPRepository(Properties.getString("url"), Properties.getString("repository"));
		rep.initialize();
		setCon(rep.getConnection());
	}
	/**
	 * Disconnects from the repository
	 * @throws RepositoryException
	 */
	public static void disconnect() throws RepositoryException
	{

		getCon().close();
	}


	public static RepositoryConnection getCon() {
		return con;
	}


	public static void setCon(RepositoryConnection con) {
		RDFProvider.con = con;
	}

	public static List<String> getCustomProperties() {
		return customProperties;
	}

	public static void setCustomProperties(List<String> customProperties) {
		RDFProvider.customProperties = customProperties;
	}
}
