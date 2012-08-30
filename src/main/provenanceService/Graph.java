package provenanceService;

import java.util.ArrayList;
import java.util.List;
/**
 * Class representing a provenance graph.
 * @author Alan Eckhardt a.e@centrum.cz
 */
public class Graph {
	/**List of the Nodes in the graph.*/
	private List<Node> nodes = new ArrayList<Node>();

	/**
	 * Empty constructor.
	 */
	public Graph(){
	}
	/**
	 * Returns true if the graph contains a node with given URI.
	 * @param id URI of the node to search for.
	 * @return true if the graph contains a node with given URI.
	 */
	public boolean contains(final String id){
		for(Node n : nodes){
			if(n.getId().equals(id))
				return true;
		}
		return false;
	}
	/**
	 * Returns true if the graph contains the given node.
	 * @param n Node to search for.
	 * @return true if the graph contains the node.
	 */
	public boolean contains(final Node n){
		for(Node n2 : nodes){
			if(n2.equals(n))
				return true;
		}
		return false;
	}
	/**
	 * Returns the node with given URI.
	 * @param id URI of the node to search for.
	 * @return the node with given URI.
	 */
	public Node getNode(final String id){
		for(Node n : nodes){
			if(n.getId().equals(id))
				return n;
		}
		return null;
	}
	/**
	 * Adds node to the graph.
	 * @param n Node to add.
	 */
	public void addNode(final Node n){
		if(!nodes.contains(n) && n != null)
			nodes.add(n);
	}
	/**
	 * Add edge to the graph.
	 * @param e Edge to add.
	 */
	public void addEdge(final Edge e){
		if(e == null)
			return;
		Node from = e.getFrom();
		Node to = e.getTo();
		addNode(from);
		addNode(to);
		Node from2 = getNode(from.getId());
		Node to2 = getNode(to.getId());
		from2.addAdjacency(e);
		to2.addAdjacency(e);
	}
	/**
	 * @return all nodes in the graph.
	 */
	public List<Node> getNodes() {
		return nodes;
	}
	/**
	 * Sets the nodes of the graph, replacing existing ones.
	 * @param nodes Node to be used as the graph.
	 */
	public void setNodes(final List<Node> nodes) {
		this.nodes = nodes;
		for(int i = 0; i<this.nodes.size();i++){
			if(this.nodes.get(i) == null){
				this.nodes.remove(i);
				i--;
			}
		}
	}

	/**
	 * @return Number of nodes.
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * @param i Index of the node to return.
	 * @return i-th node in the list.
	 */
	public Node get(final int i){
		return nodes.get(i);
	}
	/**
	 * Merges two graphs.
	 * @param g2 A graph to be added to the current one.
	 * @return A new graph containing nodes and edges from both graphs.
	 */
	public Graph merge(final Graph g2){
		Graph g = new Graph();
		for(Node n:this.nodes){
			g.addNode(n);
		}
		if(g2 == null)
			return g;
		for(Node n:g2.nodes){
			Node n3 = g.getNode(n.getId());
			//n isn't in list
			if(n3 == null)
				g.addNode(n);
			//n is in list, merge edges
			else{
				for(Edge e:n.getAdjacencies()){
					boolean foundEdge = false;
					for(Edge e2:n3.getAdjacencies()){
						if(e2.getId().equals(e.getId())){
							foundEdge = true;
							break;
						}
					}
					//Add a new edge
					if(!foundEdge)
						n3.getAdjacencies().add(e);
				}
			}
		}
		return g;
	}
}
