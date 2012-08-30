package provenanceService.provenanceModel;

import provenanceService.ProvenanceServiceException;
import provenanceService.ProvenanceServiceImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * @author Alan Eckhardt a.e@centrum.cz
 */
public class PROVOModel extends ProvenanceModel{
	public PROVOModel(ProvenanceServiceImpl psi) {
		super(psi);
		JSONProvider = new OPMJSONProvider();
		RDFProvider = new PROVORDFProvider();
		SPARQLProvider = new OPMSPARQLProvider();
		dataProvider = new PROVODataProvider();
	}

	ProvenanceServiceImpl psi;
	public static final String namespace = "http://www.w3.org/ns/prov#";
	public static final String usage = namespace + "Usage";
	public static final String usageFrom = namespace +"qualifiedUsage";
	public static final String usageTo = namespace +"entity";

	public static final String generation = namespace + "Generation";
	public static final String generationFrom =namespace + "qualifiedAssociation";
	public static final String generationTo = namespace + "activity";

	public static final String association = namespace + "Association";
	public static final String associationFrom = namespace + "qualifiedAssociation";
	public static final String associationTo = namespace + "agent";
	
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
		String fromEdge = getFromEdge(type), toEdge = getToEdge(type);
		if(usage.equals(type)){
			fromEdge = usageFrom;
			toEdge = usageTo;
		}
		else if(generation.equals(type)){
			fromEdge = generationFrom;
			toEdge = generationTo;
		}
		else if(association.equals(type)){
			fromEdge = associationFrom;
			toEdge = associationTo;
		}
		Resource relationship = model.createResource(relationId);
		Resource c = model.getResource(from);
		Resource e = model.getResource(to);
		Resource r = model.getResource(type);
		model.add(relationship, RDF.type, r);
		model.add(relationship, ResourceFactory.createProperty(fromEdge), c);
		model.add(relationship, ResourceFactory.createProperty(toEdge), e);
		return relationship.getURI();
	}

	public static String getFromEdge(String type){

		if(usage.equals(type)){
			return usageFrom;
		}
		else if(generation.equals(type)){
			return generationFrom;
		}
		else if(association.equals(type)){
			return associationFrom;
		}
		return "";
	}
	public static String getToEdge(String type){

		if(usage.equals(type)){
			return usageTo;
		}
		else if(generation.equals(type)){
			return generationTo;
		}
		else if(association.equals(type)){
			return associationTo;
		}
		return "";
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