package auxiliary;

import java.util.SortedMap;
import java.util.TreeMap;

public class CollectorMap<T> {
	public final int numberOfAllowedProcesses;
	private int numberOfProcesses;
	private SortedMap<T,Long> result;
	private long iterations;
	private long time;
	synchronized public int getNumberOfProcesses() {return numberOfProcesses;}

	/**
	 * Create a collector acting as a synchronizer for p processes
	 * The collector can register items of type T and keeps a counter
	 * @param p
	 */
	public CollectorMap(int p) {
		numberOfAllowedProcesses = p;
		numberOfProcesses = 0;
		result = new TreeMap<>();
		iterations = 0;
		time = 0;
	}
	
	synchronized public void enter() throws InterruptedException
	{
//		System.out.println(numberOfProcesses + ", " + numberOfAllowedProcesses);
		if (numberOfProcesses == numberOfAllowedProcesses) {
//			System.out.println("WAITING");
			wait();
		}
		numberOfProcesses++;
	}
	
	synchronized public void leave() {
//		System.out.println(numberOfProcesses + ", " + numberOfAllowedProcesses);
//		System.out.println("Leaving");
		numberOfProcesses--;
		notify();
	}
	
	synchronized public void register(T v, long factor, long its, long sec) {
		store(v,factor);
		iterations += its;
		time += sec;
	}
	
	/**
	 * conditionally set the value assigned to t to v
	 * @param t
	 * @param v
	 */
	synchronized public void cset(T t, ComputedLong v) {
		if (!result.containsKey(t)) result.put(t, v.compute());
	}
	
	/**
	 * set the value assigned to t to v
	 * @param t
	 * @param v
	 */
	synchronized public void set(T t, Long v) {
		result.put(t, v);
	}
	
	/**
	 * has the T t received a value
	 * @param t
	 * @return
	 */
	synchronized public boolean valued(T t) {
		return result.containsKey(t);
	}
	
	private void store(T v, long factor) {
		if (result.containsKey(v)) result.put(v,result.get(v)+factor);
		else result.put(v,factor);
	}

	synchronized public boolean isReady() throws InterruptedException {
		while (numberOfProcesses > 0) wait();
		return true;
	}
	
	public synchronized SortedMap<T,Long> getResult() {
		return result;
	}
	
	synchronized public long iterations() {
		return iterations;		
	}

	synchronized public long time() {
		return time;		
	}

	synchronized public long numberOfProcesses() {
		return numberOfProcesses;		
	}
	
	synchronized public long numberOfAllowedProcesses() {
		return numberOfAllowedProcesses;		
	}
	
	@Override
	synchronized public String toString() {
		String res = ""+ getResult();
		long total = 0L;
		for (T t : getResult().keySet()) {
			total += getResult().get(t);
		}
		res += "\nTotal = " + total;
		res += "\nIterations = " + iterations();
		res += "\nTime = " + time();
		res += "\nProcesses = " + numberOfProcesses();
		res += "\nMaximal   = " + numberOfAllowedProcesses();
		return res;
	}

}
