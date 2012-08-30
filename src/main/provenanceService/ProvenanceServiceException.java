package provenanceService;

/**
 *
 * @author Alan Eckhardt a.e@centrum.cz
 * @version 1.0
 */
public class ProvenanceServiceException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -1021779004547471101L;
	/**
	 *
	 * @param cause Cause of the exception.
	 */
	public ProvenanceServiceException(final Throwable cause){
		super(cause);
	}
	/**
	 * @param message Messge of the exception.
	 *
	 */
	public ProvenanceServiceException(final String message){
		super(message);
	}
}
