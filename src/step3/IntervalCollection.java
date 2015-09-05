package step3;

import amfsmall.AntiChainInterval;
/**
 * A general collection of intervals
 * It allows to associate a value (e.g. the size) with an interval
 * @author Patrick De Causmaecker
 *
 */
public interface IntervalCollection {
	/**
	 * store a value with an interval
	 * @post if the interval already is in the collection and the value is different the result is not predictable
	 * @post the collection may ignore this method completely
	 * @param ab
	 * @param value
	 */
	public void store(AntiChainInterval ab, Long value);
	/**
	 * retrieve a value for an interval
	 * @param ab
	 * @return the value presently associated with the interval
	 */
	public Long retrieve(AntiChainInterval ab);
}
