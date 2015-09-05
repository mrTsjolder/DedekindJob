package step3;

import amfsmall.AntiChainInterval;
/**
 * the most basic collectiion does not store any information
 * @author u0003471
 *
 */
public class BasicIntervalCollection implements IntervalCollection {

	@Override
	public void store(AntiChainInterval ab, Long value) {
	}

	@Override
	public Long retrieve(AntiChainInterval ab) {
		return ab.latticeSize();
	}

}
