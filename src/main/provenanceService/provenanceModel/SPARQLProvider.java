package provenanceService.provenanceModel;

import java.util.ArrayList;
import java.util.List;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;

/** Class for transformation between the Graph and JSON representation of the
 * provenance.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public abstract class SPARQLProvider {

	/** Private constructor. */
	public SPARQLProvider() {
	}

	/** Nothing at the moment. */
	public static void init() {
	}

	/** Add brackets to the string.
	 *
	 * @param s String.
	 * @return String with the brackets. */
	protected String addBrackets(final String s) {
		return "<" + s + "> ";
	}
	/**
	 * Creates triple out of three parameters.
	 * @param s1 Subject
	 * @param s2 Property
	 * @param s3 Object
	 * @return String with the triple.
	 */
	protected String getTriple(final String s1, final String s2, final String s3) {
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
	public abstract StringBuilder getNodeSPARQL(final Node n);

	/** Return the JSON representation of an edge.
	 *
	 * @param e Edge.
	 * @return SPARQL notation of the edge content.*/
	public abstract StringBuilder getEdgeSPARQL(final Edge e);

	/** Return the SPARQL representation of a graph.
	 *
	 * @param g Graph
	 * @param insert if true, data are inserted, otherwise deleted.
	 * @return SPARQL notation of the graph content.*/
	public StringBuilder getGraphSPARQL(final Graph g, final boolean insert) {
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
