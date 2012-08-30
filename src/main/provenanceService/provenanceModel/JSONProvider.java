package provenanceService.provenanceModel;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;


/**
 * Class for transformation between the Graph and JSON representation of the
 * provenance.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public interface JSONProvider {
	/**
	 * Method for necessary initialization.
	 */
	void init();
	
	/**
	 * Gets the properties of the node from RDF repository. Does not load the
	 * adjacencies though, in order to avoid greedy crawl of the whole graph.
	 *
	 * @param g Graph where the node should be added.
	 * @param j JSON representation of the node
	 * @return The Node with filled properties without the adjacencies.
	 * @throws QueryEvaluationException
	 * @throws MalformedQueryException
	 * @throws RepositoryException
	 */
	Node getNode(final Graph g,final  JSONObject j);

	/**
	 * Gets Edge from given JSONObject. Requires that both from and to nodes are
	 * already in the graph!
	 *
	 * @param g Graph where the edge should be added.
	 * @param edge JSON representation of the edge.
	 * @return Edge created out of the JSON.
	 */
	Edge getEdge(final Graph g, final JSONObject edge);

	/**
	 * Gets the Graph from JSON representation.
	 *
	 * @param graph JSON Array to be turned into Graph.
	 * @return Graph out of the JSON Array.
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	Graph getGraph(final  JSONArray graph);

	/**
	 * Return the JSON representation of a node.
	 *
	 * @param n Node to be turned into JSON
	 * @return JSON representation ofthe node.
	 */
	JSONObject getNodeJSON(final Node n);

	/**
	 * Return the JSON representation of an edge.
	 *
	 * @param e Edge to be turned into JSON.
	 * @return JSON representation ofthe edge.
	 */
	JSONObject getEdgeJSON(final Edge e);

	/**
	 * Return the JSON representation of a graph.
	 *
	 * @param g Graph to be turned into JSON.
	 * @return json array of JSON representation of nodes.
	 */
	JSONArray getGraphJSON(final Graph g);
}
