package provenanceService.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.provenanceModel.SPARQLProvider;


public class TestSPARQLProvider extends TestCase {   
	public TestSPARQLProvider (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestSPARQLProvider(){
		Properties.setFile("test.properties");
	}
	public static Test suite() {
		TestSuite suite = new TestSuite();
		   suite.addTest(new TestSPARQLProvider ("testGetNodeSPARQL"));
		   suite.addTest(new TestSPARQLProvider ("testGetEdgeSPARQL"));
		   suite.addTest(new TestSPARQLProvider ("testGetGraphSPARQL"));
		   return suite;
		}
		
	public void testGetNodeSPARQL(){
		try {
			int i = 1;
			Node n = AllTests.getTestNode(i, "http://www.policygrid.org/provenance-generic.owl#Paper");
			StringBuilder node = AllTests.impl.getProvProvider().getSPARQLProvider().getNodeSPARQL(n);
			assertTrue(node != null);
			assertTrue(node.toString().contains(n.getId()));
			assertTrue(node.toString().contains(n.getType()));
			assertTrue(node.toString().contains(n.getTitle()));
			assertTrue(node.toString().contains(n.getId()));
			assertTrue(node.toString().contains(n.getId()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	public void testGetEdgeSPARQL(){
		try {
			int i = 1;
			Node n1 = AllTests.getTestNode(i, null);
			Node n2 = AllTests.getTestNode(i, null);
			Edge e = AllTests.getTestEdge(i,n1,n2, null);
			StringBuilder edge = AllTests.impl.getProvProvider().getSPARQLProvider().getEdgeSPARQL(e);
			assertTrue(edge != null);
			assertTrue(edge.toString().contains(e.getId()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
	public void testGetGraphSPARQL(){
		try {
			Graph g = AllTests.getTestGraph();
			StringBuilder graph = AllTests.impl.getProvProvider().getSPARQLProvider().getGraphSPARQL(g, true);
			assertTrue(graph != null);
			assertTrue(graph.toString().contains(g.get(0).getId()));
			assertTrue(graph.toString().contains(g.get(1).getId()));
			assertTrue(graph.toString().contains(g.get(2).getId()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
}
