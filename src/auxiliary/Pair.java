package auxiliary;

public class Pair<T1, T2>  {
	
	public final T1 fst;
	public final T2 snd;

	public Pair(T1 a,T2 b) {
		fst = a;
		snd = b;
	}
	
	@Override
	public int hashCode() {
		return fst.hashCode() << 1 + snd.hashCode();
	}
	
	public String toString() {
		return "(" + fst + ", " + snd +")";
	}

}
