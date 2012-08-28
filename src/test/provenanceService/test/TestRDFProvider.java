package provenanceService.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import provenanceService.Edge;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.provenanceModel.RDFProvider;


public class TestRDFProvider extends TestCase {   
	public TestRDFProvider (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestRDFProvider(){
		Properties.setFile("test.properties");
	}
	public static Test suite() {
		TestSuite suite = new TestSuite();
		   suite.addTest(new TestRDFProvider ("testConnection"));
		   suite.addTest(new TestRDFProvider ("testInsertNode"));
		   suite.addTest(new TestRDFProvider ("testInsertEdge"));
		   suite.addTest(new TestRDFProvider ("testDelete"));
		   return suite;
		}
	
	public void testConnection() {
		try {
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}

	public void testInsertNode() {
		try {
			
			Node n = AllTests.getTestNode(1, null);
			AllTests.deleteTemporary( n.getId());			
			AllTests.impl.getProvProvider().getDataProvider().insertNode(n);
			Node nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, n.getId());
			assertTrue(nControl != null);
			assertTrue(nControl.equals(n));
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}
	public void testInsertEdge() {
		try {
			
			Node n = AllTests.getTestNode(1, null);
			Node n2 = AllTests.getTestNode(2, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
			Edge e = AllTests.getTestEdge(3,n,n2,null);
			AllTests.deleteTemporary( n.getId());
			AllTests.deleteTemporary( n2.getId());
			AllTests.deleteTemporary( e.getId());
			
			AllTests.impl.getProvProvider().getDataProvider().insertNode(n);
			Node nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, n.getId());
			assertTrue(nControl != null);
			assertTrue(nControl.equals(n));
			nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, n2.getId());
			assertTrue(nControl != null);
			assertTrue(nControl.equals(n2));
			Edge nControlE = AllTests.impl.getProvProvider().getDataProvider().getEdge(null, e.getId());
			assertTrue(nControlE != null);
			assertTrue(nControlE.equals(e));
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}

	public void testInsertGraph() {
		//TODO
	}
	public void testDelete() {
		Node nControl;
			Node n = AllTests.getTestNode(1, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
			Node n2 = AllTests.getTestNode(2, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
			Edge e = AllTests.getTestEdge(3,n,n2, "http://www.policygrid.org/ourspacesVRE.owl#UploadResource");
			AllTests.deleteTemporary( n.getId());
			AllTests.deleteTemporary( n2.getId());
			AllTests.deleteTemporary( e.getId());
			nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, "http://openprovenance.org/ontology#Resource1");
			assertTrue(nControl == null || nControl.getType() == null);
			nControl = AllTests.impl.getProvProvider().getDataProvider().getNode(null, "http://openprovenance.org/ontology#Process1");
			assertTrue(nControl == null || nControl.getType() == null);
			Edge nControlE = AllTests.impl.getProvProvider().getDataProvider().getEdge(null, "http://openprovenance.org/ontology#Edge1");
			assertTrue(nControlE == null || nControlE.getType() == null);
		
	}
	
}
