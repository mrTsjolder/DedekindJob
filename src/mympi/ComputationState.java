package mympi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import amfsmall.AntiChain;

/**
 * An abstract class representing the state of computation for the algorithm
 * that is implemented by {@link ThirdOrderMPI}.
 * <p> 
 * This class provides methods to store the state of computation 
 * and persist that state. If the persistence-methods of this class
 * are used to store the state, it is assured that all files storing a state
 *  will reside in the same directory and can be read by the {@code main} method.
 * </p>
 * <p>
 * This class also provides a {@link #main(String[])} method that enables to read {@code State} objects 
 * that have been stored into files and write them to the standard output.
 * This way all stored information can be accessed at any time, without need 
 * to worry about the details of where and how the files are stored.
 * </p>
 * <p>
 * NOTE that this class in its current version cannot assure that the state is saved
 * when the JVM crashes due to native errors or power loss.
 * </p>
 * 
 * @author Pieter-Jan Hoedt
 *
 */
abstract class ComputationState implements Serializable {

	/** Identification number needed for serialisation. */
	private static final long serialVersionUID = 4547438662143346534L;
	/** string to be formatted with 3 integers 
	 * to form a filename for files storing a state object */
	private static final String FILENAME_FORMAT = "Dedekind%d-State%dof%d.ser";
	
	private final int rank;
	private final int numberOfNodes;
	private final int order;
	
	private Map<String, Long> timings = new TreeMap<>();
	private transient int counter = 0;
	private transient Map<AntiChain, Long> equivalenceClasses = null;
	private transient Map<AntiChain, Long> leftIntervalSizes = null;
	private transient Map<BigInteger, Long> munuetaEquivalenceClasses = null;
	private transient BigInteger sum = null;
	
	/**
	 * Initialize this state object with the order of 
	 * the Dedekind number being computed and an identifier.
	 * 
	 * @param 	rank
	 *       	The rank of the node to which this state belongs
	 * @param 	nrOfNodes
	 * 			The number of nodes that are computing
	 * @param 	order
	 *       	The order of the Dedekind number being computed
	 * 
	 * @throws 	IllegalArgumentException if {@code order < 3}
	 */
	protected ComputationState(int rank, int nrOfNodes, int order) 
			throws IllegalArgumentException {
		if(order < 3)
			throw new IllegalArgumentException("for n < 3, there is no challenge");
		this.rank = rank;
		this.numberOfNodes = nrOfNodes;
		this.order = order;
	}
	
	/**
	 * Initialize this state object by copying all fields but the counter 
	 * from a previous state.
	 * 
	 * @param 	previous 
	 *       	The previous state the computation was in.
	 * @throws	NullPointerException if {@code previous} is {@code null}
	 * 
	 * @see   	#ComputationState(int, int, int)
	 */
	protected ComputationState(ComputationState previous) 
			throws NullPointerException, IllegalArgumentException {
		this(previous.getRank(), previous.getNumberOfNodes(), previous.getOrder());
		setEquivalenceClasses(previous.getEquivalenceClasses());
		setLeftIntervalSizes(previous.getLeftIntervalSizes());
		setMunuetaEquivalenceClasses(previous.getMunuetaEquivalenceClasses());
		setSum(previous.getSum());
		this.timings = previous.timings;
	}
	
	/******************************************************************
	 * getters, setters, initialisers                                 *
	 ******************************************************************/

	/** @return the rank of the node to which this state belongs */
	public int getRank() {
		return this.rank;
	}

	/** @return the number of nodes that are working on this computation */
	public int getNumberOfNodes() {
		return this.numberOfNodes;
	}

	/** @return the order of the Dedekind number being computed */
	public int getOrder() {
		return this.order;
	}

	/** @return the equivalence classes computed thus far, might be {@code null} */
	protected Map<AntiChain, Long> getEquivalenceClasses() {
		return equivalenceClasses;
	}

	/** @param equivalenceClasses the equivalence classes to set */
	protected void setEquivalenceClasses(Map<AntiChain, Long> equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}
	
	/** Initialise the equivalence classes with an empty map. */
	protected void initEquivalenceClasses() {
		setEquivalenceClasses(new TreeMap<>());
	}

	/** @return the left interval sizes computed thus far, might be {@code null} */
	protected Map<AntiChain, Long> getLeftIntervalSizes() {
		return leftIntervalSizes;
	}

	/**  @param leftIntervalSizes the left interval sizes to set */
	protected void setLeftIntervalSizes(Map<AntiChain, Long> leftIntervalSizes) {
		this.leftIntervalSizes = leftIntervalSizes;
	}
	
	/** Initialise the left interval sizes with an empty map. */
	protected void initLeftIntervalSizes() {
		setLeftIntervalSizes(new TreeMap<>());
	}

	/** @return the (mu,nu,eta) equivalence classes computed thus far, might be {@code null} */
	protected Map<BigInteger, Long> getMunuetaEquivalenceClasses() {
		return munuetaEquivalenceClasses;
	}

	/** @param munuetaEquivalenceClasses the (mu,nu,eta) equivalence classes to set */
	protected void setMunuetaEquivalenceClasses(
			Map<BigInteger, Long> munuetaEquivalenceClasses) {
		this.munuetaEquivalenceClasses = munuetaEquivalenceClasses;
	}
	
	/** Initialise the (mu,nu,eta) equivalence classes with an empty map. */
	protected void initMunuetaEquivalenceClasses() {
		setMunuetaEquivalenceClasses(new TreeMap<>());
	}

	/** @return the sum computed this far, might be {@code null} */
	public BigInteger getSum() {
		return sum;
	}

	/** @param sum the sum to set 
	 *  @throws IllegalArgumentException if {@code sum} is negative and not {@code null} */
	protected void setSum(BigInteger sum) throws IllegalArgumentException {
		if(sum != null && sum.signum() < 0)
			throw new IllegalArgumentException("The sum should never be negative");
		this.sum = sum;
	}
	
	/** Initialise the sum with {@link BigInteger#ZERO}. */
	protected void initSum() {
		setSum(BigInteger.ZERO);
	}
	
	/******************************************************************
	 * counter                                                        *
	 ******************************************************************/
	
	/** @return the counter */
	public int getCounter() {
		return counter;
	}

	/** @param counter the counter to set */
	protected void setCounter(int counter) {
		this.counter = counter;
	}
	
	/** Set the value for the counter back to {@code 0} */
	protected void resetCounter() {
		this.counter = 0;
	}
	
	/**
	 * Increment the counter with 1 so that it does not exceed a certain 
	 * upper limit. If the upper limit is reached, the counter will be reset.
	 * 
	 * @param 	upperLimit
	 *       	The upper limit for the counter
	 *       
	 * @see #resetCounter()
	 */
	protected void incrementCounter() {
		if(++counter == getNumberOfNodes())
			resetCounter();
	}
	
	/**
	 * Increment the counter and test whether the current value 
	 * of the counter equals the rank of this process.
	 * 
	 * @return	{@code true} if the rank of this process and the counter are 
	 *        	equal after incrementing, {@code false} otherwise
	 */
	protected boolean myTurn() {
		incrementCounter();
		return getCounter() == getRank();
	}
	
	/******************************************************************
	 * update state                                                   *
	 ******************************************************************/
	
	/**
	 * Add a representation of an equivalence class 
	 * to the map storing the equivalence classes.
	 * 
	 * @param 	ac 	
	 *       	The representative antichain
	 *       	for the equivalence class
	 * @param 	nr 
	 *       	The number of elements in the equivalence class 
	 *       	represented by {@code ac}
	 * @throws 	NullPointerException if {@code ac} is {@code null}
	 * 
	 * @since Java 8
	 */
	protected void addEquivalenceClass(AntiChain ac, long nr) 
			throws NullPointerException {
		if(getEquivalenceClasses() == null)
			initEquivalenceClasses();
		//an equivalence class should not be added twice
		equivalenceClasses.putIfAbsent(ac, nr);
	}

	/**
	 * Add an antichain and the size of its left interval
	 * to the map storing the left interval sizes.
	 * 
	 * @param 	ac
	 *       	The antichain
	 * @param 	size
	 *       	The size of the interval {@code [AntiChain.emptyFunction(), ac]}
	 * @throws 	NullPointerException if {@code ac} is {@code null}
	 * 
	 * @since Java 8
	 */
	protected void addLeftIntervalSize(AntiChain ac, long size) 
			throws NullPointerException {
		if(getLeftIntervalSizes() == null)
			initLeftIntervalSizes();
		//a left interval size should not be added twice
		leftIntervalSizes.putIfAbsent(ac, size);
	}

	/**
	 * Add a representation of a (mu,nu,eta) equivalence class
	 * to the map storing the (mu,nu,eta) equivalence classes
	 * if this class is not yet in the map.
	 * If it is already in the map, the sum will be taken of the values.
	 * 
	 * @param 	code
	 *       	The representative code for 
	 *       	the (mu,nu,eta) equivalence class
	 * @param 	nr
	 *       	The number of elements in the equivalence class 
	 *       	represented by {@code code}
	 * @throws 	NullPointerException if {@code code} is {@code null}
	 * 
	 * @since Java 8
	 */
	protected void addToMunuetaEquivalenceClass(BigInteger code, long nr) 
			throws NullPointerException {
		if(getMunuetaEquivalenceClasses() == null)
			initMunuetaEquivalenceClasses();
		
		munuetaEquivalenceClasses.merge(code, nr, (v1, v2) -> v1 + v2);
	}
	
	/**
	 * Add all equivalence classes from a given map to the (mu,nu,eta) 
	 * equivalence classes computed thus far. This method has the same result as: 
	 * <pre>
	 * {@code for(Entry<BigInteger, Long> entry : toBeAdded.entrySet())
	 *     addToMunuetaEquivalenceClass(entry.getKey(), entry.getValue());}
	 * </pre>
	 * 
	 * @param	toBeAdded
	 *       	The {@code Map} containing the equivalence classes and their size to be added.
	 * 
	 * @since Java 8
	 * @see #addToMunuetaEquivalenceClass(BigInteger, long)
	 */
	protected void addAllToMunuetaEquivalenceClasses(Map<BigInteger, Long> toBeAdded) 
			throws NullPointerException {
		if(getMunuetaEquivalenceClasses() == null)
			initMunuetaEquivalenceClasses();
		
		setMunuetaEquivalenceClasses(Stream.of(getMunuetaEquivalenceClasses(), toBeAdded)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Entry::getKey, 
						Entry::getValue,
						(v1, v2) -> v1 + v2,
						TreeMap::new)));
	}
	
	/**
	 * Add a {@code BigInteger} to the sum calculated thus far.
	 * 
	 * @param 	nr
	 *       	The number to add to the sum
	 * @throws 	NullPointerException if {@code nr} is {@code null}
	 * @throws 	IllegalArgumentException if {@code nr} is negative
	 */
	protected void addToSum(BigInteger nr) 
			throws NullPointerException, IllegalArgumentException {
		if(nr.signum() < 0)
			throw new IllegalArgumentException("Don't be so negative");
		if(getSum() == null)
			initSum();
		
		setSum(getSum().add(nr));
	}
	
	/**
	 * Add timing results with a certain context to the list of 
	 * timings gathered throughout the algorithm.
	 * If the context already had a timing, the new time 
	 * will be added to what has been stored already.
	 * 
	 * @param 	message
	 *       	A {@code String} to describe the context of what was timed
	 * @param 	time
	 *       	The time that has been measured in milliseconds
	 */
	protected void addTiming(String message, long time) {
		timings.merge(message, time, (v1, v2) -> v1 + v2);
	}
	
	/******************************************************************
	 * algorithm methods                                              *
	 ******************************************************************/

	abstract Map<AntiChain, Long> computeEquivalenceClasses(ThirdOrderAlgorithm algo);

	abstract Map<AntiChain, Long> computeLeftIntervalSizes(ThirdOrderAlgorithm algo);

	abstract Map<BigInteger, Long> computeMunuetaEquivalenceClasses(ThirdOrderAlgorithm algo);

	abstract BigInteger computeSum(ThirdOrderAlgorithm algo);
	
	/******************************************************************
	 * persisting methods                                             *
	 ******************************************************************/
	
	/**
	 * Write this {@code ComputationState} to the correct file 
	 * so that the state can later be recovered again.
	 * <p>
	 * The path to the file can be retrieved by {@link #getFilePath(int, int)}
	 * and with {@link #getDirectoryPath()} the path to the directory containing
	 * all (correctly) stored states can be retrieved.
	 * </p>
	 * 
	 * @see #recoverState(int, int)
	 * @see #recoverStates(int, int, int)
	 * @see #recoverAllStates(int)
	 */
	void store() {
		//create directory if it does not exist yet
		try {
			Files.createDirectories(getPath().getParent());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//write state away to file
		try(OutputStream os = Files.newOutputStream(getPath());
			ObjectOutputStream oos = new ObjectOutputStream(os)) {
			oos.writeObject(this);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the path to the file where this state is stored.
	 * 
	 * @return	a {@code Path} to the file
	 * 
	 * @see #getFilePath(int, int)
	 */
	private Path getPath() {
		return ComputationState.getFilePath(getOrder(), getRank(), getNumberOfNodes());
	}
	
	/******************************************************************
	 * static persistence-related methods                             *
	 ******************************************************************/
	
	/**
	 * Get the path to the directory where the files with the 
	 * the state-objects should be stored.
	 * 
	 * @return the {@code Path} to the directory
	 * 
	 * @see #getFilePath(int, int, int)
	 */
	public static Path getDirectoryPath() {
		String directory = "states";
		return Paths.get(directory);
	}
	
	/**
	 * Get the path to the file where the state for the computation of
	 * a given Dedekind number with a certain mpi-configuration should be stored.
	 * NOTE that the file indicated by this path might not exist.
	 * 
	 * @param 	order
	 *       	The order of the Dedekind number that the state was computing
	 * @param 	rank
	 *       	The rank of the node which was in that state
	 * @param	nrOfNodes
	 *       	The number of nodes used for computation
	 * @return	the {@code Path} to the file storing the state
	 *        	for computation of Dedekind number {@code order}
	 *        	as calculated on a node with rank {@code rank}
	 *        	in an environment with {@code nrOfNodes} nodes
	 * 
	 * @see #getDirectoryPath()
	 */
	public static Path getFilePath(int order, int rank, int nrOfNodes) {
		String file = String.format(ComputationState.FILENAME_FORMAT, order, rank, nrOfNodes);
		return ComputationState.getDirectoryPath().resolve(file);
	}
	
	/**
	 * Recover the state for a certain computation from a given file.
	 * 
	 * @param 	path
	 *       	The {@code Path} to the file where the state is stored
	 * @return	the {@code ComputationState} stored in {@code path}
	 *        	if the file stores a valid state, 
	 *        	{@code null} otherwise
	 * 
	 * @see #getFilePath(int, int, int)
	 */
	private static ComputationState recoverState(Path path) {
		try(InputStream is = Files.newInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(is)) {
			return (ComputationState) ois.readObject();
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Recover the state for the computation of a given Dedekind number 
	 * with the given identifier, from the file it should be stored in.
	 * 
	 * @param 	order 
	 *       	The order of the Dedekind number that the state was computing
	 * @param 	rank
	 *       	The rank of the node which was in that state
	 * @param	nrOfNodes
	 *       	The number of nodes used for computation
	 * @return	the {@code ComputationState} with id {@code id} 
	 *        	for computation of Dedekind number {@code order}
	 *        	if this state is stored in the correct file,
	 *        	{@code null} otherwise
	 * 
	 * @see #recoverState(Path)
	 * @see #store()
	 */
	public static ComputationState recoverState(int order, int rank, int nrOfNodes) {
		return ComputationState.recoverState(ComputationState.getFilePath(order, rank, nrOfNodes));
	}
	
	/**
	 * Recover all states for the computation of a given Dedekind number
	 * with an identifier in a given interval, from the files they should be stored in.
	 * 
	 * @param 	order 
	 *       	The order of the Dedekind number that the state was computing
	 * @param 	from
	 *       	The lowest rank to find a state for
	 * @param 	to
	 *       	The highest rank to find a state for
	 * @param	nrOfNodes
	 *       	The number of nodes used for computation
	 * @return	A collection containing all states computing Dedekind number 
	 *        	{@code order} with an identifier in {@code [from, to]} 
	 *        	omitting states that were not stored (correctly)
	 * @throws 	IllegalArgumentException if {@code to < from}
	 * 
	 * @see #recoverState(int, int, int)
	 */
	public static Collection<ComputationState> recoverStates(int order, int from, int to, int nrOfNodes) 
			throws IllegalArgumentException {
		Collection<ComputationState> result = new ArrayList<>(to - from);
		ComputationState state;
		
		for(int i = from; i < to; i++) {
			state = ComputationState.recoverState(order, i, nrOfNodes);
			if(state != null)
				result.add(state);
		}
		
		return result;
	}
	
	/**
	 * Recover all states for the computation of a given Dedekind number
	 * with a certain amount of nodes from the files they should be stored in.
	 * 
	 * @param 	order 
	 *       	The order of the Dedekind number that the state was computing
	 * @param	nrOfNodes
	 *       	The number of nodes used for computation
	 * @return	A collection containing all states computing Dedekind number 
	 *        	{@code order} that were stored correctly (might be empty)
	 * 
	 * @since Java 8
	 * 
	 * @see #recoverState(int, int, int)
	 * @see #recoverState(Path)
	 * @see #store()
	 */
	public static Collection<ComputationState> recoverAllStates(int order, int nrOfNodes) {
		Collection<ComputationState> result = new ArrayList<>();
		String regex = ComputationState.FILENAME_FORMAT
				.replaceFirst("%d", String.valueOf(order))
				.replaceFirst("%d", "\\.")
				.replaceFirst("%d", String.valueOf(nrOfNodes));
		
		try {
			result = Files.walk(getDirectoryPath())
			.filter(a -> a.getFileName().toString().matches(regex))
			.map(a -> ComputationState.recoverState(a))
			.collect(Collectors.toCollection(ArrayList::new));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return result;
	}
	
	/******************************************************************
	 * Basic methods                                                  *
	 ******************************************************************/
	
	/**
	 * @return a string presenting a complete overview of this state.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("State:");
		result.append("\nComputation of |A(");
		result.append(getOrder());
		result.append(")|");
		result.append("\non node ");
		result.append(getRank());
		result.append(" of ");
		result.append(getNumberOfNodes());
		result.append("\nSize Equivalence Classes: ");
		if(getEquivalenceClasses() != null)
			result.append(getEquivalenceClasses().size());
		result.append("\nSize Left Interval Sizes: ");
		if(getLeftIntervalSizes() != null)
			result.append(getLeftIntervalSizes().size());
		result.append("\nSize Munueta Equivalence Classes: ");
		if(getMunuetaEquivalenceClasses() != null)
			result.append(getMunuetaEquivalenceClasses().size());
		result.append("\nSum: ");
		result.append(getSum());
		result.append("\n-------------------------------------------");
		for(Entry<String, Long> timing : timings.entrySet()) {
			result.append(String.format("\n%-30s %10dms", timing.getKey(), timing.getValue()));
		}
		result.append("\n-------------------------------------------");
		return result.toString();
	}
	
	/**
	 * Print the state stored in a file to the standard output.
	 * 
	 * @param 	args
	 *       	Flags to indicate which file to read from, 
	 *       	the {@code -h} flag prints the help section.
	 */
	public static void main(String[] args) {
		int order = 3, rank = -1, nrOfNodes = 4;
		ArrayList<ComputationState> states = new ArrayList<>();
		
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			switch (a) {
				case "-n" : order = Integer.valueOf(args[++i]); break;
				case "-s" : nrOfNodes = Integer.valueOf(args[++i]); break;
				case "-r" : rank = Integer.valueOf(args[++i]); break;
				case "-f" : states.add(recoverState(Paths.get(args[++i])));break;
				case "-h" : System.out.println("parameters -n -s -r -f -h\n"
						+ "-n number : show states for computation of |A(number)| (default: 3)\n"
						+ "-s nrOfNodes : specify the number of nodes that were used for computation(default: 4)\n"
						+ "-r rank : specify the rank of the node that was computing\n"
						+ "-f file : show state stored in file (multiple -f flags possible)\n"
						+ "-h : this text");return;
				default : System.out.println("Ignored " + a);break;
			}
		}
		
		if(states.isEmpty() && order > 2 && nrOfNodes > 0) {
			if(rank < 0)
				states.addAll(recoverAllStates(order, nrOfNodes));
			else
				states.add(recoverState(order, rank, nrOfNodes));
		}
		
		for(ComputationState state : states) {
			System.out.println(state + "\n");
		}
		
	}
}
