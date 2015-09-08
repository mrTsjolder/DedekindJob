package mympi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Map;

import amfsmall.AntiChain;

class SumState extends ComputationState {

	/** Identification number needed for serialisation. */
	private static final long serialVersionUID = 6895644951054876307L;

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
	SumState(ComputationState previous) 
			throws NullPointerException, IllegalArgumentException {
		super(previous);
		if(getMunuetaEquivalenceClasses() == null || getMunuetaEquivalenceClasses().isEmpty())
			throw new IllegalArgumentException(
					"To sum, munueta equivalence classes are needed");
		if(getLeftIntervalSizes() == null || getLeftIntervalSizes().isEmpty())
			throw new IllegalArgumentException(
					"To sum, bottom interval sizes are needed");
	}

	@Override
	Map<AntiChain, Long> computeEquivalenceClasses(ThirdOrderAlgorithm algo) {
		return getEquivalenceClasses();
	}

	@Override
	Map<BigInteger, Long> computeMunuetaEquivalenceClasses(ThirdOrderAlgorithm algo) {
		return getMunuetaEquivalenceClasses();
	}

	@Override
	Map<AntiChain, Long> computeLeftIntervalSizes(ThirdOrderAlgorithm algo) {
		return getLeftIntervalSizes();
	}
	
	@Override
	BigInteger computeSum(ThirdOrderAlgorithm algo) {
		long start = System.currentTimeMillis();
		algo.computeSum();
		addTiming("sum", System.currentTimeMillis() - start);
		
		// the only way the state transition should not take place
		// is when the program has not been interrupted...
		if(!algo.isInterrupted())
			algo.setCurrentState(new FinishedState(this));
		
		return getSum();
	}
	
	private void writeObject(ObjectOutputStream out) 
			throws IOException {
		out.defaultWriteObject();
		out.writeObject(getLeftIntervalSizes());
		out.writeObject(getMunuetaEquivalenceClasses());
		out.writeObject(getSum());
		out.writeInt(getCounter());
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setLeftIntervalSizes((Map<AntiChain, Long>) in.readObject());
		setMunuetaEquivalenceClasses((Map<BigInteger, Long>) in.readObject());
		setSum((BigInteger) in.readObject());
		// turn back counter with one, because the last iteration failed
		setCounter(in.readInt() - 1);
	}
	
	@Override
	public String toString() {
		return "Sum" + super.toString();
	}

}
