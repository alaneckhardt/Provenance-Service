package provenanceService.provenanceModel;

import provenanceService.ProvenanceServiceException;
import provenanceService.ProvenanceServiceImpl;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * 
 * @author Alan Eckhardt a.e@centrum.cz
 */
public abstract class ProvenanceModel{

	protected JSONProvider JSONProvider;
	protected RDFProvider RDFProvider;
	protected SPARQLProvider SPARQLProvider;
	protected DataProvider dataProvider;

	public DataProvider getDataProvider() {
		return dataProvider;
	}
	public void setDataProvider(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	protected ProvenanceServiceImpl psi;
	
	public ProvenanceModel(ProvenanceServiceImpl psi){
		this.psi = psi;
	}
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
	public abstract String addCausalRelationship(Model mmodel, final String type, final String from, final String to) throws ProvenanceServiceException;

	public JSONProvider getJSONProvider() {
		return JSONProvider;
	}
	public void setJSONProvider(JSONProvider jsonProvider) {
		this.JSONProvider = jsonProvider;
	}
	public RDFProvider getRDFProvider() {
		return RDFProvider;
	}
	public void setRDFProvider(RDFProvider rDFProvider) {
		RDFProvider = rDFProvider;
	}
	public SPARQLProvider getSPARQLProvider() {
		return SPARQLProvider;
	}
	public void setSPARQLProvider(SPARQLProvider sPARQLProvider) {
		SPARQLProvider = sPARQLProvider;
	}
	public ProvenanceServiceImpl getPsi() {
		return psi;
	}
	public void setPsi(ProvenanceServiceImpl psi) {
		this.psi = psi;
	}
	/** Create an instance of given type.
	 *
	 * @param type
	 *            Type of the node
	 * @param sessionId Id of the session.
	 * @return URI of the new resource.
	 * @throws ProvenanceServiceException */
	public abstract String addNode(Model model, final String type) throws ProvenanceServiceException;
}