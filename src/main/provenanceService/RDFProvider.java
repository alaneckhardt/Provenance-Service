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
	private static OntModel ontologies;

	/**List of custom properties to load.*/
	private static List<String> customProperties;
	@SuppressWarnings("unchecked")
	public static void init() {
		
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
	public static Edge getEdge(Graph g, Resource edge) throws OpenRDFException {	
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
		
		if (g != null)
			e.setFrom(g.getNode(edge.getProperty(Utility.getProp("from")).getResource().getURI()));
		if (e.getFrom() == null) {
			e.setFrom(getNode(g, edge.getProperty(Utility.getProp("from")).getResource()));
		}
		
		if (g != null)
			e.setTo(g.getNode(edge.getProperty(Utility.getProp("to")).getResource().getURI()));
		if (e.getTo() == null) {
			e.setTo(getNode(g, edge.getProperty(Utility.getProp("to")).getResource()));
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
	 * Return the RDF representation of a node.
	 * @param n
	 * @return
	 */
	public static Model getNodeModel(Node n){
		Model m = ModelFactory.createDefaultModel();
		if(n == null)
			return m;
		Resource r = m.createResource(n.getId());
		if(n.getType() != null)
			m.add(r, RDF.type, m.createResource(n.getType()));
		if(n.getTitle() != null)
			m.add(r, Utility.getProp("title"), n.getTitle());
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
				m.add(n1, Utility.getProp("title"), e.getFrom().getTitle());
			m.add(edge, Utility.getProp("from"), n1);
			
			if(e.getTo().getType() != null)
				m.add(n2, RDF.type, m.createResource(e.getTo().getType()));
			if(e.getTo().getTitle() != null)
				m.add(n2, Utility.getProp("title"), e.getTo().getTitle());			
			m.add(edge, Utility.getProp("to"), n2);					
		}
		return m;
	}
	

	/**
	 * Return the RDF representation of an edge.
	 * @param n
	 * @return
	 */
	public static Model getEdgeModel(Edge e){
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
	 * Loads the custom properties from the RDF repository.
	 * @param n
	 * @throws OpenRDFException
	 */
	public static  void loadCustomProperties(Node n, Resource res) throws OpenRDFException{		
		for (String prop : customProperties) {			
			String val = null;
			Statement t = res.getProperty(res.getModel().getProperty(prop));
			if(t != null){
				val = t.getString();
				if(val != null && !("".equals(val)))
					n.addProperty(prop, val);
			}
		}
	}

}
