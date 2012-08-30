package provenanceService.provenanceModel;

import java.util.ArrayList;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Utility;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * Class for transformation between the Graph and JSON representation of the
 * provenance.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public final class OPMJSONProvider implements JSONProvider{
	/**
	 * 
	 */
	public OPMJSONProvider(){};
	/**
	 * Nothing at the moment.
	 */
	public void init() {

	}

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
	public Node getNode(final Graph g,final  JSONObject j) {
		Node node = null;
		if (g != null)
			node = g.getNode(j.get("id").toString());
		// Node is in the graph
		if (node != null)
			return node;

		node = new Node(j.get("id").toString());
		if (j.containsKey("title"))
			node.setTitle(j.get("title").toString());
		if (j.containsKey("fullType"))
			node.setType(j.get("fullType").toString());
		if (j.containsKey("basicType"))
			node.setBasicType(j.get("basicType").toString());
		node.setAdjacencies(new ArrayList<Edge>());
		if (j.containsKey("properties")) {
			JSONArray properties = j.getJSONArray("properties");
			for (Object p : properties) {
				JSONObject prop = (JSONObject) p;
				node.addProperty(prop.getString("name"), prop.get("value"));
			}
		}
		return node;
	}

	/**
	 * Gets Edge from given JSONObject. Requires that both from and to nodes are
	 * already in the graph!
	 *
	 * @param g Graph where the edge should be added.
	 * @param edge JSON representation of the edge.
	 * @return Edge created out of the JSON.
	 */
	public Edge getEdge(final Graph g, final JSONObject edge) {
		Edge e = new Edge(edge.get("id").toString());
		e.setType(edge.get("type").toString());
		if (g != null)
			e.setFrom(g.getNode(edge.get("from").toString()));
		// From is not in the graph - create node with only the id
		if (e.getFrom() == null) {
			JSONObject tmp = new JSONObject();
			tmp.put("id", edge.get("from"));
			e.setFrom(getNode(g, tmp));
		}
		if (g != null)
			e.setTo(g.getNode(edge.get("to").toString()));
		// To is not in the graph - create node with only the id
		if (e.getTo() == null) {
			JSONObject tmp = new JSONObject();
			tmp.put("id", edge.get("to"));
			e.setTo(getNode(g, tmp));
		}
		// Add the edge only if everything's all right
		if (e.getFrom() != null && e.getTo() != null) {
			e.getFrom().getAdjacencies().add(e);
			e.getTo().getAdjacencies().add(e);
		} else
			return null;
		return e;
	}

	/**
	 * Gets the Graph from JSON representation.
	 *
	 * @param graph JSON Array to be turned into Graph.
	 * @return Graph out of the JSON Array.
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public Graph getGraph(final  JSONArray graph) {
		Graph gNew = new Graph();
		for (int i = 0; i < graph.size(); i++) {
			JSONObject node = (JSONObject) graph.get(i);
			Node n = getNode(gNew, node);
			gNew.addNode(n);
		}
		for (int i = 0; i < graph.size(); i++) {
			JSONObject node = (JSONObject) graph.get(i);
			JSONArray edges = (JSONArray) node.get("adjacencies");
			for (int j = 0; j < edges.size(); j++) {
				JSONObject edge = (JSONObject) edges.get(j);
				Edge e = getEdge(gNew, edge);
				e.getFrom().getAdjacencies().add(e);
				e.getTo().getAdjacencies().add(e);
			}
		}
		return gNew;
	}

	/**
	 * Return the JSON representation of a node.
	 *
	 * @param n Node to be turned into JSON
	 * @return JSON representation ofthe node.
	 */
	public JSONObject getNodeJSON(final Node n) {
		JSONObject node = new JSONObject();
		JSONArray edges = (JSONArray) new JSONArray();
		node.put("title", n.getTitle());
		node.put("id", n.getId());
		node.put("fullType", n.getType());
		node.put("basicType", n.getBasicType());
		node.put("adjacencies", edges);
		JSONArray properties = new JSONArray();
		for (String name : n.getProperties().keySet()) {
			Object value = n.getProperty(name);
			JSONObject prop = new JSONObject();
			prop.put("name", name);
			prop.put("value", value);
			properties.add(prop);
		}
		node.put("properties", properties);
		return node;
	}

	/**
	 * Return the JSON representation of an edge.
	 *
	 * @param e Edge to be turned into JSON.
	 * @return JSON representation ofthe edge.
	 */
	public JSONObject getEdgeJSON(final Edge e) {
		JSONObject edge = (JSONObject) new JSONObject();
		edge.put("id", e.getId());
		edge.put("to", e.getTo().getId());
		edge.put("from", e.getFrom().getId());
		edge.put("type", e.getType());
		edge.put("typeText", Utility.getLocalName(e.getType()));
		JSONArray properties = new JSONArray();
		for (String name : e.getProperties().keySet()) {
			Object value = e.getProperty(name);
			JSONObject prop = new JSONObject();
			prop.put("name", name);
			prop.put("value", value);
			properties.add(prop);
		}
		edge.put("properties", properties);
		return edge;
	}

	/**
	 * Return the JSON representation of a graph.
	 *
	 * @param g Graph to be turned into JSON.
	 * @return json array of JSON representation of nodes.
	 */
	public JSONArray getGraphJSON(final Graph g) {
		if (g == null)
			return null;
		JSONArray graph = (JSONArray) new JSONArray();
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			JSONObject node = getNodeJSON(n);
			for (int j = 0; j < n.getAdjacencies().size(); j++) {
				Edge e = n.getAdjacencies().get(j);
				JSONObject edge = getEdgeJSON(e);
				((JSONArray) node.get("adjacencies")).add(edge);
			}
			graph.add(node);
		}
		return graph;
	}
}
