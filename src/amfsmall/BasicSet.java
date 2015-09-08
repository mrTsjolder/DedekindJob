package amfsmall;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Sets of integers to be used as the base elements in the space of antimonotonic functions
 * immutable
 * @author u0003471
 *
 */
public class BasicSet implements Iterable<Integer> {

	private HashSet<Integer> theSet;
	
	private static BasicSet theEmptySet = new BasicSet();
	
	public static BasicSet emptySet() {
		return theEmptySet;
	}
	
	private BasicSet() {
		theSet = new HashSet<Integer>();
	}

	/**
	 * pick one element at random from this set
	 * throws an NoSuchElementException if the set is empty
	 * @param sup
	 */
	public int getOneFrom() throws NoSuchElementException {
		if (isEmpty()) throw new NoSuchElementException();
		Integer[] array = new Integer[theSet.size()];
		theSet.toArray(array);
		double index = (float) Math.random()*theSet.size();
		return array[(int) Math.floor(index)];
	}
	
	/**
	 * checks whether this set has any elements
	 * @return the set has no elements
	 */
	private boolean isEmpty() {
		return theSet.isEmpty();
	}

	/**
	 * construct a basic set with one extra element
	 * @param x the extra element
	 * return this U {x}
	 */
	public BasicSet add(int x) {
		BasicSet res = new BasicSet();
		res.theSet = new HashSet<Integer>(theSet);
		res.theSet.add(x);
		return res;
	}
	
	/**
	 * return a string representation for display only
	 * return the set described in a string
	 */
	public String toString() {
		return theSet.toString();
	}

	@Override
	public Iterator<Integer> iterator() {
		// TODO Auto-generated method stub
		return new Iterator<Integer>() {

			Iterator<Integer> it = theSet.iterator();
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Integer next() {
				return it.next();
			}

			@Override
			public void remove() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	/**
	 * elementary combinatorics
	 * @param i
	 * @param j
	 * @return
	 */
	public static long combinations(int i, int j) {
		long res = 1;
		int t = i - j;
		while (i > j) {res *= i;i--;}
		while (t > 1) {res /= t;t--;}
		return res;
	}

	public BasicSet remove(int f) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.remove(f);
		return res;
	}

	public boolean contains(int f) {
		return theSet.contains(f);
	}

}
