package provenanceService.provenanceModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.ProvenanceServiceImpl;
import provenanceService.Utility;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
/**
 * Class providing functions for manipulation with RDF. Conversions between
 * Graph and RDF and querying the underlying RDF repository as well.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public abstract class DataProvider {
	/** All the ontologies loaded into one big model. */
	protected OntModel ontologies;

	/** List of custom properties to load. */
	protected List<String> customProperties;

	/**URL of the SPARQL endpoint for querying.*/
	protected String endpoint;
	/**URL of the SPARQL endpoint to write the data to.*/
	protected String endpointWrite;

	/** The impl. */
	protected ProvenanceServiceImpl impl;

	/**
	 * Initialises the connection to repository, loads  the ontologies.
	 *
	 * @param impl the impl
	 */
	@SuppressWarnings("unchecked")
	public void init(final ProvenanceServiceImpl impl) {
		this.impl = impl;
		if(this.impl == null)
			this.impl = ProvenanceService.getSingleton();
		System.setProperty("http.proxyHost", Properties.getString("proxyhost"));
		System.setProperty("http.proxyPort", Properties.getString("proxyport"));

		// customProperties = new ArrayList<String>();
		customProperties = Properties.getValues().getList("customProperties");
		endpoint = Properties.getValues().getString("endpoint");
		endpointWrite = Properties.getValues().getString("endpointWrite");

		String path = Properties.getString("ontologiesDirectory");
		path = Properties.getBaseFolder() + path;
		// File d = new File(path);
		/*
		 * String[] onts = d.list(new FilenameFilter() { public boolean
		 * accept(File arg0, String name) { return name.endsWith(".owl"); } });
		 */
		List<String> ontologiesNames = Properties.getValues().getList("ontologies");
		ontologies = ModelFactory.createOntologyModel();
		if (ontologiesNames != null) {
			for (String s : ontologiesNames) {
				Model m = ModelFactory.createOntologyModel();
				FileInputStream in;
				try {
					in = new FileInputStream(path + s);
					m = (OntModel) m.read(in, "");
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

	/**
	 * Executes SPARQL update on the remote endpoint.
	 * @param query Update to be executed.
	 */
	protected void executeUpdate(final String query){
		//Send the SPARQL update to the server
		try{
		UpdateRequest queryObj = UpdateFactory.create(query);
		UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(queryObj, endpointWrite);
		qexec.execute();
		}
		catch(Exception e){
			//Try the other method
			UpdateRequest queryObj = UpdateFactory.create(query);
			UpdateProcessor qexec = UpdateExecutionFactory.createRemote(queryObj, endpointWrite);
			qexec.execute();
		}
	}

	/**
	 * Deletes the list of uris from the repository.
	 *
	 * @param uris List of uris to be deleted.
	 */
	public void delete(final List<String> uris) {
		// connect();
		for (String s : uris) {
			Node n = this.getNode(null, s);
			Edge e = this.getEdge(null, s);
			String query;
			if (n.getBasicType() != null)
				query = impl.getSPARQLProvider().getNodeSPARQL(n).toString();
			else
				query = impl.getSPARQLProvider().getEdgeSPARQL(e).toString();
			query = "DELETE DATA { " + query + " }";
			executeUpdate(query);
		}
		// disconnect();
	}

	/**
	 * Deletes the given model from repository.
	 * @param delete Model to be deleted.
	 */
	public void delete(final Model delete)  {
		if(delete.size() == 0)
			return;
		// connect();
/*
        GraphStore gs=GraphStoreFactory.create(delete);
        gs.setDefaultGraph(delete.getGraph());
        UpdateRequest ur = UpdateFactory.create(query);
        Update update = ur.getOperations().get(0);
        UpdateExecutionFactory.createRemote(ur,"http://localhost:3030/per2.owl");
        UpdateAction.execute(ur,gs);*/

		String query = impl.getSPARQLProvider().getGraphSPARQL(impl.getRDFProvider().getModelGraph(delete), false).toString();
		executeUpdate(query);
	}

	/**
	 * Writes the given model to the repository.
	 * @param m Model to be written to the repository.
	 */
	public void write(final Model m)  {
		// connect();
		// TODO policies handling and validation
		//StringWriter out = new StringWriter();
		//m.write(out, "N-TRIPLE");
		//String result = "INSERT DATA { " + out.toString()+ " }";
		if(m.size() == 0)
			return;
		String query = impl.getSPARQLProvider().getGraphSPARQL(impl.getRDFProvider().getModelGraph(m), true).toString();
		executeUpdate(query);
	}

	/**
	 * Returns the subclasses of the given type.
	 * @param type Type, which subclasses will be returned.
	 * @param direct If true, only direct subclasses are returned. Useful when creating a tree of classes.
	 * @return List of subclasses.
	 */
	public Vector<String> getSubclasses(final String type, final boolean direct) {
		Vector<String> classList = new Vector<String>();
		OntClass c = ontologies.getOntClass(type);
		if (c == null)
			return classList;
		try {
			// Do it write because of iterators possible conflict.
			ontologies.enterCriticalSection(Lock.WRITE);
			for (Iterator<OntClass> it = c.listSubClasses(direct); it.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection();
		}
		return classList;
	}

	/**
	 * Returns the subclasses of the given type.
	 * @param type Type, which subclasses will be returned.
	 * @return List of subclasses.
	 */
	public Vector<String> getSubclasses(final String type) {
		Vector<String> classList = new Vector<String>();
		OntClass c = ontologies.getOntClass(type);
		if (c == null)
			return classList;
		try {
			// Do it write because of iterators possible conflict.
			ontologies.enterCriticalSection(Lock.WRITE);
			for (Iterator<OntClass> it = c.listSubClasses(false); it.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection();
		}
		return classList;
	}

	/**
	 * Returns the superclasses of the given type.
	 * @param resourceType Type, which superclasses will be returned.
	 * @return List of superclasses.
	 */
	public Vector<String> getSuperclasses(final String resourceType) {
		Vector<String> classList = new Vector<String>();
		// Do it write because of iterators possible conflict.
		ontologies.enterCriticalSection(Lock.WRITE);
		try {
			OntClass c = ontologies.getOntClass(resourceType);
			for (Iterator<OntClass> it = c.listSuperClasses(false); it
					.hasNext();) {
				OntClass sc = (OntClass) it.next();
				classList.add(sc.getURI());
			}
		} finally {
			ontologies.leaveCriticalSection();
		}
		return classList;
	}

	/**
	 * Returns iterator over all restrictions that are superclasses of given
	 * class.
	 *
	 * @param oclass Name of the class.
	 * @return Iterator over all restrictions that are superclasses of given
	 * class.
	 */
	public Iterator<Restriction> getRestrictionsOnClass(final String oclass) {
		OntClass c = ontologies.getOntClass(oclass);
		ExtendedIterator<OntClass> l = c.listSuperClasses();
		List<Restriction> restr = new ArrayList<Restriction>();
		// System.out.println(oclass);
		while (l.hasNext()) {
			OntClass sc = l.next();
			// We take only restrictions
			if (sc.isRestriction()) {
				Restriction r = sc.asRestriction();
				restr.add(r);
			}
		}
		return restr.iterator();
	}

	/**
	 * Gets the entity description.
	 *
	 * @param resource the resource
	 * @return the entity description
	 */
	protected String getEntityDescription(final String resource) {
		// For persons, we have to get the name instead of the title.
		if (impl.getBasicTypes() != null
				&& "Agent".equals(impl.getShape(getProperty(
						resource, Properties.getString("type"))))) {
			String firstname = getProperty(resource,
					Properties.getString("firstname"));
			String surname = getProperty(resource,
					Properties.getString("surname"));
			String fullname = firstname + " " + surname;
			return fullname;
		} else
			return getProperty(resource, Properties.getString("title"));
	}

	/**
	 * Loads the adjacencies of the given node and adds them to the graph object.
	 *
	 * @param g The graph to be used.
	 * @param n Node which adjacencies are returned.
	 * @param to 0=to,1=from,2=both
	 */
	public abstract void getAdjacencies(final Graph g, final Node n,final  int to);

	/**
	 * Loads the custom properties from the RDF repository.
	 *
	 * @param n the n
	 * @param res the res
	 */
	public void loadCustomProperties(final Node n, final Resource res) {
		for (String prop : customProperties) {
			String val = null;
			if (res == null)
				val = getProperty(n.getId(), prop);
			else {
				Statement t = res.getProperty(res.getModel().getProperty(prop));
				if (t != null)
					val = t.getString();
			}
			if (val != null && !("".equals(val)))
				n.addProperty(prop, val);
		}
	}

	/**
	 * Gets the properties of the node from RDF repository. Does not load the
	 * adjacencies though, in order to avoid greedy crawl of the whole graph.
	 *
	 * @param g the g
	 * @param resource the resource
	 * @return The Node with filled properties without the adjacencies.
	 */
	public abstract Node getNode(final Graph g, final String resource) ;

	/**
	 * Finds the edge in the RDF repository.
	 *
	 * @param g the g
	 * @param edgeURI URI of the edge in the repository.
	 * @return New Edge object.
	 */
	public abstract Edge getEdge(final Graph g,final  String edgeURI) ;

	/**
	 * Return Node from given Resource.
	 *
	 * @param g the g
	 * @param res the res
	 * @return the node
	 */
	public Node getNode(final Graph g, final Resource res)  {
		if (res == null)
			return null;
		if (!Utility.isURI(res.getURI()))
			return null;
		return getNode(g, res.getURI());
	}

	/**
	 * Gets the Edge with identifier from given resource.
	 *
	 * @param g The graph.
	 * @param edge Resource representing the edge.
	 * @return the edge
	 */
	public Edge getEdge(final Graph g, final Resource edge)  {
		if (edge == null)
			return null;
		if (!Utility.isURI(edge.getURI()))
			return null;
		return getEdge(g, edge.getURI());
	}

	/**
	 * Gets the all provenance.
	 *
	 * @return the all provenance
	 */
	public Graph getAllProvenance() {
		List<String> individuals = new ArrayList<String>();
		for (String c : impl.getNodes()) {
				individuals.addAll(getPropertiesTo(c,
						"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		}
		List<String> edges = new ArrayList<String>();
		for (String c : impl.getProperties()) {
				edges.addAll(getPropertiesTo(c,
						"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		}
		Graph g = new Graph();
		for (String ind : individuals) {
			Node n;
				n = getNode(g, ind);
				g.addNode(n);
		}
		for (String ind : edges) {
			Edge e;
				e = getEdge(g, ind);
				g.addEdge(e);
		}
		return g;
	}

	/**
	 * Inserts node into the RDF repository.
	 *
	 * @param n the n
	 * @return the int
	 */
	public int insertNode(final Node n) {
		Model m = impl.getRDFProvider().getNodeModel(n);
		try {
			write(m);
		} catch (Exception e) {
			return -1;
		}
		// All ok, discard the model.
		m.close();
		return 0;
	}

	/**
	 * Inserts node into the RDF repository.
	 *
	 * @param e Edge to be inserted.
	 * @return the int
	 */
	public int insertEdge(final Edge e) {
		Model m = impl.getRDFProvider().getEdgeModel(e);
		try {
			write(m);
		} catch (Exception ex) {
			return -1;
		}
		// All ok, discard the model.
		m.close();
		return 0;
	}

	/**
	 * Inserts graph into the RDF repository.
	 *
	 * @param g the g
	 * @return the int
	 */
	public int insertGraph(final Graph g) {
		Model m = impl.getRDFProvider().getGraphModel(g);
		try {
			write(m);
		} catch (Exception ex) {
			return -1;
		}
		// All ok, discard the model.
		m.close();
		return 0;
	}

	/**
	 * Gets the subject's property from RDF repository.
	 *
	 * @param subject the subject
	 * @param property the property
	 * @return the property
	 */
	protected String getProperty(final String subject, final String property) {
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <" + subject + "> <" + property
				+ "> ?y. } ");
		return executeSparqlQuery(qry);
	}

	/**
	 * Gets the subject's property from RDF repository.
	 *
	 * @param subject the subject
	 * @param property the property
	 * @return the property to
	 */
	protected String getPropertyTo(final String subject, final String property){
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { ?y <" + property
				+ "> <" + subject + ">. } ");
		return executeSparqlQuery(qry);
	}

	/**
	 * Gets list of subject's properties from RDF repository.
	 *
	 * @param subject the subject
	 * @param property the property
	 * @return the properties
	 */
	protected List<String> getProperties(final String subject, final String property) {
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { <" + subject + "> <" + property
				+ "> ?y } ");
		return executeArraySparqlQuery(qry);
	}

	/**
	 * Gets the properties to.
	 *
	 * @param subject the subject
	 * @param property the property
	 * @return the properties to
	 */
	protected List<String> getPropertiesTo(final String subject, final String property) {
		StringBuffer qry = new StringBuffer(1024);
		qry.append("SELECT ?y where { ?y <" + property + "> <" + subject
				+ ">  } ");
		return executeArraySparqlQuery(qry);
	}

	/**
	 * Executes given query on the RDF repository.
	 *
	 * @param qry Query in SPARQL.
	 * @return Result to the query from SPARQL Endpoint or ontologies
	 */
	protected List<String> executeArraySparqlQuery(final StringBuffer qry) {
		Query query = QueryFactory.create(qry.toString());
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);

		ResultSet results = qexec.execSelect();
		qexec.close();

		List<String> resource = new ArrayList<String>();
		while (results.hasNext()) {
			QuerySolution bindingSet = results.next();
			RDFNode value = bindingSet.get("y");
			resource.add(value.toString());
		}

		// Results from the ontologies.
		Query queryOb = QueryFactory.create(qry.toString());
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(queryOb, ontologies);
		ResultSet jenaResults = qe.execSelect();
		QuerySolution bindingSet = null;
		while (jenaResults.hasNext()) {
			bindingSet = jenaResults.next();
			Object res = bindingSet.get("y");
			if (res != null)
				resource.add(res.toString());
		}
		// disconnect();
		return resource;
	}

	/**
	 * Executes given query on the RDF repository.
	 *
	 * @param qry Query in SPARQL.
	 * @return Result to the query from SPARQL Endpoint or ontologies
	 */
	protected String executeSparqlQuery(final StringBuffer qry) {
		String resource = null;
		Query query = QueryFactory.create(qry.toString());
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);

		ResultSet results = qexec.execSelect();
		qexec.close();

		while (results.hasNext()) {
			QuerySolution bindingSet = results.next();
			RDFNode value = bindingSet.get("y");
			resource = value.toString();
		}

		if(resource == null){

			//Results from the ontologies.
			Query queryOb = QueryFactory.create(qry.toString());
			// Execute the query and obtain results
			QueryExecution qe = QueryExecutionFactory.create(queryOb, ontologies);
			ResultSet jenaResults = qe.execSelect();
			QuerySolution bindingSet = null;

			while( jenaResults.hasNext()){
				bindingSet = jenaResults.next() ;
				Object res = bindingSet.get("y");
				if(res != null)
					resource = (res.toString());
			}
		}
		// disconnect();
		return resource;
	}


	/**
	 * Gets the custom properties.
	 *
	 * @return the custom properties
	 */
	public List<String> getCustomProperties() {
		return customProperties;
	}

	/**
	 * Sets the custom properties.
	 *
	 * @param customProperties the new custom properties
	 */
	public void setCustomProperties(final List<String> customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * Gets the ontologies.
	 *
	 * @return the ontologies
	 */
	public OntModel getOntologies() {
		return this.ontologies;
	}

	/**
	 * Sets the ontologies.
	 *
	 * @param ontologies the new ontologies
	 */
	public void setOntologies(final OntModel ontologies) {
		this.ontologies = ontologies;
	}
}
