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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;
import amfsmall.AntiChainSolver;
import amfsmall.SmallBasicSet;
import mpi.MPI;
import mpi.MPIException;
import step3.MuNuEta;

/**
 * A class representing 1 node in a HPC environment 
 * that helps computing Dedekind numbers with the 
 * third order algorithm of prof. De Causmaecker.
 * 
 * <p>
 * This class has been developed to work with the openmpi-1.8.3 library for java 
 * from <a href="http://www.open-mpi.org"> open-mpi.org</a>.
 * To compile and/or run this code, open-mpi is needed. 
 * Compiling can be done by adding {@code -classpath <path to mpi.jar>}
 * as a flag to {@code javac} on the command line or by adding {@code mpi.jar}
 * as an external library to the IDE.
 * A mpi-program can normally be run with a command like
 * {@code mpirun <mpi-flags> java <java-flags> <application>}
 * in the command line.
 * </p>
 * Normal usage of this class generally resides in an MPI-context as follows:
 * <pre>
 * public static void main(String[] args) {
 *     MPI.Init(args);
 *     
 *     int myRank = MPI.COMM_WORLD.getRank();
 *     int nrOfNodes = MPI.COMM_WORLD.getSize();
 *     
 *     new ThirdOrderAlgorithm(myRank, nrOfNodes, n);
 *     
 *     MPI.Finalize();
 * }
 * </pre>
 * 
 * @author Pieter-Jan Hoedt
 *
 */
public class ThirdOrderAlgorithm {
	
	/** Rank of the root node. */
	public static final int ROOT_RANK = 0;
	/** Tag to indicate that the size of the real data is being sent/received. */
	public static final int NUM_TAG = 1;
	/** Tag to indicate real data is being sent/received. */
	public static final int DATA_TAG = 2;

	private volatile boolean interrupted = false;

	private int nMin3;
	private ComputationState currentState;

	/**
	 * Initialize this algorithm with a state it should start computing in.
	 * 
	 * This constructor also sets up a {@code shutdownHook} to make sure 
	 * that the state of the computations is stored before the JVM quits.
	 * 
	 * NOTE that the nodes will wait for each other to initialize the state.
	 * 
	 * @param 	state
	 *       	The {@code ComputationState} to start computing in
	 * @throws	IllegalStateException
	 *        	if the JVM started the shutdown sequence already
	 * 
	 * @see #setCurrentState(ComputationState)
	 * @see Runtime#addShutdownHook(Thread)
	 * @see #stopComputation()
	 */
	ThirdOrderAlgorithm(ComputationState state) throws IllegalStateException {
		setCurrentState(state);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stopComputation();
			}

		});
	}

	/**
	 * Initialise this algorithm with the necessary MPI information and 
	 * the Dedekind number that should be computed.
	 * 
	 * NOTE that the nodes will wait for each other to initialize the state.
	 * 
	 * @param 	rank
	 *       	The MPI-rank for this node
	 * @param 	nrOfNodes
	 *       	The number of nodes available for computation
	 * @param 	n
	 *       	The Dedekind number to compute
	 * @throws	IllegalArgumentException 
	 *        	if {@code n < 3}
	 *        
	 * @see #ThirdOrderAlgorithm(ComputationState)
	 */
	public ThirdOrderAlgorithm(int rank, int nrOfNodes, int n) 
			throws IllegalArgumentException {
		this(new InitState(rank, nrOfNodes, n));
	}

	/** @return the rank of this node */
	public int getRank() {
		return getCurrentState().getRank();
	}

	/** @return the number of nodes used for computation */
	public int getNumberOfNodes() {
		return getCurrentState().getNumberOfNodes();
	}

	/** @return the Dedekind number being computed by this node */
	public int getN() {
		return getCurrentState().getOrder();
	}

	/** 
	 * Set which Dedekind number to compute.
	 * Invocation of this method puts this algorithm back in the init-state.
	 * 
	 * @param 	n 
	 *       	The Dedekind number to compute.
	 * @throws	IllegalArgumentException if {@code n < 3}
	 *       
	 * @see #setCurrentState(ComputationState)
	 * @see InitState
	 */
	protected void setN(int n) throws IllegalArgumentException {
		setCurrentState(new InitState(getRank(), getNumberOfNodes(), n));
	}

	/**
	 * Check whether or not the interrupted-flag has been set. 
	 * 
	 * @return 	{@code true} if the interrupted-flag has been set, 
	 *        	{@code false} otherwise
	 *  
	 *  @see #stopComputation() 
	 */
	public boolean isInterrupted() {
		return this.interrupted;
	}

	/**
	 * Set the interrupted-flag and persist the current state 
	 * of computation by writing it to a file.
	 * 
	 * @see #isInterrupted()
	 * @see ComputationState#store()
	 */
	private void stopComputation() {
		interrupted = true;
		getCurrentState().store();
	}

	/** @return the current state of computation for this node */
	public ComputationState getCurrentState() {
		return this.currentState;
	}

	/** @param state The new state for this node */
	protected void setCurrentState(ComputationState state) {
		if(waitForOthers()) {
			this.currentState = state;
			this.nMin3 = state.getOrder() - 3;
		}
	}

	/** @return the equivalence classes computed thus far*/
	public Map<AntiChain, Long> getEquivalenceClasses() {
		return getCurrentState().getEquivalenceClasses() == null ?
				Collections.emptyMap() : getCurrentState().getEquivalenceClasses();
	}

	/** @return the left interval sizes computed thus far */
	public Map<AntiChain, Long> getLeftIntervalSizes() {
		return getCurrentState().getLeftIntervalSizes() == null ?
				Collections.emptyMap() : getCurrentState().getLeftIntervalSizes();
	}

	/** @return the (mu,nu,eta) equivalence classes computed thus far */
	public Map<BigInteger, Long> getMunuetaEquivalenceClasses() {
		return getCurrentState().getMunuetaEquivalenceClasses() == null ?
				Collections.emptyMap() : getCurrentState().getMunuetaEquivalenceClasses();
	}

	/** @return the sum computed this far */
	public BigInteger getSum() {
		return getCurrentState().getSum() == null ? 
				BigInteger.ZERO : getCurrentState().getSum();
	}
	
	/************************************************************
	 * computation methods										*
	 ************************************************************/

	/**
	 * Compute the Dedekind number.
	 * 
	 * @return	the nth Dedekind number {@code |A(getN())|}
	 */
	public BigInteger compute() {
		getCurrentState().computeEquivalenceClasses(this);
		getCurrentState().computeLeftIntervalSizes(this);
		getCurrentState().computeMunuetaEquivalenceClasses(this);
		getCurrentState().computeSum(this);
		return getSum();
	}

	/**
	 * Compute the equivalence classes of antichains in {@code |A(n-3)|}
	 * and update the computation state of this node.
	 * 
	 * @return	a {@code Map} mapping a representative antichain for
	 *        	each equivalence class to the number of antichains in
	 *        	that equivalence class
	 *        
	 * @see ComputationState#addEquivalenceClass(AntiChain, long)
	 */
	public Map<AntiChain, Long> computeEquivalenceClasses() {
		SortedMap<BigInteger, Long>[] temp = AntiChainSolver.equivalenceClasses(nMin3);

		for(int i = 0; i < temp.length; i++) {
			long factor = SmallBasicSet.combinations(nMin3, i);

			for(Entry<BigInteger, Long> entry : temp[i].entrySet()) {
				getCurrentState().addEquivalenceClass(
						AntiChain.decode(entry.getKey()), entry.getValue() * factor);
			}
		}

		return getEquivalenceClasses();
	}

	/**
	 * Compute the left interval sizes |[{}, {@code ac}]|
	 * for all representatives of an equivalence class
	 * and update the computation state of this node.
	 * 
	 * @return	a {@code Map} mapping each representative antichain
	 *        	to its left interval size.
	 *        
	 * @see #computeEquivalenceClasses()
	 */
	public Map<AntiChain, Long> computeLeftIntervalSizes() {
		for(AntiChain ac : getEquivalenceClasses().keySet()) {
			getCurrentState().addLeftIntervalSize(ac, 
					new AntiChainInterval(AntiChain.emptyFunction(), ac).latticeSize());
		}

		return getLeftIntervalSizes();
	}

	/**
	 * Compute the (mu,nu,eta) equivalence classes in {@code |A(n-3)|}
	 * and update the computation state of this node.
	 * <p>
	 * 
	 * After all nodes have finished local computation, the results are gathered 
	 * and merged by every single node.
	 * </p>
	 * 
	 * 
	 * @return	a {@code Map} mapping a representative (mu,nu,eta) for each 
	 *        	equivalence class to the number of of (mu,nu,eta)'s in that 
	 *        	equivalence class 
	 * 
	 * @see #computeEquivalenceClasses()
	 * @see #gatherAllMunuetaEquivalenceClasses()
	 */
	public Map<BigInteger, Long> computeMunuetaEquivalenceClasses() {
		Iterator<Entry<AntiChain, Long>> it = getEquivalenceClasses().entrySet().iterator();
		AntiChainInterval space = AntiChainInterval.fullSpace(nMin3);
		Set<int[]> g = AntiChain.universeFunction(nMin3).symmetryGroup();

		Entry<AntiChain, Long> entry;
		AntiChain mu;
		BigInteger code;
		TreeMap<BigInteger, Long> temp;

		while(it.hasNext() && !isInterrupted()) {
			entry = it.next();
			// static distribution of work
			if(getCurrentState().myTurn()) {
				mu = entry.getKey();
				temp = new TreeMap<>();
				for(AntiChain nu : space) {
					for(AntiChain eta : getEtaInterval(mu, nu)) {
						code = new MuNuEta(mu, nu, eta).standard(g).encode();
						temp.merge(code, entry.getValue(), (v1, v2) -> v1 + v2);
					}
				}
				
				if(!isInterrupted())
					getCurrentState().addAllToMunuetaEquivalenceClasses(temp);
			}

			it.remove();
		}

		if(waitForOthers()) {
			try {
				//TODO: possible without TreeMap (e.g. Hash values)?
				Map<BigInteger, Long> merged = gatherAllMunuetaEquivalenceClasses().stream()
						.map(Map::entrySet).flatMap(Set::stream)
						.collect(Collectors.toMap(
								Entry::getKey, 
								Entry::getValue, 
								(v1, v2) -> v1 + v2,
								TreeMap::new));
				
				if(waitForOthers())
					getCurrentState().setMunuetaEquivalenceClasses(merged);
			} catch (ClassCastException | MPIException e) {
				e.printStackTrace();
			}
		}

		return getMunuetaEquivalenceClasses();
	}

	/**
	 * Sum over all (mu,nu,eta)'s to compute a part of the Dedekind number
	 * and update the computation state of this node.
	 * This summation makes use of the (mu,nu,eta) equivalence classes
	 * and the left interval sizes to make computations more efficient.
	 * 
	 * <p>
	 * After all nodes have finished local computation, the results are gathered 
	 * and merged by the node with {@code getRank() == ROOT_RANK}.
	 * </p>
	 * 
	 * @return a {@code BigInteger} representing the nth Dedekind number {@code |A(getN())|}
	 * 
	 * @see #computeLeftIntervalSizes()
	 * @see #computeMunuetaEquivalenceClasses()
	 * @see #gatherSum()
	 */
	public BigInteger computeSum() {
		Iterator<Entry<BigInteger, Long>> it = getMunuetaEquivalenceClasses().entrySet().iterator();

		Entry<BigInteger, Long> entry;
		MuNuEta mne;
		long factor;
		long temp;

		while(it.hasNext() && !isInterrupted()) {
			entry = it.next();
			// static distribution of work
			if(getCurrentState().myTurn()) {
				mne = MuNuEta.decode(entry.getKey());
				factor = mne.rhoIntegral(nMin3) * entry.getValue();
				
				temp = 0;
				for(AntiChain abc : new AntiChainInterval(mne.getBottom(), mne.getMaxBottom()))
					temp += mne.p3(abc) * getLeftIntervalSizes().get(abc.standard()) * factor;
				
				if(!isInterrupted())
					getCurrentState().addToSum(BigInteger.valueOf(temp));
			}

			it.remove();
		}

		if(waitForOthers()) {
			try {
				BigInteger summed = gatherSum().stream()
						.reduce(BigInteger.ZERO, BigInteger::add);
				
				if(waitForOthers())
					getCurrentState().setSum(summed);
			} catch (MPIException e) {
				e.printStackTrace();
			}
		}

		return getSum();
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

	/**
	 * Compute the interval in which eta must be when mu and nu are given.
	 * 
	 * @param 	mu
	 *       	The mu from (mu,nu,eta)
	 * @param 	nu
	 *       	The nu from (mu,nu,eta)
	 * @return	an interval of antichains containing all possibilities for eta
	 */
	private AntiChainInterval getEtaInterval(AntiChain mu, AntiChain nu) {
		AntiChain overlap = new AntiChain(mu);
		overlap.retainAll(nu);

		AntiChain etamu = new AntiChain(mu);
		etamu.removeAll(overlap);
		AntiChain etanu = new AntiChain(nu);
		etanu.removeAll(overlap);

		return new AntiChainInterval(etamu.join(etanu), mu.join(nu));
	}

	/**
	 * Gather the (mu,nu,eta) equivalence classes from all nodes in the mpi-configuration.
	 * 
	 * @return	a {@code Collection} containing the (mu,nu,eta) equivalence classes
	 *        	from every node in the mpi configuration, this node included:
	 *        	{@code gatherAllMunuetaEquivalenceClasses().size() == getNumberOfNodes()}
	 * @throws 	MPIException 
	 *        	if something goes wrong with intercommunication between the nodes
	 */
	private Collection<Map<BigInteger, Long>> gatherAllMunuetaEquivalenceClasses() 
			throws MPIException {
		Collection<Map<BigInteger, Long>> result = new ArrayList<>(getNumberOfNodes());
		byte[] sendbuf = serialize((Serializable) getMunuetaEquivalenceClasses());
		int[] count;
		byte[] buf;
		
		for(int i = 0; i < getNumberOfNodes(); i++) {
			count = getRank() == i ? new int[]{sendbuf.length} : new int[1];
			MPI.COMM_WORLD.bcast(count, count.length, MPI.INT, i);
			buf = getRank() == i ? buf = sendbuf : new byte[count[0]];
			MPI.COMM_WORLD.bcast(buf, buf.length, MPI.BYTE, i);
			result.add((Map<BigInteger, Long>) deserialize(buf));
		}
		
		return result;
	}

	/**
	 * Gather the sums computed from every node in the root-node.
	 * 
	 * @return	a {@code Collection} containing all the partial sums 
	 *        	computed by every node in the mpi-configuration if 
	 *        	{@code getRank() == ROOT_RANK}, an empty collection otherwise
	 * @throws 	MPIException
	 *        	if something goes wrong with intercommunication between the nodes
	 */
	private Collection<BigInteger> gatherSum() throws MPIException {
		Collection<BigInteger> result = new ArrayList<>(getNumberOfNodes());
		byte[] sendbuf = getSum().toByteArray();
		int[] recvcounts = new int[getNumberOfNodes()];
		
		MPI.COMM_WORLD.gather(new int[]{sendbuf.length}, 1, MPI.INT, 
				recvcounts, 1, MPI.INT, ROOT_RANK);
		
		int[] displs = new int[recvcounts.length];
		displs[0] = 0;
		for(int i = 1; i < recvcounts.length; i++)
			displs[i] = displs[i - 1] + recvcounts[i - 1];
		
		byte[] recvbuf = new byte[displs[displs.length-1] + recvcounts[recvcounts.length -1]];
		MPI.COMM_WORLD.gatherv(sendbuf, sendbuf.length, MPI.BYTE, 
				recvbuf, recvcounts, displs, MPI.BYTE, ROOT_RANK);
		
		if(getRank() == ROOT_RANK) {
			for(int i = 1; i < displs.length; i++)
				result.add(new BigInteger(
						Arrays.copyOfRange(recvbuf, displs[i - 1], displs[i])));
			
			result.add(new BigInteger(
					Arrays.copyOfRange(recvbuf, displs[displs.length - 1], recvbuf.length)));
		}
			
		return result;
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

	/**
	 * Compute and print a Dedekind number.
	 * This main method allows to recover from saved states 
	 * or computations from scratch.
	 * 
	 * @param 	args
	 *       	Flags to indicate which file to read from, 
	 *       	the {@code -h} flag prints the help section.
	 * @throws 	MPIException
	 *        	If something goes wrong while initializing 
	 *        	or finalizing the mpi environment
	 */
	public static void main(String[] args) throws MPIException {
		int n = 3;
		boolean recover = false;
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			switch (a) {
				case "-n" : n = Integer.valueOf(args[i+1]); i++; break;
				case "-r" : recover = Boolean.valueOf(args[i+1]);i++; break;
				case "-h" : System.out.println("parameters -n -r -h\n"
						+ "-n number : compute |A(number)| (n > 2!)\n"
						+ "-r true/false : recover from state/don't recover\n"
						+ "-h : this text");return;
				default : System.out.println("Ignored " + a);break;
			}
		}

		MPI.Init(args);

		int myRank = MPI.COMM_WORLD.getRank();
		int nrOfNodes = MPI.COMM_WORLD.getSize();

		ThirdOrderAlgorithm node;
		if(recover) {
			ComputationState state = ComputationState.recoverState(n, myRank, nrOfNodes);
			node = new ThirdOrderAlgorithm(state);
		} else {
			node = new ThirdOrderAlgorithm(myRank, nrOfNodes, n);
		}
		BigInteger result = node.compute();	
		
		System.out.println("result for " + node.getRank() + ": " + result);

		MPI.Finalize();
	}

}
