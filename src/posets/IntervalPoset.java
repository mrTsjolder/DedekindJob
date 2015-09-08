package posets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import amfsmall.*;

public class IntervalPoset extends SimplePosetSize<SmallBasicSet> {

	private SortedSet<SmallBasicSet>[] level;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> successors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> predecessors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> before;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> after;
	static int cnt = 100;

	private void buildAfter(SmallBasicSet v) {
		SortedSet<SmallBasicSet> a = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet p : successors.get(v)) {
			a.add(p);
			a.addAll(getAfter(p));
		}
		after.put(v, a);
	}

	private void buildBefore(SmallBasicSet v) {
		SortedSet<SmallBasicSet> b = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet p : predecessors.get(v)) {
			b.add(p);
			b.addAll(getBefore(p));
		}
		before.put(v, b);
	}
	
	@Override
	public SortedSet<SmallBasicSet> getAfter(SmallBasicSet v) {
		if (!after.containsKey(v) && successors.containsKey(v)) buildAfter(v);
		return after.get(v);
	}
	
	@Override
	public SortedSet<SmallBasicSet> getBefore(SmallBasicSet v) {
		if (!before.containsKey(v) && predecessors.containsKey(v)) buildBefore(v);
		return before.get(v);
	}

	@Override
	public long getLatticeSize()  {
		return getLatticeSize(this.getMaximalLevelNumber() % 2 != 1);
	}

	@Override
	public long getLatticeSize(boolean odd) {
		int firstLevel;
		int exp;
		if (odd) {
			exp = 0;
			firstLevel = 1;
		}
		else {
			exp = (getLevel(1).size());
			firstLevel = 2;
		}
		
		// for all levels firstLevel + 2k, compute the set of predecessors of the predecessors
		Map<SmallBasicSet,Set<SmallBasicSet>> prepredec = new HashMap<SmallBasicSet,Set<SmallBasicSet>>();
//		System.out.print("Lattice Size doing ");
		for (int i=firstLevel;i<=this.getMaxLevel();i+=2) {
//			System.out.print(getLevel(i).size() + " ");
			for (SmallBasicSet s : getLevel(i)) {
				Set<SmallBasicSet> prepre = new HashSet<SmallBasicSet>();
				prepredec.put(s, prepre);
				for (SmallBasicSet p : this.getPredecessors(s)) {
					prepre.addAll(this.getPredecessors(p));
				}
			}
		}
//		System.out.println(".");
		return getLatticeSize(exp,prepredec,new HashSet<SmallBasicSet>(),firstLevel);
	}
	
	private long getLatticeSize(int exp,
			Map<SmallBasicSet, Set<SmallBasicSet>> prepredec,
			Set<SmallBasicSet> lowerLevel, int l) {
		if (l > getMaxLevel()) {
			return pow(exp);
		}
		Set<SmallBasicSet> thisLevel = new HashSet<SmallBasicSet>();
		Set<SmallBasicSet> goodSuccessors = new HashSet<SmallBasicSet>();
		Set<SmallBasicSet> allPredecessors = new HashSet<SmallBasicSet>();
		for (SmallBasicSet s : this.getLevel(l)) {
			if (lowerLevel.containsAll(prepredec.get(s))) {
				thisLevel.add(s);
			}
		}
		Iterator<Set<SmallBasicSet>> it = getSetIterator(thisLevel);
		long res = 0L;
		while(it.hasNext()) {
			Set<SmallBasicSet> alfa = it.next();
			goodSuccessors.clear();
			if (l+1 <= this.getMaxLevel()) {
				Set<SmallBasicSet> levelAbove = getLevel(l+1);
				for (SmallBasicSet t : levelAbove) {
					if (alfa.containsAll(this.getPredecessors(t)))
						goodSuccessors.add(t);
				}
			}
			allPredecessors.clear();
			for (SmallBasicSet s:alfa) {
				allPredecessors.addAll(this.getPredecessors(s));
			}
			int myExp = goodSuccessors.size();
			int lowExp = allPredecessors.size();
			res += pow(exp - lowExp)*getLatticeSize(myExp,prepredec,alfa,l+2);
		}
		return res;
	}

	@Override
	public SortedSet<SmallBasicSet> getLevel(int n) {
		if (n > level.length) return new TreeSet<SmallBasicSet>();
		return level[n-1];
	}

	/**
	 * @pre s is in the poset
	 */
	@Override
	public int getLevel(SmallBasicSet s) {
		return (int) s.size();
	}

	@Override
	public int getMaxLevel() {
		return level.length;
	}

	@Override
	public SortedSet<SmallBasicSet> getPosetElements() {
		return (SortedSet<SmallBasicSet>) successors.keySet();
	}

	/**
	 */
	@Override
	public SimplePosetSize<SmallBasicSet> getPosetFrom(SortedSet<SmallBasicSet> bottom) {
/*		if (bottom.isEmpty()) return new IntervalPoset(0);

		SmallBasicSet running = bottom.first();
		long minLevel = running.size();
		for (SmallBasicSet s : bottom) if (getLevel(s) < minLevel) minLevel = getLevel(s);
		IntervalPoset res = new IntervalPoset(getMaxLevel());

		for (SmallBasicSet r : bottom) {
			res.level[(int) (getLevel(r) - minLevel)].add(r);
			res.successors.put(r, new TreeSet<>)
		}
*/		return null;
	}

	@Override
	public SortedSet<SmallBasicSet> getPredecessors(SmallBasicSet v) {
		return predecessors.get(v);
	}

	private Iterator<Set<SmallBasicSet>> getSetIterator(
			final Set<SmallBasicSet> thisLevel) {
		return new Iterator<Set<SmallBasicSet>>() {

			HashSet<SmallBasicSet> currentSet, nextSet;
			SmallBasicSet[] elements;
			boolean finished;
			{
				currentSet = new HashSet<SmallBasicSet>(); // emptySet
				nextSet = new HashSet<SmallBasicSet>();
				nextSet.addAll(thisLevel); // all elements
				finished = false;
				elements = new SmallBasicSet[thisLevel.size()];
				elements = thisLevel.toArray(elements);
			}
			@Override
			public boolean hasNext() {
				return !finished;
			}

			@Override
			public Set<SmallBasicSet> next() {
				// currentSet follows nextSet
				HashSet<SmallBasicSet> res = new HashSet<SmallBasicSet>(currentSet);
				HashSet<SmallBasicSet> h = currentSet;
				// interchange references
				currentSet = nextSet;
				nextSet = h;
				int i;
				// currentSet is increased to become the follower of nextSet
				for (i=0;i<elements.length;i++) {
					if (currentSet.contains(elements[i])) {
						currentSet.remove(elements[i]);
					}
					else {
						currentSet.add(elements[i]);
						break;
					}
				}
				for (i=0;i<elements.length;i++) {
					if (currentSet.contains(elements[i])) {
						currentSet.remove(elements[i]);
					}
					else {
						currentSet.add(elements[i]);
						break;
					}
				}
				// if current becomes empty the iteration is finished
				if (currentSet.isEmpty()) finished = true;
				return res;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	@Override
	public SortedSet<SmallBasicSet> getSuccessors(SmallBasicSet v) {
		return successors.get(v);
	}
	
	public String toString() {
		SortedSet<SmallBasicSet> set = getPosetElements();
		if (set.isEmpty()) return "[]";
		String res = "[";
		for (SmallBasicSet s : getPosetElements()) {
			res = res + s + ",";
		}
		return res.substring(0,res.length()-1) + "]";
	}
}
