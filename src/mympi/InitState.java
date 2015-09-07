package mympi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Map;

import step3.MuNuEta.MNECode;
import amfsmall.AntiChain;

/**
 * 
 * @author Pieter-Jan Hoedt
 *
 */
class InitState extends ComputationState {
	
	/** Identification number needed for serialisation. */
	private static final long serialVersionUID = -7296332744165641916L;

	InitState(int rank, int nrOfNodes, int order) 
			throws IllegalArgumentException {
		super(rank, nrOfNodes, order);
	}
	 
	@Override
	Map<AntiChain, Long> computeEquivalenceClasses(ThirdOrderAlgorithm algo) {
		long start = System.currentTimeMillis();
		algo.computeEquivalenceClasses();
		addTiming("equivalence classes", System.currentTimeMillis() - start);
		return getEquivalenceClasses();
	}

	@Override
	Map<AntiChain, Long> computeLeftIntervalSizes(ThirdOrderAlgorithm algo) 
			throws IllegalStateException {
		if(getEquivalenceClasses() == null || getEquivalenceClasses().isEmpty())
			throw new IllegalStateException(
					"equivalence classes should be computed first");
		long start = System.currentTimeMillis();
		algo.computeLeftIntervalSizes();
		addTiming("left interval sizes", System.currentTimeMillis() - start);
		algo.setCurrentState(new PreparationState(this));
		return getLeftIntervalSizes();
	}

	@Override
	Map<MNECode, Long> computeMunuetaEquivalenceClasses(ThirdOrderAlgorithm algo) 
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Not useful in this state");
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
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setEquivalenceClasses((Map<AntiChain, Long>) in.readObject());
		setLeftIntervalSizes((Map<AntiChain, Long>) in.readObject());
	}
	
	@Override
	public String toString() {
		return "Init" + super.toString();
	}

}
