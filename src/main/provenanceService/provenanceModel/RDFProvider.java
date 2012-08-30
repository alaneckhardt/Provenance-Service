package provenanceService.provenanceModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/** Class providing functions for manipulation with RDF. Conversions between
 * Graph and RDF and querying the underlying RDF repository as well.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public abstract class RDFProvider {
	/**Custom ontologies.*/
	protected OntModel ontologies;

	/** List of custom properties to load. */
	protected List<String> customProperties;
	/**Private constructor.*/
	public RDFProvider(){}
	/**
	 * Initialises the RDFProvider.
	 */
	@SuppressWarnings("unchecked")
	public void init() {

		System.setProperty("http.proxyHost", Properties.getString("proxyhost"));
		System.setProperty("http.proxyPort", Properties.getString("proxyport"));
		// customProperties = new ArrayList<String>();
		customProperties = Properties.getValues().getList("customProperties");

		String path = Properties.getString("ontologiesDirectory");
		path = Properties.getBaseFolder() + path;
		// File d = new File(path);
		/* String[] onts = d.list(new FilenameFilter() {
		 * public boolean accept(File arg0, String name) {
		 * return name.endsWith(".owl");
		 * }
		 * }); */
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

	/** Return Node from given Resource.
	 *
	 * @param g Graph that may contain the node.
	 * @param res RDF resource containing the node.
	 * @return Node*/
	public abstract Node getNode(final Graph g, final Resource res);

	/** Gets the Edge with identifier from given resource.
	 *
	 * @param g Graph that may contain the edge.
	 * @param edge RDF resource containing the edge.
	 * @return Edge */
	public abstract Edge getEdge(final Graph g,final Resource edge);

	/** Return the RDF representation of a node.
	 *
	 * @param n Node
	 * @return Model of the node.*/
	public abstract Model getNodeModel(final Node n);

	/** Return the RDF representation of an edge.
	 *
	 * @param e Edge.
	 * @return Model of the edge.*/
	public abstract Model getEdgeModel(final Edge e) ;

	/** Return the RDF representation of a graph.
	 *
	 * @param g Graph to be represented.
	 * @return Model of the graph.*/
	public abstract Model getGraphModel(final Graph g) ;

	/** Return the Graph representation of a rdf model.
	 *
	 * @param m Model to be transformed to Graph.
	 * @return Graph made of the Model.*/
	public abstract Graph getModelGraph(final Model m);

	/** Loads the custom properties from the RDF repository.
	 *
	 * @param n Node. Shouldn't be null.
	 * @param res RDF resource containing the custom properties for the node.
	 */
	public void loadCustomProperties(final Node n,final Resource res)  {
		for (String prop : customProperties) {
			String val = null;
			Statement t = res.getProperty(res.getModel().getProperty(prop));
			if (t != null) {
				val = t.getString();
				if (val != null && !("".equals(val)))
					n.addProperty(prop, val);
			}
		}
	}

}
