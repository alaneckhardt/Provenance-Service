package provenanceService.provenanceModel;

import provenanceService.Edge;
import provenanceService.Node;
import provenanceService.Properties;

import com.hp.hpl.jena.vocabulary.RDF;

/** Class for transformation between the Graph and JSON representation of the
 * provenance.
 *
 * @author Alan Eckhardt a.e@centrum.cz
 */
public final class OPMSPARQLProvider extends SPARQLProvider{

	/** Private constructor. */
	public OPMSPARQLProvider() {
	}

	/** Return the JSON representation of a node.
	 *
	 * @param n Node.
	 * @return SPARQL notation of the node content.*/
	public StringBuilder getNodeSPARQL(final Node n) {
		StringBuilder node = new StringBuilder();
		if (n.getTitle() != null)
			node.append(getTriple(n.getId(), Properties.getString("title"), n.getTitle()));

		if (n.getType() != null)
			node.append(getTriple(n.getId(), RDF.type.getURI(), n.getType()));
		for (String name : n.getProperties().keySet()) {
			Object value = n.getProperty(name);
			node.append(getTriple(n.getId(), name, value.toString()));
		}
		return node;
	}

	/** Return the JSON representation of an edge.
	 *
	 * @param e Edge.
	 * @return SPARQL notation of the edge content.*/
	public StringBuilder getEdgeSPARQL(final Edge e) {
		StringBuilder edge = new StringBuilder();
		edge.append(getTriple(e.getId(), Properties.getString("to"), e.getTo().getId()));
		edge.append(getTriple(e.getId(), Properties.getString("from"), e.getFrom().getId()));
		edge.append(getTriple(e.getId(), RDF.type.getURI(), e.getType()));
		for (String name : e.getProperties().keySet()) {
			Object value = e.getProperty(name);
			edge.append(getTriple(e.getId(), name, value.toString()));
		}
		return edge;
	}

}
