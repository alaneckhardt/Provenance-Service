package provenanceService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Class helps handling session and request parameters. Request and session parameter has to be given to this class.
 * @author AE
 * @version 1.0
 */
public class ParameterHelper {
	HttpServletRequest req;
	HttpSession sess;
	public ParameterHelper(HttpServletRequest req, HttpSession sess){
		this.req = req;
		this.sess = sess;
	}
	/**
	 * Return the value of parameter in request or attribute in session. Request has priority.
	 * If neither is defined, return the default value.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public Object getParameter(String name, Object defaultValue){
		Object par = req.getParameter(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}
		par = req.getAttribute(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}par = sess.getAttribute(name);		
		if(par != null && !"null".equals(par)){
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
	public Object getSessionParameter(String name, Object defaultValue){
		Object par = sess.getAttribute(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}
		return defaultValue;
	}
	
	public void setSessionParameter(String name, Object value){
		sess.setAttribute(name, value);	
	}

	/**
	 * Return the value of parameter in request. 
	 * If it is not defined, return the default value.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public Object getRequestParameter(String name, Object defaultValue){
		Object par = req.getParameter(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}
		par = req.getAttribute(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}
		return defaultValue;
	}
	
	public void setRequestParameter(String name, Object value){
		req.setAttribute(name, value);	
	}
	
	
	public HttpServletRequest getReq() {
		return req;
	}


	public void setReq(HttpServletRequest req) {
		this.req = req;
	}


	public HttpSession getSess() {
		return sess;
	}


	public void setSess(HttpSession sess) {
		this.sess = sess;
	}
	
	

}
