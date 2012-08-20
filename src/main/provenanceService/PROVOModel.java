package provenanceService;

import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;


public class PROVOModel extends ProvenanceModel{
	public PROVOModel(ProvenanceServiceImpl psi) {
		super(psi);
	}

	ProvenanceServiceImpl psi;

	/** Adds relationship to the process. Causal relationships are e.g.
	 * controlledBy, Used, wasGeneratedBy,...
	 *
	 * @param sessionId Id of the session.
	 *            URI of the process to be the relation added to.
	 * @param type
	 *            The type of the causal relationship.
	 * @param from
	 *            The subject of the relationship - this can be artifact, agent,
	 *            or other process.
	 * @param to
	 *            The object of the relationship - this can be artifact, agent,
	 *            or other process.
	 * @return URI of the new relationship.
	 * @throws ProvenanceServiceException */
	public String addCausalRelationship(Model model, final String type, final String from, final String to) throws ProvenanceServiceException{
		String relationId = psi.getNewURI();
		Resource relationship = model.createResource(relationId);
		Resource c = model.getResource(from);
		Resource e = model.getResource(to);
		Resource r = model.getResource(type);
		model.add(relationship, RDF.type, r);
		model.add(relationship, Utility.getProp("from"), c);
		model.add(relationship, Utility.getProp("to"), e);
		return relationship.getURI();
	}

	/** Create an instance of given type.
	 *
	 * @param type
	 *            Type of the node
	 * @param sessionId Id of the session.
	 * @return URI of the new resource.
	 * @throws ProvenanceServiceException */
	public String addNode(final Model model, final String type) throws ProvenanceServiceException{
		String instanceId = psi.getNewURI();
		Resource res = model.createResource(instanceId);
		model.createResource(type);
		model.add(res, RDF.type, model.createResource(type));
		return res.getURI();
	}
}