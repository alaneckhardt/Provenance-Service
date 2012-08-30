package provenanceService.provenanceModel;


import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.Utility;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/** Class providing functions for manipulation with RDF. Conversions between
 * Graph and RDF and querying the underlying RDF repository as well.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public final class OPMRDFProvider extends RDFProvider {

	/** Return Node from given Resource.
	 *
	 * @param g Graph that may contain the node.
	 * @param res RDF resource containing the node.
	 * @return Node*/
	public Node getNode(final Graph g, final Resource res) {
		Node node = null;
		if (res == null)
			return null;
		if (!Utility.isURI(res.getURI()))
			return null;
		if (g != null)
			node = g.getNode(res.getURI());
		// Node is in the graph
		if (node != null)
			return node;

		node = new Node(res.getURI());
		Statement t = res.getProperty(res.getModel().getProperty(Properties.getString("title")));
		if (t != null)
			node.setTitle(t.getString());
		// else
		// node.setTitle(Utility.getLocalName(res.getURI()));

		t = res.getProperty(RDF.type);
		if (t != null) {
			node.setType(t.getObject().toString());
			node.setBasicType(ProvenanceService.getSingleton().getShape(node.getType()));
		}
		loadCustomProperties(node, res);
		return node;
	}

	/** Gets the Edge with identifier from given resource.
	 *
	 * @param g Graph that may contain the edge.
	 * @param edge RDF resource containing the edge.
	 * @return Edge*/
	public Edge getEdge(final Graph g,final Resource edge) {
		if (edge == null)
			return null;
		if (!Utility.isURI(edge.getURI()))
			return null;
		if (edge.getProperty(Utility.getProp("from")) == null || edge.getProperty(Utility.getProp("to")) == null) {
			return null;
		}
		Edge e = new Edge(edge.getURI());
		Statement t = edge.getProperty(RDF.type);
		if (t != null)
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

		// Add the edge only if everything's all right
		if (e.getFrom() != null && e.getTo() != null) {
			e.getFrom().getAdjacencies().add(e);
			e.getTo().getAdjacencies().add(e);
		} else
			return null;
		return e;
	}

	/** Return the RDF representation of a node.
	 *
	 * @param n Node
	 * @return Model of the node.*/
	public Model getNodeModel(final Node n) {
		Model m = ModelFactory.createDefaultModel();
		if (n == null)
			return m;
		Resource r = m.createResource(n.getId());
		if (n.getType() != null)
			m.add(r, RDF.type, m.createResource(n.getType()));
		if (n.getTitle() != null)
			m.add(r, Utility.getProp("title"), n.getTitle());
		for (String prop : n.getProperties().keySet()) {
			Property p = m.getProperty(prop);
			Object value = n.getProperties().get(prop);
			IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
			boolean includeWarnings = false;
			IRI iri;
			iri = iriFactory.create(value.toString());
			// Literal
			if (iri.hasViolation(includeWarnings))
				r.addProperty(p, value.toString());
			// Resource
			else
				r.addProperty(p, m.createResource(value.toString()));

		}
		for (Edge e : n.getAdjacencies()) {
			Resource edge = m.createResource(e.getId());
			if (e.getType() != null)
				m.add(edge, RDF.type, m.createResource(e.getType()));

			Resource n1 = m.createResource(e.getFrom().getId());
			Resource n2 = m.createResource(e.getTo().getId());
			if (e.getFrom().getType() != null)
				m.add(n1, RDF.type, m.createResource(e.getFrom().getType()));
			if (e.getFrom().getTitle() != null)
				m.add(n1, Utility.getProp("title"), e.getFrom().getTitle());
			m.add(edge, Utility.getProp("from"), n1);

			if (e.getTo().getType() != null)
				m.add(n2, RDF.type, m.createResource(e.getTo().getType()));
			if (e.getTo().getTitle() != null)
				m.add(n2, Utility.getProp("title"), e.getTo().getTitle());
			m.add(edge, Utility.getProp("to"), n2);
		}
		return m;
	}

	/** Return the RDF representation of an edge.
	 *
	 * @param e Edge.
	 * @return Model of the edge.*/
	public  Model getEdgeModel(final Edge e) {
		Model m = ModelFactory.createDefaultModel();
		if (e == null)
			return m;
		Resource edge = m.createResource(e.getId());
		m.add(edge, RDF.type, m.createResource(e.getType()));
		Resource n1 = m.createResource(e.getFrom().getId());
		m.add(edge, Utility.getProp("from"), n1);

		Resource n2 = m.createResource(e.getTo().getId());
		m.add(edge, Utility.getProp("to"), n2);
		return m;
	}

	/** Return the RDF representation of a graph.
	 *
	 * @param g Graph to be represented.
	 * @return Model of the graph.*/
	public Model getGraphModel(final Graph g) {
		Model m = ModelFactory.createDefaultModel();
		for (Node n : g.getNodes()) {
			m.add(getNodeModel(n));
		}
		return m;
	}

	/** Return the Graph representation of a rdf model.
	 *
	 * @param m Model to be transformed to Graph.
	 * @return Graph made of the Model.*/
	public Graph getModelGraph(final Model m) {
		Graph g = new Graph();
		if (m == null)
			return g;
		for (String nodeType : ProvenanceService.getSingleton().getNodes()) {
			ResIterator it = m.listResourcesWithProperty(RDF.type, m.getResource(nodeType));
			while (it.hasNext()) {
				Resource r = it.next();
				g.addNode(getNode(g, r));
			}
		}
		for (String edgeType : ProvenanceService.getSingleton().getProperties()) {
			ResIterator it = m.listResourcesWithProperty(RDF.type, m.getResource(edgeType));
			while (it.hasNext()) {
				Resource r = it.next();
				g.addEdge(getEdge(g, r));
			}
		}
		return g;
	}

}
