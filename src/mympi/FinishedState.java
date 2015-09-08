package mympi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Map;

import amfsmall.AntiChain;

/**
 * 
 * @author Pieter-Jan Hoedt
 *
 */
public class FinishedState extends ComputationState {

	/** Identification number needed for serialisation. */
	private static final long serialVersionUID = -9188658359294661410L;

	/**
	 * Initialize this state object with a previous state.
	 * 
	 * @param 	previous 
	 *       	The previous state the computation was in.
	 * @throws	NullPointerException if {@code previous} is {@code null}
	 * @throws 	IllegalArgumentException 
	 *        	if {@code previous} does not meet the requirements 
	 *        	to go to this state.
	 * 
	 * @see ComputationState#ComputationState(ComputationState)
	 */
	FinishedState(ComputationState previous) 
			throws IllegalArgumentException {
		super(previous);
		if(getSum() == null || getSum().signum() < 0)
			throw new IllegalArgumentException(
					"To finish, a positive sum is needed");
	}

	@Override
	Map<AntiChain, Long> computeEquivalenceClasses(ThirdOrderAlgorithm algo) {
		return getEquivalenceClasses();
	}

	@Override
	Map<AntiChain, Long> computeLeftIntervalSizes(ThirdOrderAlgorithm algo) {
		return getLeftIntervalSizes();
	}

	@Override
	Map<BigInteger, Long> computeMunuetaEquivalenceClasses(ThirdOrderAlgorithm algo) {
		return getMunuetaEquivalenceClasses();
	}

	@Override
	BigInteger computeSum(ThirdOrderAlgorithm algo) {
		return getSum();
	}
	
	private void writeObject(ObjectOutputStream out) 
			throws IOException {
		out.defaultWriteObject();
		out.writeObject(getSum());
	}
	
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setSum((BigInteger) in.readObject());
	}
	
	public String toString() {
		return "Finished" + super.toString();
	}

 }
