package kdtree;

public class HPoint {

	protected int[] coord;

	protected HPoint(final int n) {
		coord = new int[n];
	}

	public HPoint(final int[] x) {

		coord = new int[x.length];
		System.arraycopy(x, 0, coord, 0, x.length);
	}

	@Override
	protected Object clone() {

		return new HPoint(coord);
	}

	protected boolean equals(final HPoint p) {

		// seems faster than java.util.Arrays.equals(), which is not
		// currently supported by Matlab anyway
		for (int i = 0; i < coord.length; ++i) {
			if (coord[i] != p.coord[i]) {
				return false;
			}
		}

		return true;
	}

	protected static int sqrdist(final HPoint x, final HPoint y) {

		int dist = 0;

		for (int i = 0; i < x.coord.length; ++i) {
			final int diff = x.coord[i] - y.coord[i];
			dist += diff * diff;
		}

		return dist;

	}

	protected static double eucdist(final HPoint x, final HPoint y) {
		return Math.sqrt(sqrdist(x, y));
	}

	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < coord.length; ++i) {
			s = s + coord[i] + " ";
		}
		return s;
	}

}
