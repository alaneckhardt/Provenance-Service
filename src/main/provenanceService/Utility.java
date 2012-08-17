package provenanceService;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/** Library of useful functions.
 *
 * @author AE */
public final class Utility {
	/** Private constructor. */
	private Utility() {
	};

	/** @param prop URI of the property.
	 * @return Property. */
	public static Property getProp(final String prop) {
		return ResourceFactory.createProperty(Properties.getString(prop));
	}

	/** @param s String.
	 * @return true, if the string is a URI. */
	public static boolean isURI(final String s) {
		IRIFactory iriFactory = IRIFactory.semanticWebImplementation();
		boolean includeWarnings = false;
		IRI iri;
		iri = iriFactory.create(s);
		// Literal
		if (iri.hasViolation(includeWarnings))
			return false;
		// Resource
		else
			return true;
		/*try {
		 * new URI( s);
		 * } catch (URISyntaxException e) {
		 * return false;
		 * }
		 * return true; */

	}
	/**
	 * Check if the arguments are the same or both null.
	 * @param s1 Argument 1
	 * @param s2 Argument 2
	 * @return True if same or both nulls.
	 */
	public static boolean isSameOrNull(final Object s1,final  Object s2) {
		if (s1 == null && s2 == null)
			return true;
		if (s1 == null || s2 == null)
			return false;
		return s1.equals(s2);
	}

	/** Returns the local name part of the uri.
	 *
	 * @param uri URI
	 * @return Local name of URI
	 */
	public static String getLocalName(final String uri) {
		if (uri.contains("#")) {
			return uri.substring(uri.lastIndexOf("#") + 1);
		}
		if (uri.contains("/")) {
			return uri.substring(uri.lastIndexOf("/") + 1);
		}
		return uri;
	}

	/** Returns the namespace part of the uri.
	 *
	 * @param uri URI
	 * @return the namespace part of the uri */
	public static String getNamespace(final String uri) {
		if (uri.contains("#")) {
			return uri.substring(0, uri.lastIndexOf("#") - 1);
		}
		if (uri.contains("/")) {
			return uri.substring(0, uri.lastIndexOf("/") - 1);
		}
		return uri;
	}

}
