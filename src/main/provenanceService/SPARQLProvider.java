package provenanceService;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/** Class for transformation between the Graph and JSON representation of the
 * provenance.
 *
 * @author AE */
public final class SPARQLProvider {

	/** Private constructor. */
	private SPARQLProvider() {
	}

	/** Nothing at the moment. */
	public static void init() {
	}

	/** Add brackets to the string.
	 *
	 * @param s String.
	 * @return String with the brackets. */
	private static String addBrackets(final String s) {
		return "<" + s + "> ";
	}
	/**
	 * Creates triple out of three parameters.
	 * @param s1 Subject
	 * @param s2 Property
	 * @param s3 Object
	 * @return String with the triple.
	 */
	private static String getTriple(final String s1, final String s2, final String s3) {
		IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		boolean includeWarnings = false;
		IRI iri;
		iri = iriFactory.create(s3);
		// Literal
		if (iri.hasViolation(includeWarnings))
			return addBrackets(s1) + addBrackets(s2) + "\"" + s3 + "\".\n";
		// Resource
		else
			return addBrackets(s1) + addBrackets(s2) + addBrackets(s3) + ".\n";
	}

	/** Return the JSON representation of a node.
	 *
	 * @param n Node.
	 * @return SPARQL notation of the node content.*/
	public static StringBuilder getNodeSPARQL(final Node n) {
		StringBuilder node = new StringBuilder();
		if (n.getTitle() != null)
			node.append(getTriple(n.getId(), Properties.getString("title"), n.getTitle()));

		if (n.getType() != null)
			node.append(getTriple(n.getId(), RDF.type.getURI(), n.getType()));
		for (String name : n.getProperties().keySet()) {
			Object value = n.getProperty(name);
			node.append(getTriple(n.getId(), name, value.toString()));
		}
		return node;
	}

	/** Return the JSON representation of an edge.
	 *
	 * @param e Edge.
	 * @return SPARQL notation of the edge content.*/
	public static StringBuilder getEdgeSPARQL(final Edge e) {
		StringBuilder edge = new StringBuilder();
		edge.append(getTriple(e.getId(), Properties.getString("to"), e.getTo().getId()));
		edge.append(getTriple(e.getId(), Properties.getString("from"), e.getFrom().getId()));
		edge.append(getTriple(e.getId(), RDF.type.getURI(), e.getType()));
		for (String name : e.getProperties().keySet()) {
			Object value = e.getProperty(name);
			edge.append(getTriple(e.getId(), name, value.toString()));
		}
		return edge;
	}

	/** Return the SPARQL representation of a graph.
	 *
	 * @param g Graph
	 * @param insert if true, data are inserted, otherwise deleted.
	 * @return SPARQL notation of the graph content.*/
	public static StringBuilder getGraphSPARQL(final Graph g, final boolean insert) {
		String action = "INSERT";
		if (!insert)
			action = "DELETE";
		StringBuilder graph = new StringBuilder();
		List<String> edgesInserted = new ArrayList<String>();
		graph.append(action + " DATA {\n");
		for (int i = 0; g != null && i < g.size(); i++) {
			Node n = g.get(i);
			StringBuilder s = getNodeSPARQL(n);
			graph.append(s);
			for (Edge e : n.getAdjacencies()) {
				if (!edgesInserted.contains(e.getId())) {
					graph.append(getEdgeSPARQL(e));
					edgesInserted.add(e.getId());
				}
			}
		}
		graph.append("}");
		return graph;
	}
}
