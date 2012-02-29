package provenanceService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Class helps handling session and request parameters. Request and session parameter has to be given to this class.
 * @author AE
 * @version 1.0
 */
public abstract class ParameterHelper {
	static HttpServletRequest req;
	static HttpSession sess;
	
	/**
	 * Return the value of parameter in request or attribute in session. Request has priority.
	 * If neither is defined, return the default value.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static Object getParameter(String name, Object defaultValue){
		Object par = req.getParameter(name);		
		if(par != null){
			return par;
		}
		par = req.getAttribute(name);		
		if(par != null){
			return par;
		}par = sess.getAttribute(name);		
		if(par != null){
			return par;
		}
		return defaultValue;
	}
	

	/**
	 * Return the value of attribute in session. 
	 * If it is not defined, return the default value.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static Object getSessionParameter(String name, Object defaultValue){
		Object par = sess.getAttribute(name);		
		if(par != null){
			return par;
		}
		return defaultValue;
	}
	
	public static void setSessionParameter(String name, Object value){
		sess.setAttribute(name, value);	
	}

	/**
	 * Return the value of parameter in request. 
	 * If it is not defined, return the default value.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static Object getRequestParameter(String name, Object defaultValue){
		Object par = req.getParameter(name);		
		if(par != null){
			return par;
		}
		par = req.getAttribute(name);		
		if(par != null){
			return par;
		}
		return defaultValue;
	}
	
	public static void setRequestParameter(String name, Object value){
		req.setAttribute(name, value);	
	}
	
	
	public static HttpServletRequest getReq() {
		return req;
	}


	public static void setReq(HttpServletRequest req) {
		ParameterHelper.req = req;
	}


	public static HttpSession getSess() {
		return sess;
	}


	public static void setSess(HttpSession sess) {
		ParameterHelper.sess = sess;
	}
	
	

}
