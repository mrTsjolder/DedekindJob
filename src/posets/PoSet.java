/**
 * 
 */
package posets;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import amfsmall.DoubleNodeCreationError;
import amfsmall.PoSetIncompatibleSuccession;

/**
 * 
 * general PoSet definition for uniform testing
 * It should accept pattern defined in PoSetTest for the type T = String
 * @author patrickdecausmaecker
 *
 */
public interface PoSet<T extends Comparable<T>> extends SortedSet<T> {


	/**
	 * Prior to any topological sort, initialise all elements
	 * set labels to one,...
	 * 
	 * return is false if the poset is empty, otherwise it is true
	 * 
	 */
	public boolean initialise();

	/**
	 * labeling on a topological sort orderlist
	 */
	public void uniqueLabels();
	
	/**
	 * toString should produce a representation of the PoSet in the form label[labelofsuccessor,..]...
	 * the values of the elements can be obtained through the legenda methods
	 * @return
	 */
	
	public String toString();
	/**
	 * label, value pair for each element sorted on label
	 * @return
	 */
	
	public String legenda();
	/**
	 * return the label corresponding to this value
	 * labels are strictly positive integers, -1 indicates non existence of the value
	 * @param label
	 * @return
	 */
	
	public int legenda(T value);
	/**
	 * the set of values corresponding to this label
	 * @return
	 */
	public Set<T> legenda(int label);
	
	/**
	 * 
	 * remove the value v, add the value i
	 * @param v
	 * @param i
	 * @throws DoubleNodeCreationError 
	 */
	public void replaceValue(T v, T i) throws DoubleNodeCreationError;
	/**
	 * permute the values in the nodes according to permutation perm
	 * used to test unambiguity of the labeling procedure
	 * works best if perm.size() == this.size()
	 * @param perm
	 */
	public void permuteValues(Permutation perm);
	/**
	 * label on increasing values
	 */
	public void labelOnValues();

	/**
	 * possibility to log an operation
	 * @return
	 */
	public String getLog();
	
	/**
	 * get successors and predecessors of a value
	 * @param v
	 * @return
	 */
	public SortedSet<T> getSuccessors(T v);
	public SortedSet<T> getPredessors(T v);
	/**
	 * get the set of values smaller(larger) according to PoSet inequality than the current value
	 * @param v
	 * @return
	 */
	public SortedSet<T> getBefore(T v);
	public SortedSet<T> getAfter(T v);
	
	public List<Set<T>> getComponents();
	
	/**
	 * set an immediate succession between first and last
	 * @throws PoSetIncompatibleSuccession if the attempted succession is in conflict with existing wone
	 * case 1 if first < last already in the PoSet (immediate succession is impossible or does already exist)
	 * case 2 if first > last in the PoSet
	 * If either first or last is not yet in the PoSet, it will be created
	 * @param first
	 * @param last
	 */
	public void addSuccession(T first,T last) throws PoSetIncompatibleSuccession;
	
	/**
	 * compute the size of the minimal distributive lattice containing this poset
	 * @return
	 */
	public BigInteger getLatticeSize();
}
