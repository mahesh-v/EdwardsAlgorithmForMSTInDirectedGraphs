import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


/**
 * Long Project 2
 * Edward's Algorithm for finding the MST and calculating its weight in a directed graph
 * 
 * @author Darshan and Mahesh
 *
 */
public class EdwardsAlgo {
	private static long mst_wt;

	public static void main(String[] args) throws IOException {
		Graph g = Graph.readGraph(new Scanner(new File("test/lp2-m.txt")), true);
		Timer t = new Timer();
		List<Edge> spanningTreeEdges = findMST(g, g.verts.get(1));
		t.end();
		System.out.println(mst_wt);
		Collections.sort(spanningTreeEdges, new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2) {
				return e1.To.name - e2.To.name;
			}
		});
		if(spanningTreeEdges.size() < 50){
			for (Edge edge : spanningTreeEdges) {
				System.out.println(edge);
			}
		}
		System.out.println(t);
	}

	/**
	 * Returns a list of Edges that form the minimum spanning tree
	 * 
	 * @param g Input graph
	 * @param root Root of the MST
	 * @return List<Edge> that represents the MST
	 */
	public static List<Edge> findMST(Graph g, Vertex root) {
		//initialize
		mst_wt = 0;
		for (Vertex vertex : g) {
			if(vertex.active){
				vertex.zeroEdges.clear();
				vertex.parent = null;
				vertex.parentEdge = null;
				vertex.seen =  false;
				vertex.fromC = null;
				vertex.toC = null;
			}
		}
		populateZeroWeightEdges(g, root);
		List<Edge> spanningTreeEdges = new ArrayList<Edge>();
		//BFS
		LinkedList<Vertex> queue = new LinkedList<Vertex>();
		queue.add(root);
		while(!queue.isEmpty()){
			Vertex v = queue.removeFirst();
			v.seen = true;
			for (Edge e : v.zeroEdges) {
				if(!e.To.active)
					continue;
				e.isInMST = true;
				mst_wt+=e.Weight;
				spanningTreeEdges.add(e);
				Vertex u = e.To;
				if(!u.seen && u.active){
					u.parentEdge = e;
					u.parent=v;
					queue.add(u);
				}
			}
		}
		Vertex x = null;//find unseen vertex
		for (Vertex v : g) {
			if(!v.seen && v.active){
				x = v;
				break;
			}
		}
		if(x==null)
			return spanningTreeEdges;//all active vertices are reachable
		
		//Traverse backward from x to find the zero weight cycle
		Vertex current = x;
		current.seen = true;
		Vertex cycleStart = null;
		do{
			current = current.revZeroEdge.From;
			if(current.seen){
				cycleStart = current;
				break;
			}
			current.seen = true;
		} while(true);
		Vertex C = new Vertex(++(g.numNodes));
		do{//mark C as super vertex for all verts in cycle
			current.superVertex = C;
			current.active = false;//make it inactive for the future
			current = current.revZeroEdge.From;
		} while(current != cycleStart);
		
		g.verts.add(C);
		do{//for all vertices in cycle, add edges that are not within itself
			//from current to vertices outside C
			for (Edge e : current.Adj) {
				if(e.To.superVertex != C && e.To.active){
					if(e.To.fromC == null)//no edge from C to e.To
						e.To.fromC = addNewEdge(C, e.To, e);
					else if(e.To.fromC.Weight > e.Weight){ //edge exists
						e.To.fromC.Weight = e.Weight;
						e.To.fromC.oldEdge = e;
					}
				}
			}
			//from vertices outside C to current
			for (Edge e : current.revAdj) {
				if(e.From.superVertex != C && e.From.active){
					if(e.From.toC == null)//no edge from e.From to C
						e.From.toC = addNewEdge(e.From, C, e);
					else if(e.From.toC.Weight > e.Weight){ //edge exists
						e.From.toC.Weight = e.Weight;
						e.From.toC.oldEdge = e;
					}
				}
			}
			current = current.revZeroEdge.From;
		}while(current != cycleStart);
		//call recursively with new graph
		List<Edge> sTreeEdges = findMST(g, root);
		
		//expand the cycle in 3 steps:
		//Step 1, remove out-going edges from C and replace with outgoing edges from cycle.
		for (Edge zeroEdge : C.zeroEdges) {
			if(zeroEdge.isInMST){
				zeroEdge.isInMST = false;
				mst_wt-=zeroEdge.Weight;
				sTreeEdges.remove(zeroEdge);
				//replace with old edge
				zeroEdge.oldEdge.isInMST = true;
				mst_wt+=zeroEdge.oldEdge.Weight;
				sTreeEdges.add(zeroEdge.oldEdge);
				zeroEdge.To.parentEdge = zeroEdge.oldEdge;
				zeroEdge.To.parent = zeroEdge.oldEdge.From;
			}
		}
		//Step 2, remove incoming edge into C and replace with that which was incoming
		Edge incoming = C.parentEdge;//findEdgeBetweenVertices(C.parent, C);
		incoming.isInMST = false;
		mst_wt-=incoming.Weight;
		sTreeEdges.remove(incoming);
		//replace with old edge
		incoming.oldEdge.isInMST = true;
		mst_wt+=incoming.oldEdge.Weight;
		sTreeEdges.add(incoming.oldEdge);
		Vertex cycle_start = incoming.oldEdge.To;
		cycle_start.parentEdge = incoming.oldEdge;
		cycle_start.parent = C.parent;
		
		//Step 3, add all the edges that were shrunk back to MST
		current = cycle_start;
		do{ // back track through reverse zero edges in cycle and add all but 1 to MST
			Edge edge = current.revZeroEdge;
			current.active = true;
			if(edge.To != cycle_start){ //do not add edge only in this case
				edge.isInMST = true;
				mst_wt+=edge.Weight;
				edge.To.parentEdge = edge;
				edge.To.parent = edge.From;
				sTreeEdges.add(edge);
			}
			current = edge.From;
		}while (current!=cycle_start);
		
		//return MST with cycle expanded
		C.active=false;
		return sTreeEdges;
	}

	private static Edge addNewEdge(Vertex from, Vertex to, Edge oldEdge) {
		Edge e = new Edge(from, to, oldEdge.Weight);
		e.oldEdge = oldEdge;
		from.Adj.add(e);
		to.revAdj.add(e);
		return e;
	}

	private static void populateZeroWeightEdges(Graph g, Vertex root) {
		for (Vertex v : g) {
			if(v.active && v!=root){
				v.revZeroEdge = null;
				int min = Integer.MAX_VALUE;
				for (Edge e : v.revAdj) {
					if(e.From.active && e.Weight < min){
						min = e.Weight;
					}
				}
				for (Edge e : v.revAdj) {
					if(e.From.active && (e.Weight - min) == 0){
						v.revZeroEdge = e;
						e.From.zeroEdges.add(e);
						break;
					}
				}
				if(v.revZeroEdge == null)
					System.out.println("This should be an error");
			}
		}
	}

}
