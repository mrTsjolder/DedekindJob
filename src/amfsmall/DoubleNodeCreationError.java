package amfsmall;

public class DoubleNodeCreationError extends Exception {

	Object value;
	
	public DoubleNodeCreationError(Object v) {
		value = v;
	}
	
	public String toString() {
		return "Node with value " + value + " exists";
	}
}
