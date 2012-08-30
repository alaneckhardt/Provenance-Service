package provenanceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Class representing a node in the provenance graph. It could be a process,
 * agent or a resource, though no check of the type is performed.
 * @author Alan Eckhardt a.e@centrum.cz
 */
public class Node {

	/**
	 * One of Agent, Artifact and Process
	 */
	private String basicType = null;
	private String type = null;
	private String id = null;
	private String title = null;
	private List<Edge> adjacencies = new ArrayList<Edge>();
	/** Custom properties, such as timestamp. */
	private Map<String, Object> properties = new HashMap<String, Object>();

	public Node(String id) {
		if (id == null)
			throw new NullPointerException();
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void addAdjacency(Edge e) {
		if (!adjacencies.contains(e))
			adjacencies.add(e);
	}

	public List<Edge> getAdjacencies() {
		return adjacencies;
	}

	public void setAdjacencies(List<Edge> adjacencies) {
		this.adjacencies = adjacencies;
	}

	public String getBasicType() {
		return basicType;
	}

	public void setBasicType(String basicType) {
		this.basicType = basicType;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Node) {
			Node n2 = (Node) arg0;
			if (Utility.isSameOrNull(n2.id, this.id)
			/* &&
			 * Utility.isSameOrNull(n2.basicType,this.basicType) &&
			 * Utility.isSameOrNull(n2.type,this.type) &&
			 * Utility.isSameOrNull(n2.title,this.title) */
			) {
				return true;
			} else
				return false;
		} else if (arg0 instanceof JSONObject) {
			JSONObject n2 = (JSONObject) arg0;
			if (Utility.isSameOrNull(n2.getString("id"), this.id)
			/* &&
			 * Utility.isSameOrNull(n2.getString("basicType"),this.basicType)
			 * &&
			 * Utility.isSameOrNull(n2.getString("fullType"),this.type) &&
			 * Utility.isSameOrNull(n2.getString("title"),this.title) */
			) {
				return true;
			} else
				return false;

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
