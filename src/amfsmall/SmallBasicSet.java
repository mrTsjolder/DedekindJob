package amfsmall;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Sets of integers to be used as the base elements in the space of antimonotonic functions
 * Small sets only contain integers between 1 and MAXELEMENT
 * It is a small world version of the unlimited world BasicSet
 * This allows to speed up by a factor of 10 approximately
 * Combined goodies (fast and unlimited) could be obtained through the use of BitSet.
 * immutable
 * @author u0003471
 *
 */
public class SmallBasicSet implements Iterable<Integer>, Comparable<SmallBasicSet> {

	private long theSet; // set representaion

	private static long[] bits;

	private static BasicSet[] sets;
	
	private static boolean[] compactSets; // intervals starting in 1
	
	public final static long MAXELEMENT = 12;
	
	static {
		bits = new long[(int) MAXELEMENT];
		sets = new BasicSet[1 << MAXELEMENT];
		compactSets = new boolean[1 << MAXELEMENT];
		
		int p = 1;
		for (int i=0;i<MAXELEMENT;i++) {
			bits[i] = p;
			p <<= 1;
		}
		long m2 = 1;
		BasicSet aSet = BasicSet.emptySet();
		for (int i=0;i<(1<<MAXELEMENT);i++) {
			sets[i] = aSet;
			if (i == m2 - 1) {
				compactSets[i] = true;
				m2 <<= 1;
			}
			else compactSets[i] = false;
			aSet = increaseSet(aSet,1);
		}
	}
	
	private static BasicSet increaseSet(BasicSet aSet,int e) {
		if (aSet.contains(e)) {
			return increaseSet(aSet.remove(e),e+1);
		}
		else return aSet.add(e);
	}
	
	private static SmallBasicSet theEmptySet = new SmallBasicSet();
	
	public static SmallBasicSet emptySet() {
		return theEmptySet;
	}
	
	private SmallBasicSet() {
		theSet = 0L;
	}
	/**
	 * construct a set from an array
	 * @param is
	 */
	public SmallBasicSet(int[] is) {
		theSet = 0L;		
		for (Integer i : is) {
				theSet |= getBit(i);
		}
	}
	
	public SmallBasicSet(long l) {
		theSet = l;
	}

	private static long getBit(int i) {
		return bits[i-1];
	}

	/**
	 * split the basic set in n equally sized parts
	 * @param n
	 * @return an array of n disjoint basic sets, the n parts, the union is this
	 */
	public SmallBasicSet[] split(int n) {
		SmallBasicSet[] res = new SmallBasicSet[n];
		int p = 0;
		for (p=0;p<n;p++) res[p] = new SmallBasicSet();
		p = 0;
		for (int i : getSet(theSet)) {
			res[p] = res[p].add(i);
			p = (p + 1) % n;
		}
		return res;
	}

	private BasicSet getSet(long theSet2) {
		return sets[(int) theSet2];
	}

	/**
	 * the number of subsets of this basic set
	 * @return 2^size()
	 */
	public long numberOFSubsets() {
		long res = 1L;
		return res << size();
	}

	/**
	 * the number of elements in this set
	 * @param b
	 * @return size
	 */
	public long size() {
		return Long.bitCount(theSet);
	}
	
	/**
	 * subset relationship
	 * @param b
	 * @return is b a subset of this set
	 */
	public boolean hasAsSubset(SmallBasicSet b) {
		return (theSet | b.theSet) == theSet;
	}

	/**
	 * checks whether this set has any elements
	 * @return the set has no elements
	 */
	public boolean isEmpty() {
		return theSet == 0;
	}
	/**
	 * Is the set b equal to this set
	 * 
	 * @param b
	 * @return b contains the all elements of this and this contains all elements of b
	 */
	public boolean equals(SmallBasicSet b) {
		return theSet == b.theSet;	
	}

	/**
	 * compute the difference of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this \ basicSet
	 */
	public SmallBasicSet minus(SmallBasicSet basicSet) {
		return new SmallBasicSet(theSet & (~basicSet.theSet));
	}

	/**
	 * compute the difference of this BasicSet and the set containing x
	 * as a new basic set
	 * @param x
	 * @return this \ {x}
	 */
	public SmallBasicSet minus(Integer x) {
		return new SmallBasicSet(theSet & (~getBit(x)));
	}

	/**
	 * compute the union of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this U basicSet
	 */
	public SmallBasicSet union(SmallBasicSet basicSet) {
		return new SmallBasicSet(theSet | basicSet.theSet);
	}

	/**
	 * compute the intersection of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this intersection basicSet
	 */
	public SmallBasicSet intersection(SmallBasicSet basicSet) {
		return new SmallBasicSet(theSet & basicSet.theSet);
	}

	/**
	 * construct a basic set with one extra element
	 * @param x the extra element
	 * return this U {x}
	 */
	public SmallBasicSet add(int x) {
		return new SmallBasicSet(theSet | getBit(x));
	}
	
	/**
	 * construct a basic set with a number of extra elements
	 * @param x an array of integer
	 * return this U {i|i in x}
	 */
	public SmallBasicSet add(int[] x) {
		long res = theSet;
		for (Integer i : x) res = res |getBit(i);
		return new SmallBasicSet(res);
	}
	
	/**
	 * return a string representation for display only
	 * return the set described in a string
	 */
	public String toString() {
		if (isEmpty()) return "0";
		String res = "";
		for (int i : this) res += toCharacter(i); 
		return res;
	}

	/**
	 * translate an accepatable character to an integer
	 * @param c
	 * @return if c in '1','2',...,'f' return 1-15 else return 0
	 */
	protected static int toIntegerElement(char c) {
		switch (c) {
		case '1': return 1;
		case '2': return 2;
		case '3': return 3;
		case '4': return 4;
		case '5': return 5;
		case '6': return 6;
		case '7': return 7;
		case '8': return 8;
		case '9': return 9;
		case 'a': return 10;
		case 'b': return 11;
		case 'c': return 12;
		case 'd': return 13;
		case 'e': return 14;
		case 'f': return 15;
		}
		return 0;
	}

	/**
	 * accept only characters in the range '1'-'9'and 'a' tp 'f'  (hex)
	 * limited by MAXELEMENT
	 * @param c
	 * @return o < toIntegerElement(c) <= MAXELEMENT
	 */
	public static boolean isAcceptableCharElement(char c) {
		int v = toIntegerElement(c);
		return (0 < v && v <= SmallBasicSet.MAXELEMENT);
	}


	protected char toCharacter(int i) {
		switch (i) {
		case 1 : return '1';
		case 2 : return '2';
		case 3 : return '3';
		case 4 : return '4';
		case 5 : return '5';
		case 6 : return '6';
		case 7 : return '7';
		case 8 : return '8';
		case 9 : return '9';
		case 10 : return 'a';
		case 11 : return 'b';
		case 12 : return 'c';
		case 13 : return 'd';
		case 14 : return 'e';
		case 15 : return 'f';		
		}
		return '?';
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {

			long currentBit = 1;
			int current = 1;
			{
				while ((currentBit & theSet) == 0 && current <= MAXELEMENT) {
					currentBit <<= 1;
					current++;
				}
			}
			@Override
			public boolean hasNext() {
				return current <= MAXELEMENT;
			}

			@Override
			public Integer next() {
				int prv = current;
				currentBit <<= 1;
				current++;				
				while ((currentBit & theSet) == 0 && current <= MAXELEMENT) {
					currentBit <<= 1;
					current++;
				}
				return prv;
			}

			@Override
			public void remove() {
//				throw new NotImplementedException();
			}
			
		};
//		return getSet().iterator();

	}

	/**
	 * elementary combinatorics
	 * @param i
	 * @param j
	 * @return
	 */
	public static long combinations(int i, int j) {
		if (i < j) return 0;
		long res = 1;
		int t = i - j;
		while (i > j) {res *= i;i--;}
		while (t > 1) {res /= t;t--;}
		return res;
	}

	public SmallBasicSet remove(int f) {
		return new SmallBasicSet(theSet & (~getBit(f)));
	}

	public boolean contains(int f) {
		return (theSet | getBit(f)) == theSet;
	}

	public AntiChain immediateSubSets() {
		AntiChain res = new AntiChain();
		for (int x:this) res.add(this.minus(x));
		return res;
	}

	@Override
	public int compareTo(SmallBasicSet o) {
		return (int) (this.theSet - o.theSet);
	}
	
	public int hashCode() {
		return (int) theSet % Integer.MAX_VALUE;
	}
	
	/**
	 * checks whether this is set is an interval with emptyset as the lower bound, inclusive
	 * @return
	 */
	public boolean isCompact() {
		return compactSets[(int) theSet];

	}
	
	/**
	 * @return the smallest element in the set. 0 if the set is empty
	 * 
	 */
	public int minimum() {
		if (isEmpty()) return 0;
		int m = 1;
		while (!contains(m)) m++;
		return m;
		}
	
	/**
	 * @return the largest element in the set. 0 if the set is empty
	 * 
	 */
	public int maximum() {
		if (isEmpty()) return 0;
		int m = (int) MAXELEMENT;
		while (!contains(m)) m--;
		return m;
		}

	public static SmallBasicSet universe() {
		return new SmallBasicSet((1 << MAXELEMENT) - 1);
	}

	public static SmallBasicSet universe(int n) {
		return new SmallBasicSet((1 << n) - 1);
	}

	/**
	 * the list of all subsets of this set
	 * @return TreeSet with all subsets
	 */
	public TreeSet<SmallBasicSet> getSubSets() {
		TreeSet<SmallBasicSet> res = new TreeSet<SmallBasicSet>();
		res.add(this);
		if (this.isEmpty()) return res;
		for (int x : this) {
			for (SmallBasicSet s : this.minus(x).getSubSets()) {
				res.add(s);
				res.add(s.add(x));
			}
		}
		return res;
	}

	/**
	 * map this set according to the transformation in table: i -> table[i] + 1
	 * @pre table.length - 1 < s.maximum()
	 * @param table
	 * @return
	 */
	public SmallBasicSet map(int[] table) {
		SmallBasicSet res = SmallBasicSet.emptySet();
		for (int i:this)
			res = res.add(table[i] + 1);
		return res;
	}

	/**
	 * convert an integer to a set
	 * @param x
	 * @return
	 */
	public static SmallBasicSet decode(int x) {
		return new SmallBasicSet(x);
	}

	public static SmallBasicSet getAcceptableElements() {
		return universe((int) MAXELEMENT);
	}

	}

