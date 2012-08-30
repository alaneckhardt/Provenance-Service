package provenanceService;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Class representing an edge between two nodes.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 *
 */
public class Edge {
	/**URI of the edge.*/
	private String id;
	/**Type of the edge.*/
	private String type;
	/**Start and end of the edge.*/
	private Node from, to;
	/**Custom properties of the edge.*/
	private Map<String, Object> properties = new HashMap<String, Object>();

	/**
	 * Instantiates a new edge.
	 *
	 * @param id the id
	 */
	public Edge(final String id) {
		if (id == null)
			throw new NullPointerException();
		this.id = id;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type.
	 *
	 * @param type the new type
	 */
	public void setType(final String type) {
		this.type = type;
	}

	/**
	 * Gets the from.
	 *
	 * @return the from
	 */
	public Node getFrom() {
		return from;
	}

	/**
	 * Sets the from.
	 *
	 * @param from the new from
	 */
	public void setFrom(final Node from) {
		this.from = from;
	}

	/**
	 * Gets the to.
	 *
	 * @return the to
	 */
	public Node getTo() {
		return to;
	}
	/**
	 *
	 * @param to Setter for to.
	 */
	public void setTo(final Node to) {
		this.to = to;
	}

	/**
	 * Equals method
	 */
	@Override
	public boolean equals(final Object arg0) {
		if (arg0 instanceof Edge) {
			Edge e2 = (Edge) arg0;
			if (Utility.isSameOrNull(e2.id, this.id)
			/* &&
			 * Utility.isSameOrNull(e2.type,this.type) &&
			 * Utility.isSameOrNull(e2.from,this.from) &&
			 * Utility.isSameOrNull(e2.to,this.to) */
			) {
				return true;
			} else
				return false;
		} else if (arg0 instanceof JSONObject) {
			JSONObject e2 = (JSONObject) arg0;
			if (Utility.isSameOrNull(e2.getString("id"), this.id)
			/* &&
			 * Utility.isSameOrNull(e2.getString("type"),this.type) &&
			 * Utility.isSameOrNull(this.from,e2.get("from")) &&
			 * Utility.isSameOrNull(this.to,e2.get("to")) */
			) {
				return true;
			} else
				return false;
		}
		return false;
	}
	/**
	 * Adds property.
	 * @param name name
	 * @param value value
	 */
	public void addProperty(final String name, final Object value) {
		properties.put(name, value);
	}
	/**
	 * @param name name
	 * @return the value of the property.
	 */
	public Object getProperty(final String name) {
		return properties.get(name);
	}
	/**
	 * @return properties of the edge.
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
	/**
	 * Sets the properties to the edge.
	 * @param properties properties.
	 */
	public void setProperties(final Map<String, Object> properties) {
		this.properties = properties;
	}

}
