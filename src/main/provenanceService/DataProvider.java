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
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * Class providing functions for manipulation with RDF. Conversions between Graph and RDF and querying the underlying RDF repository as well.
 * @author AE
 *
 */
public class DataProvider {
	private  RepositoryConnection con;
	private  OntModel ontologies;

	/**List of custom properties to load.*/
	private  List<String> customProperties;
	@SuppressWarnings("unchecked")
	public  void init() {
		
		System.setProperty("http.proxyHost", Properties.getString("proxyhost"));
		System.setProperty("http.proxyPort", Properties.getString("proxyport"));
		try {
			connect();
		} catch (RepositoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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

	public  void delete(List<String> uris) throws OpenRDFException, IOException {
		//connect();		
		for(String s : uris){
			Node n = this.getNode(null, s);
			Edge e = this.getEdge(null, s);
			String query;
			if(n.getBasicType() != null)
				query = SPARQLProvider.getNodeSPARQL(n).toString();
			else
				query = SPARQLProvider.getEdgeSPARQL(e).toString();
			query = "DELETE DATA { " + query + " }";
			Update up = getCon().prepareUpdate(org.openrdf.query.QueryLanguage.SPARQL, query);
			up.execute();
		}
		//disconnect();		
	}
	public  void delete(Model delete) throws OpenRDFException, IOException {
		//connect();		
		getCon().prepareUpdate(org.openrdf.query.QueryLanguage.SPARQL, SPARQLProvider.getGraphSPARQL(getModelGraph(delete),false).toString());
		//disconnect();		
	}
	
	public  void write(Model m) throws OpenRDFException, IOException {
		//connect();
		//TODO policies handling and validation
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.write(out);
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		getCon().add(in, Properties.getString("url"), RDFFormat.RDFXML);
		//disconnect();		
	}
	

	public  Vector<String> getSubclasses(String type, boolean direct) {
		Vector<String> classList = new Vector<String>();
		OntClass c = ontologies.getOntClass(type);
		if(c == null)
			return classList;
		try {
			//Do it write because of iterators possible conflict.
			ontologies.enterCriticalSection(Lock.WRITE);
			for (Iterator<OntClass> it = c.listSubClasses(direct); it.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection() ;
		}
		return classList;
	}
	public  Vector<String> getSubclasses(String type) {
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

	public  Vector<String> getSuperclasses( String resourceType) {
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
	public  Iterator<Restriction> getRestrictionsOnClass(String oclass){
		OntClass c = ontologies.getOntClass(oclass);		
		ExtendedIterator<OntClass> l = c.listSuperClasses();
		List<Restriction> restr = new ArrayList<Restriction>();
		//System.out.println(oclass);
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

	private  String getEntityDescription(String resource) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
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
	public  void getAdjacencies(Graph g, Node n, int to){		
		try {
			List<String> adjacencies = null;
			if(to==0)
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("from"));
			else if(to==1)
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("to"));
			else if(to==2){
				adjacencies = getPropertiesTo(n.getId(), Properties.getString("from"));
				adjacencies.addAll(getPropertiesTo(n.getId(), Properties.getString("to")));
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
	public  void loadCustomProperties(Node n, Resource res) throws OpenRDFException{		
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
	public  Node getNode(Graph g, String resource) throws OpenRDFException{	
		if(!Utility.isURI(resource))
			return null;
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
	 * Finds the edge in the RDF repository. 
	 * @param g
	 * @param edgeURI
	 * @return New Edge object.
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public  Edge getEdge(Graph g, String edgeURI) throws OpenRDFException{
		if(edgeURI == null || edgeURI.equals(""))
			return null;
		if(!Utility.isURI(edgeURI))
			return null;
		Edge edge = new Edge(edgeURI);
		edge.setType(getProperty(edgeURI, Properties.getString("type")));
		if ((edge.getType() == null || edge.getType().equals("")))
			return null;
		
		String from = getProperty(edgeURI, Properties.getString("from"));
		if(from == null || !Utility.isURI(from))
			return null;
		if (g != null)
			edge.setFrom(g.getNode(from));
		if (edge.getFrom() == null) {
			edge.setFrom(getNode(g, from));			
		}
		if(edge.getFrom() == null)
			return null;

		String to = getProperty(edgeURI, Properties.getString("to"));
		if(to == null || !Utility.isURI(to))
			return null;
		if (g != null)
			edge.setTo(g.getNode(to));
		if (edge.getTo() == null) {
			edge.setTo(getNode(g, to));
		}
		if(edge.getTo() == null)
			return null;
		return edge;			
	}
	/**
	 * Return Node from given Resource
	 * @param g
	 * @param res
	 * @return
	 */
	public  Node getNode(Graph g, Resource res){		
		Node node = null;
		if(res == null)
			return null;
		if(!Utility.isURI(res.getURI()))
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
		//else
		//	node.setTitle(Utility.getLocalName(res.getURI()));

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
	public  Edge getEdge(Graph g, Resource edge) throws OpenRDFException {	
		if(edge == null)
			return null;	
		if(!Utility.isURI(edge.getURI()))
			return null;
		if(edge.getProperty(Utility.getProp("from")) == null || edge.getProperty(Utility.getProp("to")) == null){
			return null;
		}
		Edge e = new Edge(edge.getURI());
		Statement t = edge.getProperty(RDF.type);
		if(t != null)
			e.setType(t.getObject().toString());
		

		Resource tmp = edge.getProperty(Utility.getProp("from")).getResource();
		if(tmp == null || !Utility.isURI(tmp.getURI()))
			return null;
		if (g != null)
			e.setFrom(g.getNode(tmp.getURI()));
		if (e.getFrom() == null) {
			e.setFrom(getNode(g, tmp));
		}
		if(e.getFrom() == null)
			return null;

		tmp = edge.getProperty(Utility.getProp("to")).getResource();
		if(tmp == null || !Utility.isURI(tmp.getURI()))
			return null;
		if (g != null)
			e.setTo(g.getNode(tmp.getURI()));
		if (e.getTo() == null) {
			e.setTo(getNode(g, tmp));
		}
		if(e.getTo() == null)
			return null;
		
		//Add the edge only if everything's all right
		if(e.getFrom() != null && e.getTo() != null){
			e.getFrom().getAdjacencies().add(e);
			e.getTo().getAdjacencies().add(e);
		}
		else 
			return null;
		return e;
	}
	
	public  Graph getAllProvenance(){
		List<String> individuals = new ArrayList<String>();
		for(String c : ProvenanceService.getNodes()){
			try {
				individuals.addAll(getPropertiesTo(c, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
			} catch (RepositoryException e) {
				e.printStackTrace();
			} catch (QueryEvaluationException e) {
				e.printStackTrace();
			} catch (MalformedQueryException e) {
				e.printStackTrace();
			}
		}
		List<String> edges = new ArrayList<String>();
		for(String c : ProvenanceService.getProperties()){
			try {
				edges.addAll(getPropertiesTo(c, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
			} catch (RepositoryException e) {
				e.printStackTrace();
			} catch (QueryEvaluationException e) {
				e.printStackTrace();
			} catch (MalformedQueryException e) {
				e.printStackTrace();
			}
		}
		Graph g = new Graph();
		for(String ind : individuals){
			Node n;
			try {
				n = getNode(g, ind);
				g.addNode(n);
			} catch (OpenRDFException e) {
				e.printStackTrace();
			}
		}
		for(String ind : edges){
			Edge e;
			try {
				e = getEdge(g, ind);
				g.addEdge(e);
			} catch (OpenRDFException e2) {
				e2.printStackTrace();
			}
		}
		return g;
	}

	/**
	 * Return the RDF representation of an edge.
	 * @param n
	 * @return
	 */
	public  Model getEdgeModel(Edge e){
		Model m = ModelFactory.createDefaultModel();
		if(e == null)
			return m;
		Resource edge = m.createResource(e.getId());
		m.add(edge, RDF.type, m.createResource(e.getType()));
		Resource n1 = m.createResource(e.getFrom().getId());
		m.add(edge, Utility.getProp("from"), n1);
		
		Resource n2 = m.createResource(e.getTo().getId());
		m.add(edge, Utility.getProp("to"), n2);			
		return m;
	}
	/**
	 * Return the RDF representation of a graph.
	 * @param g
	 * @return
	 */
	public  Model getGraphModel(Graph g){
		Model m = ModelFactory.createDefaultModel();
		for(Node n : g.getNodes()){			
			m.add(RDFProvider.getNodeModel(n));
		}
		return m;
	}

	/**
	 * Return the Graph representation of a rdf model.
	 * @param m
	 * @return
	 */
	public  Graph getModelGraph(Model m){
		Graph g = new Graph();
		if(m == null)
			return g;
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
	public  int insertNode(Node n){
		Model m = RDFProvider.getNodeModel(n);
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
	public  int insertEdge(Edge e){
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
	public  int insertGraph(Graph g){
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
	private  String getProperty(String subject, String property) throws RepositoryException, MalformedQueryException, QueryEvaluationException{
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <"+subject+"> <"+property+"> ?y. } ");
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
	private  List<String> getProperties(String subject, String property) throws RepositoryException, QueryEvaluationException, MalformedQueryException{
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <"+subject+"> <"+property+"> ?y } ");
		return executeArraySparqlQuery(qry);
	}
	
	private  List<String> getPropertiesTo(String subject, String property) throws RepositoryException, QueryEvaluationException, MalformedQueryException{
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
	private  List<String> executeArraySparqlQuery(StringBuffer qry) throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		List<String> resource = new ArrayList<String>();		
		String query = qry.toString();		
			//connect();	
			TupleQuery output = getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult result = output.evaluate();		
			while (result.hasNext()) 
			{
				   BindingSet bindingSet = result.next();
				   Value value = bindingSet.getValue("y");
				   resource.add(value.stringValue());
			}
			result.close();
			//disconnect();	
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
	private  String executeSparqlQuery(StringBuffer qry) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String resource = null;		
		String query = qry.toString();		
			//connect();	
			TupleQuery output = getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult result = output.evaluate();		
			while (result.hasNext()) 
			{
				   BindingSet bindingSet = result.next();
				   Value value = bindingSet.getValue("y");
				   resource = value.stringValue();
			}
			result.close();
			//disconnect();	
		return resource;
	}
	
	/**
	 * Connects to the repository
	 * @throws RepositoryException
	 */
	public  void connect() throws RepositoryException
	{
		if(con != null)
			return;
		Repository rep = new HTTPRepository(Properties.getString("url"), Properties.getString("repository"));
		rep.initialize();
		setCon(rep.getConnection());
	}
	/**
	 * Disconnects from the repository
	 * @throws RepositoryException
	 */
	public  void disconnect() throws RepositoryException
	{

		getCon().close();
	}


	public  RepositoryConnection getCon() {
		return this.con;
	}


	public  void setCon(RepositoryConnection con) {
		this.con = con;
	}

	public  List<String> getCustomProperties() {
		return customProperties;
	}

	public  void setCustomProperties(List<String> customProperties) {
		this.customProperties = customProperties;
	}
	public  OntModel getOntologies() {
		return this.ontologies;
	}

	public  void setOntologies(OntModel ontologies) {
		this.ontologies = ontologies;
	}
}
