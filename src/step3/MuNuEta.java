package step3;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;
import amfsmall.SmallBasicSet;
import auxiliary.AThread;
import auxiliary.CollectorMap;

/**
 * class representing triplets of antichains
 * class is not safe wrt its component antichains : changes to m,n and e will influence the class
 * Implementation is immutable, treat antichains in it immutable
 * @author Patrick De Causmaecker
 *
 */
public class MuNuEta implements Comparable<MuNuEta> {
	
	public final AntiChain mu;
	public final AntiChain nu;
	public final AntiChain eta;
	private boolean sorted;
	private boolean nonZero;
	private boolean nonZeroChecked;
	private AntiChain top;
	private AntiChain mina;
	private AntiChain minb;
	private AntiChain minc;
	
	public MuNuEta(AntiChain m,AntiChain n,AntiChain e) {
		mu = m;
		nu = n;
		eta = e;
		mina = null;
		minb = null;
		minc = null;		
		sorted = false;
		nonZero = false;
		nonZeroChecked = false;
		top = null;
	}
	
	/**
	 * create a sorted triplet
	 * @param m
	 * @param n
	 * @param e
	 * @pre isSorted => m, n and e are sorted
	 * @param isSorted
	 */
	protected MuNuEta(AntiChain m,AntiChain n,AntiChain e, boolean isSorted) {
		this(m,n,e);
		sorted = isSorted;
	}
	
	/**
	 * 4th order p-coefficient is the number of solutions (a,b,c) to the equations
	 * a v b = mu, a v c = nu, b v c = eta, a.meet(b).meet(c) = abc
	 * @param abc
	 * @return 3th order p-coefficient
	 */
	public long p3(AntiChain abc) {
		if (isNonZero()) return P3B.p(abc,this); // P3B is the fastest version for n <= 4
		else return 0;
	}
	
	/**
	 * does the triplet know that it is sorted
	 * the triplet may be sorted and still return false
	 * @return result == true => mu >= nu >= eta 
	 */
	public boolean isSorted() {
		return sorted;
	}
	
	/**
	 * a triplet is nonZero if there is at least one solution (a,b,c) such that
	 * a v b = mu, a v c = nu, b v c = eta
	 * @return there is at least one solution
	 */
	public boolean isNonZero() {
		if (nonZeroChecked) return nonZero;
		// every largest set must occur at least in two of mu,nu,eta
		// determine minimum values for a,b,c
		mina = new AntiChain();
		minb = new AntiChain();
		minc = new AntiChain();		
		nonZero = true;
		for (SmallBasicSet s : getTop()) {
			BitSet unSeen = new BitSet();
			unSeen.set(0); // a
			unSeen.set(1); // b
			unSeen.set(2); // c
			if (mu.contains(s)) unSeen.flip(2);
			if (nu.contains(s)) unSeen.flip(1);
			if (eta.contains(s)) unSeen.flip(0);
			if (unSeen.cardinality() > 1) { nonZero = false; break; }
			else if (unSeen.cardinality() == 1) {
				if (unSeen.get(0)) mina.add(s);
				else if (unSeen.get(1)) minb.add(s);
				else if (unSeen.get(2)) minc.add(s);
			}
		}
		nonZeroChecked = true;
		if (!nonZero) {
			mina = null;
			minb = null;
			minc = null;
		}
		return nonZero;
	}
	
	/**
	 * 
	 * @return a lower bound on a for a v b = mu, a v c = nu, b v c = eta
	 */
	public AntiChain getMina() {
		isNonZero();
		return mina;
	}
	
	/**
	 * 
	 * @return a lower bound on b for a v b = mu, a v c = nu, b v c = eta
	 */
	public AntiChain getMinb() {
		isNonZero();
		return minb;
	}
	
	/**
	 * 
	 * @return a lower bound on c for a v b = mu, a v c = nu, b v c = eta
	 */
	public AntiChain getMinc() {
		isNonZero();
		return minc;
	}
	
	/**
	 * the bottom is a lower bound on a.meet(b).meet(c) for
	 * a v b = mu, a v c = nu, b v c = eta
	 * it needs not be reachable
	 * if such a bound does not exist, null is returned
	 * @return getMina().join(getMinb()).join(getMinc())
	 */
	public AntiChain getBottom() {
		if (isNonZero()) return mina.meet(minb).meet(minc);
		else return null;
	}
	
	/**
	 * the MaxBottom is an upper bound on a.meet(b).meet(c)
	 * a v b = mu, a v c = nu, b v c = eta
	 * it needs not be reachable
	 * 
	 * @return
	 */
	public AntiChain getMaxBottom() {
		return mu.meet(nu).meet(eta);
	}
	
	/**
	 * the top of the triplet is mu v nu v eta
	 * it is the bottom of the set of rho that can have nonzero 
	 * product of sizes of intervals [x,rho] for x=mu,nu,eta
	 * @return mu v nu v eta
	 */
	public AntiChain getTop() {
		if (top != null) return top;
		top = mu.join(nu).join(eta);
		return top;
	}

	/**
	 * lexicographic comparison building on comparison of antichains
	 */
	@Override
	public int compareTo(MuNuEta o) {
		int c = mu.compareTo(o.mu);
		if (c != 0) return c;
		c = nu.compareTo(o.nu);
		if (c != 0) return c;
		return eta.compareTo(o.eta);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eta == null) ? 0 : eta.hashCode());
		result = prime * result + ((mu == null) ? 0 : mu.hashCode());
		result = prime * result + ((nu == null) ? 0 : nu.hashCode());
		return result;
	}
	
	/**
	 * equality consistent with
	 * lexicographic comparison building on comparison of antichains
	 */
	@Override
	public boolean equals(Object o) {
		try {
			return compareTo((MuNuEta) o) == 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * The minimal triplet equivalent with this under g
	 * @param g
	 * @return
	 */
	public MuNuEta standard(Set<int[]> g) {
		MuNuEta best = this.sort();
		for (int[] p : g) {
			MuNuEta mapped = this.map(p).sort();
			if (mapped.compareTo(best) < 0) best = mapped;
		}
		return best;
	}
	/**
	 * produce a MuNuEta where mu >= nu >= eta
	 * not safe wrt mu,nu and eta
	 * @return a sorted triplet with result.sorted() == true
	 */
	public MuNuEta sort() {
		if (sorted) return this;
		AntiChain nmu,nnu,neta;
		if (mu.compareTo(nu) >= 0) {nmu = mu;nnu = nu;}
		else {nmu = nu;nnu = mu;}
		if (eta.compareTo(nmu) > 0) {neta = nmu;nmu = eta;}
		else {neta = eta;}
		if (neta.compareTo(nnu) > 0) {AntiChain h = nnu;nnu = neta;neta = h;}
		return new MuNuEta(nmu,nnu,neta,true);
	}

	/**
	 * p induces a permutation of sp(mu.join(nu).join(eta))
	 * @param p
	 * @return the mapped triplet
	 */
	public MuNuEta map(int[] p) {
		return new MuNuEta(mu.map(p),nu.map(p),eta.map(p));
	}
	
	/**
	 * sum of products of sizes of intervals [x,rho] for x = mu,nu,eta
	 * for all rho in M(n) 
	 * @param n
	 * @return sum of products
	 */
	public long rhoIntegral(int n) {
		long res = 0;
		for (AntiChain rho : new AntiChainInterval(mu.join(nu).join(eta),AntiChain.universeFunction(n))) {
			res += new AntiChainInterval(mu,rho).latticeSize()
					* new AntiChainInterval(nu,rho).latticeSize()
					* new AntiChainInterval(eta,rho).latticeSize();
		}
		return res;
	}

	/**
	 * sum of products of sizes of intervals [x,rho] for x = mu,nu,eta
	 * for all rho in M(n) 
	 * @param standardSizes sizes of all standard intervals in space n
	 * @param n
	 * @return sum of products
	 */
	private Long rhoIntegral(Set<int[]> g,SortedMap<AntiChainInterval, Long> standardSizes, int n) {
		long res = 0;
		for (AntiChain rho : new AntiChainInterval(mu.join(nu).join(eta),AntiChain.universeFunction(n))) {
			res +=  standardSizes.get(new AntiChainInterval(mu,rho).fastStandard(g,n))
					* standardSizes.get(new AntiChainInterval(nu,rho).fastStandard(g,n))
					* standardSizes.get(new AntiChainInterval(eta,rho).fastStandard(g,n));
		}
		return res;
	}

	/**
	 * sum of products of sizes of intervals [x,rho] for x = mu,nu,eta
	 * for all rho in M(n) 
	 * @param standardSizes sizes of all standard intervals in space n
	 * Complexity 2^(n n/2)
	 * @param n
	 * @return sum of products
	 */
	public Long rhoIntegral(IntervalCollection standardSizes, int n) {
		long res = 0;
		for (AntiChain rho : new AntiChainInterval(mu.join(nu).join(eta),AntiChain.universeFunction(n))) { // 2^(n n/2)
			res +=  standardSizes.retrieve(new AntiChainInterval(mu,rho))
					* standardSizes.retrieve(new AntiChainInterval(nu,rho))
					* standardSizes.retrieve(new AntiChainInterval(eta,rho));
		}
		return res;
	}


	/**
	 * sum of products of sizes of intervals [x,rho] for x = mu,nu,eta
	 * for all rho in M(n)
	 * using precomputed intervalsizes in intervalSizes 
	 * @param n
	 * @return sum of products
	 */
	public long rhoIntegral(int n, SortedMap<AntiChainInterval,Long> intervalSizes) {
		long res = 0;
		for (AntiChain rho : new AntiChainInterval(mu.join(nu).join(eta),AntiChain.universeFunction(n))) {
			res +=  intervalSizes.get(new AntiChainInterval(mu,rho))
					* intervalSizes.get(new AntiChainInterval(nu,rho))
					* intervalSizes.get(new AntiChainInterval(eta,rho));
		}
		return res;
	}

	/**
	 * sum of products of sizes of intervals [x,rho] multiplied by rhoSet.get(rho)
	 * for x = mu,nu,eta and rho in rhoSet.keySet()
	 * @param rhoSet
	 * @return sum of products
	 */
	public long rhoIntegral(SortedMap<AntiChain,Long> rhoSet) {
		long res = 0;
		for (AntiChain rho : rhoSet.keySet()) {
			res += rhoSet.get(rho)
					* new AntiChainInterval(mu,rho).latticeSize()
					* new AntiChainInterval(nu,rho).latticeSize()
					* new AntiChainInterval(eta,rho).latticeSize();
		}
		return res;
	}

	/**
	 * sum of products of sizes of intervals [x,rho] multiplied by rhoSet.get(rho)
	 * for x = mu,nu,eta and rho in rhoSet.keySet()
	 * using precomputed intervalsizes in intervalSizes
	 * @param rhoSet
	 * @return sum of products
	 */
	public long rhoIntegral(SortedMap<AntiChain,Long> rhoSet, SortedMap<AntiChainInterval,Long> intervalSizes) {
		long res = 0;
		for (AntiChain rho : rhoSet.keySet()) {
			res += rhoSet.get(rho)
					* intervalSizes.get(new AntiChainInterval(mu,rho))
					* intervalSizes.get(new AntiChainInterval(nu,rho))
					* intervalSizes.get(new AntiChainInterval(eta,rho));
		}
		return res;
	}
	
	/**
	 * Generate all rho integrals for a given set of munueta triples
	 * @param set
	 * @param n
	 * @return mapping for each triple to its rhointegral value
	 */
	public static SortedMap<MuNuEta,Long> rhoIntegrals(Set<MuNuEta> set,int n) {
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		for (MuNuEta mne : set) {
			res.put(mne, mne.rhoIntegral(n));
		}
		return res;
	}
	
	/**
	 * Generate all rho integrals for a given set of munueta triples
	 * @param set
	 * @param standardSizes sizes of standard intervals in space n
	 * @param n
	 * @return mapping for each triple to its rhointegral value
	 */
	public static SortedMap<MuNuEta, Long> rhoIntegrals(Set<MuNuEta> set,
			SortedMap<AntiChainInterval, Long> standardSizes, int n) {
		Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(AntiChain.universeFunction(n+1).symmetryGroup());
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		for (MuNuEta mne : set) {
			res.put(mne, mne.rhoIntegral(g, standardSizes, n));
		}
		return res;
	}

	/**
	 * Generate all rho integrals for a given set of munuetq triples
	 * @param set
	 * @param standardIntervalSizes sizes of standard intervals in space n
	 * @param n
	 * Complexity 2^(5/2*(n n/2))
	 * @return mapping for each triple to its rhointegral value
	 */
	public static SortedMap<MuNuEta, Long> rhoIntegrals(Set<MuNuEta> set,
			IntervalCollection standardIntervalSizes, int n) {
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		for (MuNuEta mne : set) { // 2^(3/2*(n n/2))
			res.put(mne, mne.rhoIntegral(standardIntervalSizes,n)); // 2^(n n/2)
		}
		return res;
	}

	/**
	 * Generate all rho integrals for a given set of munuetq triples
	 * using p processors
	 * @param set
	 * @param standardIntervalSizes sizes of standard intervals in space n
	 * @param n
	 * @param p the number of processors
	 * Complexity 2^(5/2*(n n/2))
	 * @return mapping for each triple to its rhointegral value
	 * @throws InterruptedException 
	 */
	public static SortedMap<MuNuEta, Long> rhoIntegrals(Set<MuNuEta> set,
			final IntervalCollection standardIntervalSizes, final int n, int p) throws InterruptedException {
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		CollectorMap<MuNuEta> collector = new CollectorMap<MuNuEta>(p);
		for (final MuNuEta mne : set) { // 2^(3/2*(n n/2))
			new AThread<MuNuEta>(collector) {

				@Override
				public void doTheJob() {
					long v = mne.rhoIntegral(standardIntervalSizes,n);
					set(mne, v); // 2^(n n/2)
				}
			}.start();
		}
		if (collector.isReady()) res = collector.getResult();
		return res;
	}

	
	@SuppressWarnings("unused")
	private static Set<Set<MuNuEta>> partition(Set<MuNuEta> set, long l) {
		ArrayList<Set<MuNuEta>> res = new ArrayList<Set<MuNuEta>>();
		for (int i=0;i<l;i++) res.add(new TreeSet<MuNuEta>());
		Iterator<Set<MuNuEta>> resit = res.iterator();
		for (MuNuEta mne : set) {
			if (!resit.hasNext()) resit = res.iterator();
			resit.next().add(mne);
		}
		return new HashSet<Set<MuNuEta>>(res);
	}

	/**
	 * generate all MuNuEta for a given n that allow at least one solution (a,b,c)
	 * with a v b = mu, a v c = nu, b v c = eta
	 * @param n
	 * @return a set with all consistent MuNuEta triples
	 */
	public static SortedSet<MuNuEta> category(int n) {
		SortedSet<MuNuEta> res = new TreeSet<MuNuEta>();
		AntiChainInterval space = AntiChainInterval.fullSpace(n);

		for (AntiChain mu : space)
			for (AntiChain nu : space) {
				AntiChain etamu = new AntiChain(mu);
				etamu.removeAll(nu);
				AntiChain etanu = new AntiChain(nu);
				etanu.removeAll(mu);
				for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) {
					res.add(new MuNuEta(mu,nu,eta));
				}
			}
		return res;
	}
	
	/**
	 * Generate non equivalent munueta for a given value of n
	 * @param n
	 * Complexity n!*2^(3*(n n/2)) 
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 */
	public static SortedMap<MuNuEta,Long> equivalenceClasses(int n) {
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(); // n!
		AntiChainInterval space = AntiChainInterval.fullSpace(n);

		for (AntiChain mu : space) // 2^(n n/2)
			for (AntiChain nu : space) { // 2^(n n/2)
				AntiChain etamu = new AntiChain(mu);
				etamu.removeAll(nu); // (n n/2)
				AntiChain etanu = new AntiChain(nu);
				etanu.removeAll(mu); // (n n/2)
				for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) { // 2^(n n/2)
					MuNuEta mne = new MuNuEta(mu,nu,eta).standard(g); // n!
					if (res.containsKey(mne)) res.put(mne, res.get(mne) + 1);
					else res.put(mne, 1L);
				}
			}
		return res;
	}

	/**
	 * Generate non equivalent munueta for a given value of n
	 * Use a set of non equivalent antichains for the first iteration
	 * @param n
	 * @param ne a set of non equivalent antichains with the sizes of their equivalence classes
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 */
	public static SortedMap<MuNuEta,Long> equivalenceClasses(int n, SortedMap<AntiChain, Long> ne) {
		SortedMap<MuNuEta,Long> res = new TreeMap<MuNuEta,Long>();
		Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(); // n!
		AntiChainInterval space = AntiChainInterval.fullSpace(n);

		for (AntiChain mu : ne.keySet()) {
			Long muFactor = ne.get(mu);
			for (AntiChain nu : space) { // 2^(n n/2)
				AntiChain etamu = new AntiChain(mu);
				etamu.removeAll(nu); // (n n/2)
				AntiChain etanu = new AntiChain(nu);
				etanu.removeAll(mu); // (n n/2)
				for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) { // 2^(n n/2)
					MuNuEta mne = new MuNuEta(mu,nu,eta).standard(g); // n!
					if (res.containsKey(mne)) res.put(mne, res.get(mne) + muFactor);
					else res.put(mne, muFactor);
				}
			}
			
		}
		return res;
	}

	/**
	 * Generate non equivalent munueta for a given value of n
	 * Use a set of non equivalent antichains for the first iteration
	 * Use p parallel processes to do the job
	 * @param n
	 * @param ne a set of non equivalent antichains with the sizes of their equivalence classes
	 * @param p the number of parallel processes to be used
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 * @throws InterruptedException 
	 */
	public static SortedMap<MuNuEta,Long> equivalenceClasses(int n, final SortedMap<AntiChain, Long> ne, int p) throws InterruptedException {
		SortedMap<MuNuEta,Long> res = new TreeMap<>();
		final Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(); // n!
		final AntiChainInterval space = AntiChainInterval.fullSpace(n);

		CollectorMap<MuNuEta> theCollector = new CollectorMap<>(p);

		for (final AntiChain mu : ne.keySet()) {
			new AThread<MuNuEta>(theCollector) {

				@Override
				public void doTheJob() {
					Long muFactor = ne.get(mu);
					AntiChain etamu, etanu;
					MuNuEta mne;
					for (AntiChain nu : space) { // 2^(n n/2)
						etamu = new AntiChain(mu);
						etamu.removeAll(nu); // (n n/2)
						etanu = new AntiChain(nu);
						etanu.removeAll(mu); // (n n/2)
						for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) { // 2^(n n/2)
							mne = new MuNuEta(mu,nu,eta).standard(g); // n!
							receive(mne,muFactor);
						}
					}
				}
			}.start();
		}
		if (theCollector.isReady())	res = theCollector.getResult();
		return res;
	}

	/**
	 * Generate non equivalent munueta for a given value of n
	 * Use a set of non equivalent antichains for the first iteration
	 * @param n
	 * @param ne a set of non equivalent antichains with the sizes of their equivalence classes
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 */
	public static SortedMap<MNECode, Long> equivalenceClassesCoded(int n,
			SortedMap<AntiChain, Long> ne) {
		SortedMap<MNECode, Long> result = new TreeMap<>();
		Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup();
		AntiChainInterval space = AntiChainInterval.fullSpace(n);
		AntiChain etamu, etanu;
		MNECode mne;
		
		for(Entry<AntiChain, Long> entry : ne.entrySet()) {
			for(AntiChain nu : space) {
				etamu = new AntiChain(entry.getKey());
				etamu.removeAll(nu);
				etanu = new AntiChain(nu);
				etanu.removeAll(entry.getKey());
				for(AntiChain eta : new AntiChainInterval(etamu.join(etanu), entry.getKey().join(nu))) {
					mne = new MuNuEta(entry.getKey(), nu, eta).standard(g).encode();
					result.merge(mne, entry.getValue(), (v1, v2) -> v1 + v2);
				}
			}
		}
		return result;
	}

	/**
	 * Generate non equivalent munueta for a given value of n
	 * Use a set of non equivalent antichains for the first iteration
	 * Use p parallel processes to do the job
	 * @param n
	 * @param ne a set of non equivalent antichains with the sizes of their equivalence classes
	 * @param p the number of parallel processes to be used
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 * @throws InterruptedException 
	 */
	public static SortedMap<MNECode,Long> equivalenceClassesCoded(int n, final SortedMap<AntiChain, Long> ne, int p) throws InterruptedException {
		SortedMap<MNECode,Long> res = new TreeMap<>();
		final Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(); // n!
		final AntiChainInterval space = AntiChainInterval.fullSpace(n);

		CollectorMap<MNECode> theCollector = new CollectorMap<>(p);

		for (final AntiChain mu : ne.keySet()) {
			new AThread<MNECode>(theCollector) {

				@Override
				public void doTheJob() {
					Long muFactor = ne.get(mu);
					AntiChain etamu, etanu;
					MuNuEta mne;
					for (AntiChain nu : space) { // 2^(n n/2)
						etamu = new AntiChain(mu);
						etamu.removeAll(nu); // (n n/2)
						etanu = new AntiChain(nu);
						etanu.removeAll(mu); // (n n/2)
						for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) { // 2^(n n/2)
							mne = new MuNuEta(mu,nu,eta).standard(g); // n!
							receive(mne.encode(),muFactor);
						}
					}
				}
			}.start();
		}
		if (theCollector.isReady())	res = theCollector.getResult();
		return res;
	}

	/**
	 * Generate non equivalent munueta for a given value of n
	 * Use a set of non equivalent antichains for the first iteration
	 * @param n
	 * @param ne a set of non equivalent antichains with the sizes of their equivalence classes
	 * @param pool
	 * @return a map of equivalenceclasses with their smallest (compareTo) representative
	 */
	public static SortedMap<MNECode,Long> equivalenceClassesCoded(int n, SortedMap<AntiChain, Long> ne, ExecutorService pool) {
		SortedMap<MNECode,Long> res = new TreeMap<>();
		Set<int[]> g = AntiChain.universeFunction(n).symmetryGroup(); // n!
		AntiChainInterval space = AntiChainInterval.fullSpace(n);
		ArrayList<Future<SortedMap<MNECode, Long>>> futures = new ArrayList<>(ne.size());

		for (Entry<AntiChain, Long> e : ne.entrySet()) {
			for(AntiChain nu : space) {
				futures.add(pool.submit(new Callable<SortedMap<MNECode, Long>>() {
	
						@Override
						public SortedMap<MNECode, Long> call() {
							SortedMap<MNECode, Long> temp = new TreeMap<>();
							AntiChain mu = e.getKey();
							AntiChain etamu = new AntiChain(mu);
							etamu.removeAll(nu); // (n n/2)
							AntiChain etanu = new AntiChain(nu);
							etanu.removeAll(mu); // (n n/2)
							
							for (AntiChain eta : new AntiChainInterval(etamu.join(etanu),mu.join(nu))) { // 2^(n n/2)
								MNECode mne = new MuNuEta(mu,nu,eta).standard(g).encode(); // n!
								temp.merge(mne, e.getValue(), (v1, v2) -> v1 + v2);
							}
							return temp;
						}
						
				}));
			}
		}
		
		try {
			for(Future<SortedMap<MNECode, Long>> f : futures)
				for(Entry<MNECode, Long> e : f.get().entrySet())
					res.merge(e.getKey(), e.getValue(), (v1, v2) -> v1 + v2);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	public String toString() {
		return "(" + mu + ", " + nu + ", " + eta + ")";
	}
	
	public MNECode encode() {
		return new MNECode(mu.encode(), nu.encode(), eta.encode());
	}
	
	public static MuNuEta decode(MNECode b) {
		return new MuNuEta(AntiChain.decode(b.getM()), AntiChain.decode(b.getN()), AntiChain.decode(b.getE()));
	}

	/**
	 * more memory-friendly way to represent a {@code MuNuEta} object
	 */
	public static final class MNECode implements Comparable<MNECode>, Serializable {
		
		private static final long serialVersionUID = 383513807507254042L;
		
		private final BigInteger m;
		private final BigInteger n;
		private final BigInteger e;


		public MNECode(BigInteger encodedMu, BigInteger encodedNu, BigInteger encodedEta) {
			m = encodedMu;
			n = encodedNu;
			e = encodedEta;
		}

		/**
		 * @return the mu
		 */
		public final BigInteger getM() {
			return m;
		}

		/**
		 * @return the nu
		 */
		public final BigInteger getN() {
			return n;
		}

		/**
		 * @return the eta
		 */
		public final BigInteger getE() {
			return e;
		}

		@Override
		public int compareTo(MNECode o) {
			int res = getM().compareTo(o.getM());
			if(res == 0)
				res = getN().compareTo(o.getN());
			return res == 0 ? getE().compareTo(o.getE()) : res;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			result = prime * result + ((m == null) ? 0 : m.hashCode());
			result = prime * result + ((n == null) ? 0 : n.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			try {
				return compareTo((MNECode) obj) == 0;
			} catch (Exception e) {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return "(" + AntiChain.decode(m) + ", "
					+ AntiChain.decode(n) + ", "
					+ AntiChain.decode(e) + ")";
		}

	}

}
