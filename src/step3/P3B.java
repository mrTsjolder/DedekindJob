package step3;

import java.util.SortedSet;
import java.util.TreeSet;

import amfsmall.AntiChain;
import amfsmall.AntiChainSolver;
import amfsmall.SmallBasicSet;

/**
 * P3B is a debugging version of 
 * a helper class for MuNuEta
 * It is essentially an implementation of the order 3 p-coefficient as static method p
 * @author Patrick De Causmaecker
 *
 */
public class P3B {
	
	public static boolean debug = false;

	/**
	 * 
	 * @param pabac
	 * @param pbc
	 * @return no set in pabac is a subset of a set in pbc
	 */
	@SuppressWarnings("unused")
	private static boolean checklargest(AntiChain pabac, AntiChain pbc) {
		for (SmallBasicSet s : pabac)
			for (SmallBasicSet p : pbc)
				if (p.hasAsSubset(s)) {
					return false;
				}
		return true;
	}

	/** Any set in a chain pxy must have a superset occurring in two other chains
	 * or occur in at least one other chain
	 * @param pab
	 * @param pac
	 * @param pbc
	 * @param pabac
	 * @param pabbc
	 * @param pacbc
	 * @return
	 */
	@SuppressWarnings("unused")
	private static boolean check1(AntiChain pab, AntiChain pac, AntiChain pbc,
			AntiChain pabac, AntiChain pabbc, AntiChain pacbc) {
		return singleCheck(pacbc,pac,pbc,pab)
			&& singleCheck(pabbc,pab,pbc,pac)
			&& singleCheck(pabac,pab,pac,pbc);
	}

	private static boolean singleCheck(AntiChain pacbc, AntiChain pac,
			AntiChain pbc, AntiChain pab) {
		for (SmallBasicSet s : pab) {
			if (!pacbc.ge(s) && !pac.contains(s) && !pbc.contains(s)) return false; 
		}
		return true;
	}

	private static boolean checkunique(SortedSet<AntiChain> ina,
			SortedSet<AntiChain> inb, SortedSet<AntiChain> inc) {
		for (AntiChain ca : ina) {
			if (inb.contains(ca)) return false;
			if (inc.contains(ca)) return false;
		}
		for (AntiChain cb : inb) {
			if (ina.contains(cb)) return false;
			if (inc.contains(cb)) return false;
		}
		for (AntiChain cc : inc) {
			if (inb.contains(cc)) return false;
			if (ina.contains(cc)) return false;
		}
		
		return true;
	}

	/**
	 * compute the third order p-coefficient, that is the number of solutions to
	 * a v b = ab, a v c = ac, b v c = bc, a.meet(b).meet(c) == abc
	 * @pre consistency check | mne.isNonZero()
	 *
	 * @param abc
	 * @param ab
	 * @param ac
	 * @param bc
	 * @return the fourth order p-coefficient
	 */
	public static long p(AntiChain abc, MuNuEta mne) {
		// abreviations for convenience
		AntiChain ab = mne.mu;
		AntiChain ac = mne.nu;
		AntiChain bc = mne.eta;

		// disregard sets in abc for the time being (poset structure)
		AntiChain pab = new AntiChain(ab); pab.removeAll(abc);
		AntiChain pac = new AntiChain(ac); pac.removeAll(abc);
		AntiChain pbc = new AntiChain(bc); pbc.removeAll(abc);
		
		// collect the sets occurring in each of pxy
		AntiChain pabacbc = new AntiChain(pab);
		pabacbc.retainAll(pac);
		pabacbc.retainAll(pbc);
		// and remove them for the time being
		pab.removeAll(pabacbc);
		pac.removeAll(pabacbc);
		pbc.removeAll(pabacbc);
		
		// Any set in a chain pxy must have a superset occurring in two other chains
		// or occur in at least one other chain
		// Collect the supersets
		AntiChain pabac = new AntiChain(pab);
		pabac.retainAll(pac);
		AntiChain pabbc = new AntiChain(pab);
		pabbc.retainAll(pbc);
		AntiChain pacbc = new AntiChain(pac);
		pacbc.retainAll(pbc);
//		if (!check1(pab,pac,pbc,pabac,pabbc,pacbc)) return 0L; check is redundant if preconditions are satisfied
		
		// this determines the first sets in a,b and c
		AntiChain a = pabac.join(abc);
		AntiChain b = pabbc.join(abc);
		AntiChain c = pacbc.join(abc);
		if (!a.meet(b).meet(c).equals(abc)) return 0;
		
		// find out what is left in each of the pxy
		AntiChain rab = new AntiChain(pab);
		rab.removeAll(a.join(b));
		AntiChain rac = new AntiChain(pac);
		rac.removeAll(a.join(c));
		AntiChain rbc = new AntiChain(pbc);
		rbc.removeAll(b.join(c));
		
//		return count(a,b,c,rab,rac,rbc,pabacbc,abc); // this count is somewhat faster for n <= 4
		
		// find the connected components
		SortedSet<AntiChain> cab = AntiChainSolver.getConnectedComponents(abc, rab);
		SortedSet<AntiChain> cac = AntiChainSolver.getConnectedComponents(abc, rac);
		SortedSet<AntiChain> cbc = AntiChainSolver.getConnectedComponents(abc, rbc);
		
		
		// in case of no doubt, put them where they belong
		SortedSet<AntiChain> ina = new TreeSet<AntiChain>();
		SortedSet<AntiChain> inb = new TreeSet<AntiChain>();
		SortedSet<AntiChain> inc = new TreeSet<AntiChain>();
		for (AntiChain rb : cab)
			for (AntiChain rc : cac) {
				if (!rb.meet(rc).le(abc)) {
					inc.add(rc);
					inb.add(rb);
				}
			}
		for (AntiChain ra : cab)
			for (AntiChain rc : cbc) {
				if (!ra.meet(rc).le(abc)) {
					inc.add(rc);
					ina.add(ra);
				}
			}
		for (AntiChain ra : cac)
			for (AntiChain rb : cbc) {
				if (!ra.meet(rb).le(abc)) {
					ina.add(ra);
					inb.add(rb);
				}
			}
		if (!checkunique(ina,inb,inc)) return 0L;
		
		for (AntiChain x : ina) a = a.join(x);
		for (AntiChain x : inb) b = b.join(x);
		for (AntiChain x : inc) c = c.join(x);
		
		cab.removeAll(ina);cab.removeAll(inb);
		cac.removeAll(ina);cac.removeAll(inc);
		cbc.removeAll(inb);cbc.removeAll(inc);

		return count(a,b,c,cab,cac,cbc,AntiChainSolver.getConnectedComponents(abc, pabacbc),abc);
		
	}

	/**
	 * count the number of ways to add components from cxy to x and y while respectiong a.meet(b).meet(c).equals(abc)
	 * @param a
	 * @param b
	 * @param c
	 * @param cab components for a or b
	 * @param cac
	 * @param cbc
	 * @param cabacbc components that must occur twice
	 * @param abc
	 * @return
	 */
	public static long count(AntiChain a, AntiChain b, AntiChain c,
			SortedSet<AntiChain> cab, SortedSet<AntiChain> cac,
			SortedSet<AntiChain> cbc, SortedSet<AntiChain> cabacbc,
			AntiChain abc) {
		if (!a.meet(b).meet(c).equals(abc)) return 0L; // impossible
		if (!cabacbc.isEmpty()) {
			AntiChain ss = cabacbc.first();
			TreeSet<AntiChain> cabacbcp = new TreeSet<AntiChain>(cabacbc);
			cabacbcp.remove(ss);
			boolean ina = !ss.meet(a).le(abc); 
			boolean inb = !ss.meet(b).le(abc); 
			boolean inc = !ss.meet(c).le(abc);
			if (ina && inb && inc) return 0L;
			if (ina && inb) return count(a.join(ss),b.join(ss),c,cab,cac,cbc,cabacbcp,abc);
			if (ina && inc) return count(a.join(ss),b,c.join(ss),cab,cac,cbc,cabacbcp,abc);
			if (inb && inc) return count(a,b.join(ss),c.join(ss),cab,cac,cbc,cabacbcp,abc);
			if (ina) return count(a.join(ss),b.join(ss),c,cab,cac,cbc,cabacbcp,abc) 
					+ count(a.join(ss),b,c.join(ss),cab,cac,cbc,cabacbcp,abc); 
			if (inb) return count(a.join(ss),b.join(ss),c,cab,cac,cbc,cabacbcp,abc) 
					+ count(a,b.join(ss),c.join(ss),cab,cac,cbc,cabacbcp,abc); 
			if (inc) return count(a.join(ss),b,c.join(ss),cab,cac,cbc,cabacbcp,abc) 
					+ count(a,b.join(ss),c.join(ss),cab,cac,cbc,cabacbcp,abc); 
			return count(a.join(ss),b.join(ss),c,cab,cac,cbc,cabacbcp,abc) 
					+ count(a.join(ss),b,c.join(ss),cab,cac,cbc,cabacbcp,abc) 
					+ count(a,b.join(ss),c.join(ss),cab,cac,cbc,cabacbcp,abc);
		}
		if (!cab.isEmpty()) {
			AntiChain ss = cab.first();
			TreeSet<AntiChain> cabp = new TreeSet<AntiChain>(cab);
			cabp.remove(ss);
			if (!ss.meet(a).le(abc)) return count(a.join(ss),b,c,cabp,cac,cbc,cabacbc,abc);
			if (!ss.meet(b).le(abc)) return count(a,b.join(ss),c,cabp,cac,cbc,cabacbc,abc);
			return count(a.join(ss),b,c,cabp,cac,cbc,cabacbc,abc) + count(a,b.join(ss),c,cabp,cac,cbc,cabacbc,abc);
		} 
		if (!cac.isEmpty()) {
			AntiChain ss = cac.first();
			TreeSet<AntiChain> cacp = new TreeSet<AntiChain>(cac);
			cacp.remove(ss);
			if (!ss.meet(a).le(abc)) return count(a.join(ss),b,c,cab,cacp,cbc,cabacbc,abc);
			if (!ss.meet(c).le(abc)) return count(a,b,c.join(ss),cab,cacp,cbc,cabacbc,abc);
			return count(a.join(ss),b,c,cab,cacp,cbc,cabacbc,abc) + count(a,b,c.join(ss),cab,cacp,cbc,cabacbc,abc);
		} 
		if (!cbc.isEmpty()) {
			AntiChain ss = cbc.first();
			TreeSet<AntiChain> cbcp = new TreeSet<AntiChain>(cbc);
			cbcp.remove(ss);
			if (!ss.meet(b).le(abc)) return count(a,b.join(ss),c,cab,cac,cbcp,cabacbc,abc);
			if (!ss.meet(c).le(abc)) return count(a,b,c.join(ss),cab,cac,cbcp,cabacbc,abc);
			return count(a,b.join(ss),c,cab,cac,cbcp,cabacbc,abc) + count(a,b,c.join(ss),cab,cac,cbcp,cabacbc,abc);
		} 
		return 1L;
	}
}
