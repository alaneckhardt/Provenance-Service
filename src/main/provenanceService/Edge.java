package provenanceService;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Class representing an edge between two nodes.
 * @author AE
 *
 */
public class Edge {
	private String id;
	private String type;
	private Node from, to;
	private Map<String, Object> properties = new HashMap<String,Object>();
	
	public Edge(String id){
		if(id == null)
			throw new NullPointerException();
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Node getFrom() {
		return from;
	}
	public void setFrom(Node from) {
		this.from = from;
	}
	public Node getTo() {
		return to;
	}
	public void setTo(Node to) {
		this.to = to;
	}
	@Override
	public boolean equals(Object arg0) {
		if(arg0 instanceof Edge){
			Edge e2 = (Edge)arg0;
			if(Utility.isSameOrNull(e2.id,this.id)/* &&
				Utility.isSameOrNull(e2.type,this.type) &&
				Utility.isSameOrNull(e2.from,this.from) &&
				Utility.isSameOrNull(e2.to,this.to)*/
					){
				return true;
				}
			else return false;
			}
		else if(arg0 instanceof JSONObject){
			JSONObject e2 = (JSONObject)arg0;
			if(Utility.isSameOrNull(e2.getString("id"),this.id) /*&&
					Utility.isSameOrNull(e2.getString("type"),this.type) &&
					Utility.isSameOrNull(this.from,e2.get("from")) &&
					Utility.isSameOrNull(this.to,e2.get("to"))*/
					){
				return true;
				}
			else return false;
			
		}
		return false;
	}
	
	public void addProperty(String name, Object value) {
		properties.put(name, value);
	}
	public Object getProperty(String name) {
		return properties.get(name);
	}
	public Map<String, Object> getProperties() {
		return properties;
	}
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
}
