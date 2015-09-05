package auxiliary;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public abstract class AThread<T extends Comparable<T>> extends Thread {

	private CollectorMap<T> collector;
	private long time;

	public CollectorMap<T> getCollector() {return collector;}
	
	/**
	 * creat a AThread using the collectorMap c and let the thread enter the collector
	 * @param c
	 * @throws InterruptedException
	 */
	public AThread(CollectorMap<T> c) throws InterruptedException {
		collector = c;
		collector.enter();
	}
	
	abstract protected void doTheJob();
	
	/**
	 * Receive factor entries of input to be registered together with a counter
	 * @param t the new input
	 * @param factor the number of entries to be counted
	 * @param i the counter
	 */
	public void receive(T t,long factor, int i) {
		collector.register(t, factor, i, getCpuTime() - time);
	}
	
	/**
	 * Receive factor entries of input to be registered together with a zero value for the counter
	 * @param t the new input
	 * @param factor the number of entries to be counted
	 */
	public void receive(T t, long factor) {
		collector.register(t, factor, 0, getCpuTime() - time);
	}
	
	/**
	 * has this T been valued
	 * @param t the T to be inspected
	 * @return
	 */
	public boolean valued(T t) {
		return collector.valued(t);
	}
	
	/**
	 * assign the value v to T t
	 * @param t
	 * @param v
	 */
	public void set(T t, ComputedLong v) {
		collector.cset(t,v);
	}
	
	/**
	 * assign the value v to T t
	 * @param t
	 * @param v
	 */
	public void set(T t, Long v) {
		collector.set(t,v);
	}
	
	public void run() {
		time = getCpuTime();
		doTheJob();
		collector.leave();
	}
	
	public long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}

}
