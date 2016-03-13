import java.util.Comparator;


public class EdgeToComparator implements Comparator<Edge> {

	@Override
	public int compare(Edge e1, Edge e2) {
		
		return e1.To.name - e2.To.name;
	}

}
