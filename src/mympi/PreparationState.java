package mympi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Map;

import amfsmall.AntiChain;
import step3.MuNuEta.MNECode;

class PreparationState extends ComputationState {

	/** Identification number needed for serialisation. */
	private static final long serialVersionUID = -7613396742248456671L;

	PreparationState(ComputationState previous) 
			throws IllegalArgumentException {
		super(previous);
		if(getEquivalenceClasses() == null || getEquivalenceClasses().isEmpty())
			throw new IllegalArgumentException(
					"To prepare, equivalence classes are needed");
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
	Map<MNECode, Long> computeMunuetaEquivalenceClasses(ThirdOrderAlgorithm algo) {
		long start = System.currentTimeMillis();
		algo.computeMunuetaEquivalenceClasses();
		addTiming("munueta equivalence classes", System.currentTimeMillis() - start);

		// the only way the state transition should not take place
		// is when the program has not been interrupted...
		if(!algo.isInterrupted())
			algo.setCurrentState(new SumState(this));
		
		return getMunuetaEquivalenceClasses();
	}

	@Override
	BigInteger computeSum(ThirdOrderAlgorithm algo) 
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Not useful in this state");
	}
	
	private void writeObject(ObjectOutputStream out) 
			throws IOException {
		out.defaultWriteObject();
		out.writeObject(getEquivalenceClasses());
		out.writeObject(getLeftIntervalSizes());
		out.writeObject(getMunuetaEquivalenceClasses());
		out.writeInt(getCounter());
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setEquivalenceClasses((Map<AntiChain, Long>) in.readObject());
		setLeftIntervalSizes((Map<AntiChain, Long>) in.readObject());
		setMunuetaEquivalenceClasses((Map<MNECode, Long>) in.readObject());
		// turn back counter with one, because the last iteration failed
		setCounter(in.readInt() - 1);
	}
	
	@Override
	public String toString() {
		return "Preparation" + super.toString();
	}

}
