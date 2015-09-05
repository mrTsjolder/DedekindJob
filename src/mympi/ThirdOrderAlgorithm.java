package mympi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;
import amfsmall.AntiChainSolver;
import amfsmall.SmallBasicSet;
import mpi.MPI;
import mpi.MPIException;
import step3.MuNuEta;
import step3.MuNuEta.MNECode;

/**
 * 
 * @author Pieter-Jan Hoedt
 *
 */
public class ThirdOrderAlgorithm {
	
	public static final int ROOT_RANK = 0;
	public static final int NUM_TAG = 1;
	public static final int DATA_TAG = 2;

	private volatile boolean interrupted = false;

	private int nMin3;
	private ComputationState currentState;

	ThirdOrderAlgorithm(ComputationState state) {
		setState(state);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stopComputation();
			}

		});
	}

	public ThirdOrderAlgorithm(int rank, int nrOfNodes, int n) {
		this(new InitState(rank, nrOfNodes, n));
	}

	public int getRank() {
		return getState().getRank();
	}

	public int getNumberOfNodes() {
		return getState().getNumberOfNodes();
	}

	public int getN() {
		return getState().getOrder();
	}

	protected void setN(int n) {
		setState(new InitState(getRank(), getNumberOfNodes(), n));
	}

	public boolean isInterrupted() {
		return this.interrupted;
	}

	private void stopComputation() {
		interrupted = true;
		getState().store();
	}

	public ComputationState getState() {
		return this.currentState;
	}

	protected void setState(ComputationState state) {
		if(waitForOthers()) {
			this.currentState = state;
			this.nMin3 = state.getOrder() - 3;
		}
	}
	
	/************************************************************
	 * computation methods										*
	 ************************************************************/

	public BigInteger compute() {
		getState().computeEquivalenceClasses(this);
		getState().computeLeftIntervalSizes(this);
		getState().computeMunuetaEquivalenceClasses(this);
		getState().computeSum(this);
		return getState().getSum();
	}

	public Map<AntiChain, Long> computeEquivalenceClasses() {
		SortedMap<BigInteger, Long>[] temp = AntiChainSolver.equivalenceClasses(nMin3);

		for(int i = 0; i < temp.length; i++) {
			long factor = SmallBasicSet.combinations(nMin3, i);

			for(Entry<BigInteger, Long> entry : temp[i].entrySet()) {
				getState().addEquivalenceClass(AntiChain.decode(entry.getKey()), entry.getValue() * factor);
			}
		}

		return getState().getEquivalenceClasses();
	}

	public Map<AntiChain, Long> computeLeftIntervalSizes(Set<AntiChain> representatives) {
		for(AntiChain ac : representatives) {
			getState().addLeftIntervalSize(ac, new AntiChainInterval(AntiChain.emptyFunction(), ac).latticeSize());
		}

		return getState().getLeftIntervalSizes();
	}

	public Map<MNECode, Long> computeMunuetaEquivalenceClasses(Map<AntiChain, Long> equivalenceClasses) {
		Iterator<Entry<AntiChain, Long>> it = equivalenceClasses.entrySet().iterator();
		AntiChainInterval space = AntiChainInterval.fullSpace(nMin3);
		Set<int[]> g = AntiChain.universeFunction(nMin3).symmetryGroup();

		Entry<AntiChain, Long> entry;
		AntiChain mu;
		MNECode code;

		while(it.hasNext() && !isInterrupted()) {
			entry = it.next();
			// static distribution of work
			if(getState().myTurn()) {
				mu = entry.getKey();

				for(AntiChain nu : space) {
					for(AntiChain eta : getEtaInterval(mu, nu)) {
						code = new MuNuEta(mu, nu, eta).standard(g).encode();
						getState().addToMunuetaEquivalenceClass(code, entry.getValue());
					}
				}
			}

			it.remove();
		}

		if(waitForOthers()) {
			try {
				Map<MNECode, Long> merged = gatherAllMunuetaEquivalenceClasses().stream()
						.map(Map::entrySet).flatMap(Set::stream)
						.collect(Collectors.toMap(
								Entry::getKey, 
								Entry::getValue, 
								(v1, v2) -> v1 + v2));
				
				if(waitForOthers())
					getState().setMunuetaEquivalenceClasses(merged);
			} catch (ClassCastException | MPIException e) {
				e.printStackTrace();
			}
		}

		return getState().getMunuetaEquivalenceClasses();
	}

	public BigInteger computeSum(Map<MNECode, Long> mneEqClasses, Map<AntiChain, Long> intervalSizes) {
		Iterator<Entry<MNECode, Long>> it = mneEqClasses.entrySet().iterator();

		Entry<MNECode, Long> entry;
		MuNuEta mne;
		long factor;

		while(it.hasNext() && !isInterrupted()) {
			entry = it.next();
			// static distribution of work
			if(getState().myTurn()) {
				mne = MuNuEta.decode(entry.getKey());
				factor = mne.rhoIntegral(nMin3) * entry.getValue();

				for(AntiChain abc : new AntiChainInterval(mne.getBottom(), mne.getMaxBottom()))
					getState().addToSum(BigInteger.valueOf(mne.p3(abc) * intervalSizes.get(abc) * factor));

			}

			it.remove();
		}

		if(waitForOthers()) {
			try {
				gatherSum();
			} catch (MPIException e) {
				e.printStackTrace();
			}
		}

		return getState().getSum();
	}
	
	/************************************************************
	 * utility methods		     								*
	 ************************************************************/

	/**
	 * Wait for the other nodes to reach the same point in code.
	 *
	 * @return	{@code true} if all nodes reached the barrier,
	 *        	{@code false} if execution was interrupted.
	 *
	 * @see mpi.Intracomm#barrier()
	 * @see #isInterrupted()
	 */
	private boolean waitForOthers() {
		try {
			MPI.COMM_WORLD.barrier();
		} catch (MPIException e) {
			e.printStackTrace();
		}
		return !isInterrupted();
	}

	private AntiChainInterval getEtaInterval(AntiChain mu, AntiChain nu) {
		AntiChain overlap = new AntiChain(mu);
		overlap.retainAll(nu);

		AntiChain etamu = new AntiChain(mu);
		etamu.removeAll(overlap);
		AntiChain etanu = new AntiChain(nu);
		etanu.removeAll(overlap);

		return new AntiChainInterval(etamu.join(etanu), mu.join(nu));
	}

	private Collection<Map<MNECode, Long>> gatherAllMunuetaEquivalenceClasses() 
			throws ClassCastException, MPIException {
		Collection<Map<MNECode, Long>> result = new ArrayList<>(getNumberOfNodes());
		byte[] sendbuf = serialize((Serializable) getState().getMunuetaEquivalenceClasses());
		int[] recvcounts = new int[getNumberOfNodes()];
		
		MPI.COMM_WORLD.allGather(new int[]{sendbuf.length}, 1, MPI.INT, recvcounts, 1, MPI.INT);
		
		int[] displs = new int[recvcounts.length];
		displs[0] = 0;
		for(int i = 1; i < recvcounts.length; i++)
			displs[i] = displs[i-1] + recvcounts[i - 1];
		byte[] recvbuf = new byte[displs[displs.length -1] + recvcounts[displs.length - 1]];
		
		MPI.COMM_WORLD.allGatherv(sendbuf, sendbuf.length, MPI.BYTE, recvbuf, recvcounts, displs, MPI.BYTE);
		
		for(int i = 1; i < displs.length; i++)
			result.add((Map<MNECode, Long>) deserialize(Arrays.copyOfRange(recvbuf, displs[i-1], displs[i])));
		result.add((Map<MNECode, Long>) deserialize(Arrays.copyOfRange(recvbuf, displs[displs.length - 1], recvbuf.length)));
		
		return result;
	}

	private Collection<BigInteger> gatherSum() throws MPIException {
		Collection<BigInteger> collected = new ArrayList<>(getNumberOfNodes());
		byte[] sendbuf = getState().getSum().toByteArray();
		int[] recvcounts = new int[getNumberOfNodes()];
		MPI.COMM_WORLD.gather(new int[]{sendbuf.length}, 1, MPI.INT, recvcounts, 1, MPI.INT, ROOT_RANK);
		int[] displs = new int[recvcounts.length];
		displs[0] = 0;
		for(int i = 1; i < recvcounts.length; i++)
			displs[i] = displs[i - 1] + recvcounts[i - 1];
		byte[] recvbuf = new byte[displs[displs.length-1] + recvcounts[displs.length -1]];
		MPI.COMM_WORLD.gatherv(sendbuf, sendbuf.length, MPI.BYTE, recvbuf, recvcounts, displs, MPI.BYTE, ROOT_RANK);
		
		for(int i = 1; i < displs.length; i++)
			collected.add(new BigInteger(Arrays.copyOfRange(recvbuf, displs[i - 1], displs[i])));
		collected.add(new BigInteger(Arrays.copyOfRange(recvbuf, displs[displs.length - 1], recvbuf.length)));
		
		return collected;
	}
	
	/************************************************************
	 * Serializing-utils										*
	 ************************************************************/

	/**
	 * Serialize an object in order to send it over MPI.
	 * 
	 * @param 	object
	 * 			The object to be serialized.
	 * @return	a byte array representing the serialized object.
	 * 			This array is empty if something went wrong.
	 * 
	 * @see #deserialize(byte[])
	 */
	private byte[] serialize(Serializable object) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    
		try(ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
		} catch (IOException ioe) {
		   	ioe.printStackTrace();   
		}
		
	    return baos.toByteArray();
	}
	
	/**
	 * Deserialize a byte array received through MPI to an object.
	 * 
	 * @param	buf
	 * 			The byte array to be deserialized.
	 * @return	The object that was represented by this byte array.
	 * 			The object will be null if something went wrong.
	 * 
	 * @see #serialize(Serializable)
	 */
	private Object deserialize(byte[] buf) {
		ByteArrayInputStream bis = new ByteArrayInputStream(buf);
		Object result = null;
		
		try(ObjectInputStream ois = new ObjectInputStream(bis)) {
			result =  ois.readObject();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} 
		
		return result;
	}
	
	/************************************************************
	 * main method             									*
	 ************************************************************/

	public static void main(String[] args) throws MPIException {
		int n = 3;

		MPI.Init(args);

		int myRank = MPI.COMM_WORLD.getRank();
		int nrOfNodes = MPI.COMM_WORLD.getSize();

		ThirdOrderAlgorithm node = new ThirdOrderAlgorithm(myRank, nrOfNodes, n);
		BigInteger result = node.compute();	
		
		System.out.println("result for " + node.getRank() + ": " + result);

		MPI.Finalize();
	}

}
