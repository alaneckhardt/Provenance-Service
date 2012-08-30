package provenanceService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Class helps handling session and request parameters. Request and session parameter has to be given to this class.
 * @author Alan Eckhardt a.e@centrum.cz
 * @version 1.0
 */
public class ParameterHelper {
	HttpServletRequest req;
	HttpSession sess;
	public ParameterHelper(final HttpServletRequest req, final HttpSession sess){
		this.req = req;
		this.sess = sess;
	}
	/**
	 * Return the value of parameter in request or attribute in session. Request has priority.
	 * If neither is defined, return the default value.
	 * @param name
	 * @param defaultValue Value that is used if request value is null
	 * @return
	 */
	public Object getParameter(final String name, final Object defaultValue){
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
	 * @param defaultValue Value that is used if session value is null
	 * @return
	 */
	public Object getSessionParameter(final String name, final Object defaultValue){
		Object par = sess.getAttribute(name);		
		if(par != null && !"null".equals(par)){
			return par;
		}
		return defaultValue;
	}
	
	public void setSessionParameter(final String name, final Object value){
		sess.setAttribute(name, value);	
	}

	/**
	 * Return the value of parameter in request. 
	 * If it is not defined, return the default value.
	 * @param name
	 * @param defaultValue Value that is used if request value is null
	 * @return
	 */
	public Object getRequestParameter(final String name, final Object defaultValue){
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
	
	public void setRequestParameter(final String name, final Object value){
		req.setAttribute(name, value);	
	}
	
	
	public HttpServletRequest getReq() {
		return req;
	}


	public void setReq(final HttpServletRequest req) {
		this.req = req;
	}


	public HttpSession getSess() {
		return sess;
	}


	public void setSess(final HttpSession sess) {
		this.sess = sess;
	}
	
	

}
