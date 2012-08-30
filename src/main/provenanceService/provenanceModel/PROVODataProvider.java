package provenanceService.provenanceModel;

import java.util.ArrayList;
import java.util.List;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.Utility;

/**
 * Class providing functions for manipulation with RDF. Conversions between
 * Graph and RDF and querying the underlying RDF repository as well.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public class PROVODataProvider extends DataProvider {
	/**
	 * Loads the adjacencies of the given node.
	 *
	 * @param g
	 *            The graph to be used.
	 * @param n
	 *            Node which adjacencies are returned.
	 * @param to
	 *            0=to,1=from,2=both
	 */
	public void getAdjacencies(final Graph g, final Node n,final  int to) {
		try {
			List<String> adjacencies = new ArrayList<String>();
			for (String edgeType : ProvenanceService.getSingleton().getProperties()) {

				if (to == 0)
					adjacencies.addAll(getProperties(n.getId(),PROVOModel.getFromEdge(edgeType)));
				else if (to == 1)
					adjacencies.addAll(getPropertiesTo(n.getId(),PROVOModel.getToEdge(edgeType)));
				else if (to == 2) {
					adjacencies.addAll(getProperties(n.getId(),PROVOModel.getFromEdge(edgeType)));
					adjacencies.addAll(getPropertiesTo(n.getId(),PROVOModel.getToEdge(edgeType)));
				}
			}
			for (String s : adjacencies) {
				Edge e = getEdge(g, s);
				if (e != null && e.getTo() != null && e.getFrom() != null)
					n.getAdjacencies().add(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Gets the properties of the node from RDF repository. Does not load the
	 * adjacencies though, in order to avoid greedy crawl of the whole graph.
	 *
	 * @param g Graph that potentially contains the resource
	 * @param resource URI of the node
	 * @return The Node with filled properties without the adjacencies.
	 */
	public Node getNode(final Graph g, final String resource) {
		if (!Utility.isURI(resource))
			return null;
		Node node = null;
		if (resource == null || resource.equals(""))
			return null;
		if (g != null)
			node = g.getNode(resource);
		// Node is in the graph
		if (node != null)
			return node;

		node = new Node(resource);
		node.setType(getProperty(resource, Properties.getString("type")));
		node.setTitle(getEntityDescription(resource));
		node.setBasicType(impl.getShape(node.getType()));
		node.setAdjacencies(new ArrayList<Edge>());
		loadCustomProperties(node, null);
		return node;
	}

	/**
	 * Finds the edge in the RDF repository.
	 *
	 * @param g Graph that potentially contains the resource
	 * @param edgeURI URI of the edge in the repository.
	 * @return New Edge object.
	 */
	public Edge getEdge(final Graph g,final  String edgeURI) {
		if (edgeURI == null || edgeURI.equals(""))
			return null;
		if (!Utility.isURI(edgeURI))
			return null;
		Edge edge = new Edge(edgeURI);
		edge.setType(getProperty(edgeURI, Properties.getString("type")));
		if ((edge.getType() == null || edge.getType().equals("")))
			return null;

		String from = getPropertyTo(edgeURI, PROVOModel.getFromEdge(edge.getType()));
		if (from == null || !Utility.isURI(from))
			return null;
		if (g != null)
			edge.setFrom(g.getNode(from));
		if (edge.getFrom() == null) {
			edge.setFrom(getNode(g, from));
		}
		if (edge.getFrom() == null)
			return null;

		String to = getProperty(edgeURI,PROVOModel.getToEdge(edge.getType()));
		if (to == null || !Utility.isURI(to))
			return null;
		if (g != null)
			edge.setTo(g.getNode(to));
		if (edge.getTo() == null) {
			edge.setTo(getNode(g, to));
		}
		if (edge.getTo() == null)
			return null;
		return edge;
	}

}
