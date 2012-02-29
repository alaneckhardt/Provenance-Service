package provenanceService;

/**
 * Library of usefull functions.
 * @author AE
 *
 */
public final class Utility {
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


