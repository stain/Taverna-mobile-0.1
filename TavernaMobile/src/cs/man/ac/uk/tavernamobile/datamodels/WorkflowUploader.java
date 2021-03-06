package cs.man.ac.uk.tavernamobile.datamodels;

import java.io.Serializable;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "uploader")
public class WorkflowUploader extends ElementBase implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2000734822875049386L;
	
	@Text
	protected String value;

	public String getValue() {
		return value;
	}
}
