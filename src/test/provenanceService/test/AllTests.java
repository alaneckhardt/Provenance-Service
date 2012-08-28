package provenanceService.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.ProvenanceServiceImpl;
import provenanceService.Utility;


public class AllTests extends TestCase {   
	static ProvenanceServiceImpl impl;
	public AllTests (String testName) {
		super (testName);
	}
	public AllTests(){
	}
	public static Test suite() {
		Properties.setBaseFolder("./");
		Properties.setFile("test.properties");
		impl = ProvenanceService.getSingleton();

		impl.initProvenance();
		TestSuite suite = new TestSuite();		
		
		suite.addTest(TestFileAccess.suite());   
		/*suite.addTest(TestSPARQLProvider.suite());   
		suite.addTest(TestRDFProvider.suite());
		suite.addTest(TestJSONProvider.suite());
		suite.addTest(TestProvenanceService.suite());*/
		return suite;
	}	
	public static Node getTestNode(int i, String type){
		Node n = new Node("http://openprovenance.org/ontology#Resource"+i);
		if(type != null){
			n.setTitle(Utility.getLocalName(type)+i);
			n.setType(type);
			n.setBasicType(impl.getShape(type));
		}
		int j = 0;
		impl.getProvProvider().getDataProvider().init(impl);
		for(String prop : impl.getProvProvider().getDataProvider().getCustomProperties()){
			n.addProperty(prop, "TestProp"+i*10+j);
			j++;
		}
		return n;
	}
	public static Node getTestNodeNoTitle(int i, String type){
		if(type == null)
			type = "http://www.policygrid.org/provenance-generic.owl#Paper";
		Node n = new Node("http://openprovenance.org/ontology#Resource"+i);
		n.setType(type);
		n.setBasicType(impl.getShape(type));		
		return n;
	}
	public static Edge getTestEdge(int i, Node n1, Node n2, String type){
		if(type == null)
			type = "http://openprovenance.org/ontology#WasGeneratedBy";
		Edge e = new Edge("http://openprovenance.org/ontology#Edge"+i);
		e.setType(type);
		e.setFrom(n1);
		e.setTo(n2);
		n1.addAdjacency(e);
		n2.addAdjacency(e);
		return e;
	}
	public static void deleteTemporary(String subject) {
		try {

			/*	StringBuffer qry = new StringBuffer(1024);
				qry.append("construct { ");
				qry.append("<"+subject+"> ?p ?o. } where { ");
				qry.append("<"+subject+"> ?p ?o. } ");
				String query = qry.toString();
				impl.getProvProvider().getDataProvider().init(impl);
				impl.getProvProvider().getDataProvider().connect();
				GraphQuery q = impl.getProvProvider().getDataProvider().getCon().prepareGraphQuery(QueryLanguage.SPARQL, query);				
				GraphQueryResult result = q.evaluate();
				impl.getProvProvider().getDataProvider().getCon().remove(result);
				impl.getProvProvider().getDataProvider().getCon().commit();
				impl.getProvProvider().getDataProvider().disconnect();*/
			} catch (Exception e) {
			fail(e.getLocalizedMessage());			
		}
	}

	public static Graph getTestGraph(){
		Graph g = new Graph();
		int i = 1;
		Node res1 = AllTests.getTestNode(i++, "http://www.policygrid.org/provenance-generic.owl#Paper");
		Node proc1 = AllTests.getTestNode(i++, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
		Node agent1 = AllTests.getTestNode(i++, "http://xmlns.com/foaf/0.1/Person");
		Node res2 = AllTests.getTestNode(i++, "http://www.policygrid.org/provenance-generic.owl#Paper");
		Node proc2 = AllTests.getTestNodeNoTitle(i++, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
		Node agent2 = AllTests.getTestNode(i++, "http://xmlns.com/foaf/0.1/Person");
		
		g.addNode(agent1);
		g.addNode(proc1);
		g.addNode(res1);
		g.addNode(agent2);
		g.addNode(proc2);
		g.addNode(res2);
		Edge e = AllTests.getTestEdge(i++, proc1, res1,  "http://openprovenance.org/ontology#WasGeneratedBy");
		Edge e2 = AllTests.getTestEdge(i++, agent1, proc1,   "http://openprovenance.org/ontology#WasControlledBy");
		Edge e3 = AllTests.getTestEdge(i++, proc2, res2,  "http://openprovenance.org/ontology#WasGeneratedBy");
		Edge e4 = AllTests.getTestEdge(i++, agent2, proc2,   "http://openprovenance.org/ontology#WasControlledBy");
		return g;
	}
	
	public static void testEdge(Edge e){
		assertTrue(e != null);
		e.getFrom();
		e.getTo();
		e.getId();
		e.getProperties();
		e.getProperty("");
		e.getType();
	}
	public static void testNode(Node n){
		assertTrue(n != null);
		n.getAdjacencies();
		n.getBasicType();
		n.getId();
		n.getProperties();
		n.getProperty("");
		n.getTitle();
		n.getType();
	}
}
