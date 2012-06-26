package provenanceService;

import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
/**
 * Class for transformation between the Graph and JSON representation of the provenance.
 * @author AE
 *
 */
public class JSONProvider {
		
	/**
	 * Nothing at the moment.
	 */
	public static void init(){
		
	}
	/**
	 * Gets the properties of the node from RDF repository. Does not load the adjacencies though, in order to avoid greedy crawl of the whole graph.
	 * @param g
	 * @param resource
	 * @return The Node with filled properties without the adjacencies.
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws RepositoryException 
	 */
	public static Node getNode(Graph g, JSONObject j) {		
		Node node = null;
		if(g != null)
			node = g.getNode(j.get("id").toString());
		//Node is in the graph
		if(node != null)
			return node;
		
		node = new Node(j.get("id").toString());
		if(j.containsKey("title"))
			node.setTitle(j.get("title").toString());
		if(j.containsKey("fullType"))
			node.setType(j.get("fullType").toString());
		if(j.containsKey("basicType"))
			node.setBasicType(j.get("basicType").toString());
		node.setAdjacencies(new ArrayList<Edge>());
		if(j.containsKey("properties")){
			JSONArray properties = j.getJSONArray("properties");
			for (Object p : properties) {
				JSONObject prop = (JSONObject)p;
				node.addProperty(prop.getString("name"), prop.get("value"));
			}
		}
		return node;					
	}
	
	/**
	 * Gets Edge from given JSONObject. Requires that both from and to nodes are already in the graph!
	 * @param g
	 * @param edge
	 * @return
	 */
	public static Edge getEdge(Graph g, JSONObject edge) {		
		Edge e = new Edge(edge.get("id").toString());
		e.setType(edge.get("type").toString());
		if(g != null)
			e.setFrom(g.getNode(edge.get("from").toString()));
		//From is not in the graph - create node with only the id
		if(e.getFrom() == null){
			JSONObject tmp = new JSONObject();
			tmp.put("id", edge.get("from"));
			e.setFrom(getNode(g, tmp));
		}
		if(g != null)
			e.setTo(g.getNode(edge.get("to").toString()));		
		//To is not in the graph - create node with only the id
		if(e.getTo() == null){
			JSONObject tmp = new JSONObject();
			tmp.put("id", edge.get("to"));
			e.setTo(getNode(g, tmp));
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
	 * Gets the Graph from JSON representation. 
	 * @param g
	 * @param j
	 * @return
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public static Graph getGraph(Graph g, JSONArray graph){
		Graph gNew = new Graph();
		for (int i = 0; i < graph.size(); i++) {
			JSONObject node = (JSONObject) graph.get(i);
			Node n = getNode(gNew, node);
			gNew.addNode(n);			
		}
		for (int i = 0; i < graph.size(); i++) {
			JSONObject node = (JSONObject) graph.get(i);
			JSONArray edges = (JSONArray)node.get("adjacencies");
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
	 * @param n
	 * @return
	 */
	public static JSONObject getNodeJSON(Node n){
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
	 * @param e
	 * @return
	 */
	public static JSONObject getEdgeJSON(Edge e){		
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
	 * @param g
	 * @return
	 */
	public static JSONArray getGraphJSON(Graph g){
		if(g == null)
			return null;
		JSONArray graph = (JSONArray) new JSONArray();
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			JSONObject node = getNodeJSON(n);
			for (int j = 0; j < n.getAdjacencies().size(); j++) {
				Edge e = n.getAdjacencies().get(j);
				JSONObject edge = getEdgeJSON(e);
				((JSONArray)node.get("adjacencies")).add(edge);
			}
			graph.add(node);
		}
		return graph;
	}
}
