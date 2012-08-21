package provenanceService.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.openrdf.OpenRDFException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import provenanceService.DataProvider;
import provenanceService.Edge;
import provenanceService.Graph;
import provenanceService.Node;
import provenanceService.Properties;
import provenanceService.ProvenanceServiceImpl;
import provenanceService.provenanceModel.OPMDataProvider;
import provenanceService.provenanceModel.PROVODataProvider;
import provenanceService.provenanceModel.SPARQLProvider;


public class TestFileAccess extends TestCase {   
	
	public static DataProvider tempDataProvider;
	public TestFileAccess (String testName) {
		super (testName);
		Properties.setFile("test.properties");
	}
	public TestFileAccess(){
		Properties.setFile("test.properties");
	}
	public static Test suite() {
			TestSuite suite = new TestSuite();
			   suite.addTest(new TestFileAccess ("changeData"));
			   suite.addTest(new TestFileAccess ("revertData"));
		   return suite;
		}
		
	public void revertData(){
		AllTests.impl.setDataProvider(tempDataProvider);
	}
	public void changeData(){

		String fileName = "SimpleWERCM.owl";
		String namespace = "http://www.local-fp7.com/ontologies/WERCM-0.0/runs/SimpleWERCM-0.0-run1234-provenance.owl#";
		fileName = "test.ttl";
		namespace = "http://www.local-fp7.com/ontologies/WERCM-0.0/runs/SimpleWERCM-0.0-run1234-provenance.owl#";
		FileInputStream in;
		try {
			in = new FileInputStream(fileName);
			//OntModel resource = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			OntModel model = ModelFactory.createOntologyModel();
			
			
			model = (OntModel) model.read(in,namespace, "TTL");
			ProvenanceServiceImpl ps = AllTests.impl;
			DataProvider dp = new PROVODataProvider();
			dp.init(AllTests.impl);
			dp.setOntologies(model);
			dp.getOntologies().add(ps.getDataProvider().getOntologies());
			tempDataProvider = ps.getDataProvider();
			ps.setDataProvider(dp);
			
			ps.initEdges();
			ps.initNodes();
			String provSession = ps.startSession();
			ps.addRDFGraph(provSession, model);
			Graph g = ps.getProvenance("http://www.policygrid.org/provenance-generic.owl#Ent2", provSession);
			int a = 1;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenRDFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
