package provenanceService.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.openrdf.OpenRDFException;

import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.JSONProvider;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceService;
import provenanceService.RDFProvider;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class TestProvenanceService extends TestCase {   
	public TestProvenanceService (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestProvenanceService(){
		Properties.setFile("test.properties");
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
			try {
				Node n = g.get(i);
				Node n2 = AllTests.dataProvider.getNode(null, n.getId());
				assertTrue(n.equals(n2));
				for(Edge e : n.getAdjacencies()){
					Edge e2 = AllTests.dataProvider.getEdge(null, e.getId());
					assertTrue(e.equals(e2));					
				}
			} catch (org.openrdf.OpenRDFException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private void checkRDFGraph(Graph g, Model m){
		for (int i = 0; i < g.size(); i++) {
			Node n = g.get(i);
			Resource r = m.getResource(n.getId());
			Node n2 = RDFProvider.getNode(null, r);
			assertTrue(n.equals(n2));
			for(Edge e : n.getAdjacencies()){
				Resource eRes = m.getResource(e.getId());
				Edge e2;
				try {
					e2 = RDFProvider.getEdge(null, eRes);
					assertTrue(e.equals(e2));				
				} catch (OpenRDFException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					fail(e1.getLocalizedMessage());
				}	
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
				Node n2 = JSONProvider.getNode(tmpG, node);
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
				Node n2 = JSONProvider.getNode(tmpG, node);
				if (n.equals(n2)) {
					boolean foundE = false;
					for (Edge e : n.getAdjacencies()) {
						foundE = false;
						JSONArray edges = node.getJSONArray("adjacencies");
						for (Object edge : edges) {
							Edge edge2 = JSONProvider.getEdge(tmpG,	(JSONObject) edge);
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
			String session = ProvenanceService.startSession();
			ProvenanceService.addJSONGraph(session, JSONProvider.getGraphJSON(g).toString());
			ProvenanceService.commit(session);
			checkGraph(g);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	public void testConvertSmallGraph() {
		String session = ProvenanceService.startSession();
		try {
			ProvenanceService.addJSONGraph(session, JSONProvider.getGraphJSON(g).toString());
			Model m = ProvenanceService.getModel(session);
			JSONArray json = ProvenanceService.getJSONGraph(session);
			checkRDFGraph(g, m);
			checkJSONGraph(g, json);
			ProvenanceService.rollback(session);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} finally {
			ProvenanceService.rollback(session);			
		}
	}

	public void testClean() {
		String session = ProvenanceService.startSession();
		try {
			ProvenanceService.addJSONGraph(session, JSONProvider.getGraphJSON(g).toString());
			Model m = ProvenanceService.getModel(session);
			m.add(m.getResource("http://openprovenance.org/ontology#Resource1"), m.getProperty("http://testProperty"), "Value");
			ProvenanceService.clean(session);
			Resource r = m.getResource("http://openprovenance.org/ontology#Resource1");
			Object o = r.getProperty(m.getProperty("http://testProperty"));
			assertTrue(o==null);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		} finally {
			ProvenanceService.rollback(session);			
		}
	}
	public void testDeleteGraph(){
		for (int i = 0; i < g.size(); i++) {
			try {
				Node n = g.get(i);
				AllTests.deleteTemporary( n.getId());
				Node nControl = AllTests.dataProvider.getNode(null, n.getId());
				assertTrue(nControl == null || nControl.getType() == null);
				
				for(Edge e : n.getAdjacencies()){
					AllTests.deleteTemporary( e.getId());
					Edge eControl = AllTests.dataProvider.getEdge(null, e.getId());
					assertTrue(eControl == null);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
	}
	
}
