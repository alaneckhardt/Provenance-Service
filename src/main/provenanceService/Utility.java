package provenanceService;

import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Library of usefull functions.
 * @author AE
 *
 */
public final class Utility {

	public static Property getProp(String prop){
		return ResourceFactory.createProperty(Properties.getString(prop));
	}
	
	public static boolean isURI(String s){		
		IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		boolean includeWarnings = false;
		IRI iri;
		iri = iriFactory.create(s); 
		//Literal
		if (iri.hasViolation(includeWarnings)) 
			return false;
		//Resource
		else
			return true;
		/*

		try {
			new URI( s);
		} catch (URISyntaxException e) {
			return false;
		}
		return true;*/

	}
	public static boolean isSameOrNull(Object s1, Object s2){
		if(s1 == null && s2 == null)
			return true;
		if(s1 == null || s2 == null)
			return false;
		return s1.equals(s2);
	}
	/**
	 * Returns the local name part of the uri.
	 * @param uri
	 * @return
	 */
	public static String getLocalName(String uri) {		 
		 if (uri.contains("#")) {
			 return uri.substring(uri.lastIndexOf("#")+1);
		 }
		 if (uri.contains("/")) {
			 return uri.substring(uri.lastIndexOf("/")+1);
		 }
		 return uri;
	}

	/**
	 * Returns the namespace part of the uri.
	 * @param uri
	 * @return
	 */
	public static String getNamespace(String uri) {	 
		 if (uri.contains("#")) {
			 return uri.substring(0,uri.lastIndexOf("#")-1);
		 }
		 if (uri.contains("/")) {
			 return uri.substring(0,uri.lastIndexOf("/")-1);
		 }
		 return uri;
	}
	
	
}


