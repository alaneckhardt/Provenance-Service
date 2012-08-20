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
import provenanceService.Utility;


public class TestJSONProvider extends TestCase {   
	public TestJSONProvider (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestJSONProvider(){
		Properties.setFile("test.properties");
	}
	public static Test suite() {
		TestSuite suite = new TestSuite();
		   suite.addTest(new TestJSONProvider ("testGetNode"));
		   suite.addTest(new TestJSONProvider ("testGetEdge"));
		   suite.addTest(new TestJSONProvider ("testGetNodeJSON"));
		   suite.addTest(new TestJSONProvider ("testGetEdgeJSON"));
		   return suite;
		}
	
	public JSONObject getTestJSONNode(int i){
		JSONObject node = new JSONObject();
		JSONArray edges = (JSONArray) new JSONArray();
		node.put("title", "Test"+i);			
		node.put("id", "http://openprovenance.org/ontology#Resource"+i);				
		node.put("fullType", "http://www.policygrid.org/provenance-generic.owl#Paper");			
		node.put("basicType", "Artifact");	
		node.put("adjacencies", edges);
		return node;
	}
	public JSONObject getTestJSONEdge(int i,JSONObject n1, JSONObject n2){
		JSONObject edge = (JSONObject) new JSONObject();
		edge.put("to", n1.get("id"));
		edge.put("from", n2.get("id"));
		edge.put("id", "http://openprovenance.org/ontology#Edge"+i);
		edge.put("type", "http://openprovenance.org/ontology#WasGeneratedBy");
		edge.put("typeText", Utility.getLocalName("http://openprovenance.org/ontology#WasGeneratedBy"));
		return edge;
	}
	
	public void testGetNode() {
		try {
			int i = 2;
			Node n = AllTests.impl.getProvProvider().getJSONProvider().getNode(null, getTestJSONNode(i));
			assertTrue(n != null);
			assertTrue(n.getBasicType().equals("Artifact"));
			assertTrue(n.getType().equals("http://www.policygrid.org/provenance-generic.owl#Paper"));
			assertTrue(n.getId().equals("http://openprovenance.org/ontology#Resource"+i));
			assertTrue(n.getTitle().equals("Test"+i));		
			AllTests.testNode(n);	
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}

	public void testGetEdge() {
		try {
			int i = 1;
			JSONObject n1 = getTestJSONNode(i+1);
			JSONObject n2 = getTestJSONNode(i+2);
			JSONObject edge = getTestJSONEdge(1, n1, n2);
			Graph g = new Graph();
			Node node1 = AllTests.impl.getProvProvider().getJSONProvider().getNode(g, n1);
			Node node2 = AllTests.impl.getProvProvider().getJSONProvider().getNode(g, n2);
			g.addNode(node1);
			g.addNode(node2);
			Edge e = AllTests.impl.getProvProvider().getJSONProvider().getEdge(g, edge);
			assertTrue(e != null);
			assertTrue(e.getType().equals("http://openprovenance.org/ontology#WasGeneratedBy"));
			assertTrue(e.getId().equals("http://openprovenance.org/ontology#Edge"+i));
			assertTrue(e.getFrom().equals(n2));
			assertTrue(e.getTo().equals(n1));
			AllTests.testEdge(e);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
	
	public void testGetNodeJSON(){
		try {
			int i = 1;
			Node n = AllTests.getTestNode(i, null);
			JSONObject node = AllTests.impl.getProvProvider().getJSONProvider().getNodeJSON(n);
			assertTrue(node != null);
			assertTrue(n.equals(node));
			i = 2;
			n = AllTests.getTestNode(i, "http://www.policygrid.org/provenance-generic.owl#Paper");
			node = AllTests.impl.getProvProvider().getJSONProvider().getNodeJSON(n);
			assertTrue(node != null);
			assertTrue(n.equals(node));
			i = 3;
			n = AllTests.getTestNodeNoTitle(i, null);
			node = AllTests.impl.getProvProvider().getJSONProvider().getNodeJSON(n);
			assertTrue(node != null);
			assertTrue(n.equals(node));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	public void testGetEdgeJSON(){
		try {
			int i = 1;
			Node n1 = AllTests.getTestNode(i, null);
			Node n2 = AllTests.getTestNode(i, null);
			Edge e = AllTests.getTestEdge(i,n1,n2, null);
			JSONObject edge = AllTests.impl.getProvProvider().getJSONProvider().getEdgeJSON(e);
			assertTrue(edge != null);
			assertTrue(e.equals(edge));
			i = 2;
			n1 = AllTests.getTestNode(i, "http://www.policygrid.org/provenance-generic.owl#Paper");
			n2 = AllTests.getTestNodeNoTitle(i, null);
			e = AllTests.getTestEdge(i,n1,n2, null);
			edge = AllTests.impl.getProvProvider().getJSONProvider().getEdgeJSON(e);
			assertTrue(edge != null);
			assertTrue(e.equals(edge));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
}
