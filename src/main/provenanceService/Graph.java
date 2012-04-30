package provenanceService;

import java.util.ArrayList;
import java.util.List;
/**
 * Class representing a provenance graph.
 * @author AE
 *
 */
public class Graph {	
	List<Node> nodes = new ArrayList<Node>();

	public Graph(){
		
	}
	
	public boolean contains(String id){
		for(Node n : nodes){
			if(n.getId().equals(id))
				return true;
		}
		return false;
	}
	public boolean contains(Node n){
		for(Node n2 : nodes){
			if(n2.equals(n))
				return true;
		}
		return false;
	}
	public Node getNode(String id){
		for(Node n : nodes){
			if(n.getId().equals(id))
				return n;
		}
		return null;
	}
	public void addNode(Node n){
		if(!nodes.contains(n) && n != null)
			nodes.add(n);
	}
	public void addEdge(Edge e){
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
	
	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
		for(int i = 0; i<this.nodes.size();i++){
			if(this.nodes.get(i) == null){
				this.nodes.remove(i);
				i--;
			}
		}
	}
	public int size(){
		return nodes.size();
	}
	public Node get(int i){
		return nodes.get(i);
	}
	public Graph merge(Graph g2){
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
