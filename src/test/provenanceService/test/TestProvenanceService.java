package provenanceService.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.ProvenanceServiceImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class TestProvenanceService extends TestCase {   
	ProvenanceServiceImpl impl = ProvenanceService.getSingleton();
	public TestProvenanceService (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestProvenanceService(){
		Properties.setFile("test.properties");
		impl = ProvenanceService.getSingleton();
	}
	public static Test suite() {
		TestSuite suite = new TestSuite();
		   suite.addTest(new TestProvenanceService ("testInsertSmallGraph"));
		   suite.addTest(new TestProvenanceService ("testConvertSmallGraph"));
		   suite.addTest(new TestProvenanceService ("testDeleteGraph"));
		   suite.addTest(new TestProvenanceService ("testClean"));
		   return suite;
		}

	Graph g = AllTests.getTestGraph();
	
	private void checkGraph(Graph g){
		for (int i = 0; i < g.size(); i++) {
				Node n = g.get(i);
				Node n2 = AllTests.impl.getProvProvider().getDataProvider().getNode(null, n.getId());
				assertTrue(n.equals(n2));
				for(Edge e : n.getAdjacencies()){
					Edge e2 = AllTests.impl.getProvProvider().getDataProvider().getEdge(null, e.getId());
					assertTrue(e.equals(e2));					
				}
		}
	}
	private void checkRDFGraph(Graph g, Model m){
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			Resource r = m.getResource(n.getId());
			Node n2 = AllTests.impl.getProvProvider().getRDFProvider().getNode(null, r);
			assertTrue(n.equals(n2));
			for(Edge e : n.getAdjacencies()){
				Resource eRes = m.getResource(e.getId());
				Edge e2;
					e2 = AllTests.impl.getProvProvider().getRDFProvider().getEdge(null, eRes);
					assertTrue(e.equals(e2));	
			}
		}
	}
	

	private void checkJSONGraph(Graph g, JSONArray json){
		Graph tmpG = new Graph();
		//Check the nodes, construct the temporary graph.
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			boolean found = false;
			for (int j = 0; j < json.size(); j++) {
				JSONObject node = json.getJSONObject(j);
				Node n2 = AllTests.impl.getProvProvider().getJSONProvider().getNode(tmpG, node);
				if (n.equals(n2)) {
					found = true;
					tmpG.addNode(n2);
				}
			}
			if (!found)
				fail("No node " + n.getId());
		}
		//Now check the edges with the graph used.
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			for (int j = 0; j < json.size(); j++) {
				JSONObject node = json.getJSONObject(j);
				Node n2 = AllTests.impl.getProvProvider().getJSONProvider().getNode(tmpG, node);
				if (n.equals(n2)) {
					boolean foundE = false;
					for (Edge e : n.getAdjacencies()) {
						foundE = false;
						JSONArray edges = node.getJSONArray("adjacencies");
						for (Object edge : edges) {
							Edge edge2 = AllTests.impl.getProvProvider().getJSONProvider().getEdge(tmpG,	(JSONObject) edge);
							if (e.equals(edge2)) {
								foundE = true;
							}
						}
						if (!foundE)
							fail("No edge " + e.getId());
					}
				}
			}
		}
	}
	public void testInsertSmallGraph() {
		try {
			String session = impl.startSession();
			impl.addJSONGraph(session, AllTests.impl.getProvProvider().getJSONProvider().getGraphJSON(g).toString());
			impl.commit(session);
			checkGraph(g);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	public void testConvertSmallGraph() {
		String session = impl.startSession();
		try {
			impl.addJSONGraph(session, AllTests.impl.getProvProvider().getJSONProvider().getGraphJSON(g).toString());
			Model m = impl.getModel(session);
			JSONArray json = impl.getJSONGraph(session);
			checkRDFGraph(g, m);
			checkJSONGraph(g, json);
			impl.rollback(session);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} finally {
			impl.rollback(session);			
		}
	}

	public void testClean() {
		String session = impl.startSession();
		try {
			impl.addJSONGraph(session, AllTests.impl.getProvProvider().getJSONProvider().getGraphJSON(g).toString());
			Model m = impl.getModel(session);
			m.add(m.getResource("http://openprovenance.org/ontology#Resource1"), m.getProperty("http://testProperty"), "Value");
			impl.clean(session);
			Resource r = m.getResource("http://openprovenance.org/ontology#Resource1");
			Object o = r.getProperty(m.getProperty("http://testProperty"));
			assertTrue(o==null);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} finally {
			impl.rollback(session);			
		}
	}
	public void testDeleteGraph(){
		for (int i = 0; i < g.size(); i++) {
			try {
				Node n = g.get(i);
				AllTests.deleteTemporary( n.getId());
				Node nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, n.getId());
				assertTrue(nControl == null || nControl.getType() == null);
				
				for(Edge e : n.getAdjacencies()){
					AllTests.deleteTemporary( e.getId());
					Edge eControl = AllTests.impl.getProvProvider().getDataProvider().getEdge(null, e.getId());
					assertTrue(eControl == null);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
	}
	
}
