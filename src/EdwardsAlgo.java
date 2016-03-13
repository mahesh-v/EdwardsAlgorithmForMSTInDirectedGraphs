import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class EdwardsAlgo {

	public static void main(String[] args) throws FileNotFoundException {
		Graph g = Graph.readGraph(new Scanner(new File("test/2-lp2.txt")), true);
		List<Edge> spanningTreeEdges = findMST(g, g.verts.get(1));
		int sum = 0;
		for (Edge edge : spanningTreeEdges) {
			sum+=edge.Weight;
		}
		System.out.println(sum);
//		Collections.sort(spanningTreeEdges, new EdgeToComparator());
//		System.out.println(spanningTreeEdges);
	}

	public static List<Edge> findMST(Graph g, Vertex root) {
		for (Vertex vertex : g) {
			vertex.zeroEdges.clear();
		}
		reduceIncommingEdgeWeights(g, root);
		List<Edge> spanningTreeEdges = new ArrayList<Edge>();
		LinkedList<Vertex> queue = new LinkedList<Vertex>();
		queue.add(root);
		while(!queue.isEmpty()){
			Vertex v = queue.removeFirst();
			v.seen = true;
			for (Edge e : v.zeroEdges) {
//				if(!e.active)
//					continue;
//			if(v.zeroEdges == null)
//				continue;
				spanningTreeEdges.add(e);
				Vertex u = e.To;
				u.parent=e.From;
				if(!u.seen && u.active){
					queue.add(u);
				}
			}
		}
		Vertex x = null;
		for (Vertex v : g) {
			if(!v.seen && v.active){
				x = v;
			}
			v.seen = false;//for future use
		}
		if(x==null)
			return spanningTreeEdges;//cycles are expanded after returning
		
		//now we need to go backward from x to find the zero weight cycle
		Vertex current = x;
		current.seen = true;
		Vertex cycleStart = null;
		do{
			current = current.revZeroEdges.From;
			if(current.seen){
				cycleStart = current;
				break;
			}
			current.seen = true;
		} while(true);
		x.seen = false;
		while(x!=cycleStart){ //reset all edges up to that cycle to false for later use
			x = x.revZeroEdges.From;
			x.seen = false;
		}
		Vertex C = new Vertex(++(g.numNodes));
		do{//add all vertices in cycle to C.cycleVerts
			current.seen = false;//unrelated. for future use
			
			C.cycleVerts.add(current);
			current.active = false;//make it inactive for the future
			current = current.revZeroEdges.From;
		} while(current != cycleStart);
		
		g.verts.add(C);
		do{//add edges that are not within C.cycleVerts
			for (Edge e : current.Adj) {
				if(!C.cycleVerts.contains(e.To)){
					addNewEdge(g, C, e.To, e);
				}
			}
			for (Edge e : current.revAdj) {
				if(!C.cycleVerts.contains(e.From)){
					addNewEdge(g, e.From, C, e);
				}
			}
			current = current.revZeroEdges.From;
		}while(current != cycleStart);
		//call recursively with new graph
		List<Edge> sTreeEdges = findMST(g, root);
		//expand the cycle
		//Step 1, remove out-going edges from C and replace with outgoing edges from cycle.
		for (Edge edge : C.zeroEdges) {
			sTreeEdges.remove(edge);
			sTreeEdges.add(edge.oldEdge);
			edge.To.parent = edge.oldEdge.From;
//			Vertex cycle_vert = edge.oldEdge.From;
//			for (Edge ed : cycle_vert.zeroEdges) {//it would have been in zeroEdges list
////
//				if(ed.To.equals(edge.To)){
//					ed.To.parent = cycle_vert;
////					sTreeEdges.add(ed);
//					break;
//				}
//			}
		}
		//Step 2, remove incoming edge into C and replace with that which was incoming
		Edge incoming = findEdgeBetweenVertices(C.parent, C);
		sTreeEdges.remove(incoming);
		sTreeEdges.add(incoming.oldEdge);
		Vertex cycle_vert = incoming.oldEdge.To;
		cycle_vert.parent = C.parent;
//		for (Edge ed : cycle_vert.revAdj) {
//			if(ed.From.equals(C.parent)){
//				sTreeEdges.add(ed);
//				break;
//			}
//		}
		
		//Step 3, add all the edges that were shrunk back to MST
		current = cycle_vert;
		do{
			Edge ed = current.revZeroEdges;
			if(current != cycle_vert) {
				current.parent = ed.From;
			}
			if(!ed.To.equals(cycle_vert)) //do not complete the cycle only in this case
				sTreeEdges.add(ed);
			current = ed.From;
		}while (current!=cycle_vert);
		
//		g.verts.remove(C);
//		g.numNodes--;
		C.active=false;
		return sTreeEdges;
	}

	private static void addNewEdge(Graph g, Vertex from, Vertex to, Edge ed) {
		for (Edge edge : from.Adj) {
			if(edge.auxEdge && edge.From.equals(from) && edge.To.equals(to)){
				if(edge.Weight > ed.Weight){
					edge.Weight = ed.Weight;//simply replace the weight with the lesser weight
					edge.oldEdge = ed;
				}
				return;
			}
		}
		Edge e = new Edge(from, to, ed.Weight);
//		ed.active = false;
		e.auxEdge = true;
		e.oldEdge = ed;
		from.Adj.add(e);
		to.revAdj.add(e);
	}

	private static void reduceIncommingEdgeWeights(Graph g, Vertex root) {
		for (Vertex v : g) {
			if(v!=root && v.active){
				v.revZeroEdges = null;
				int min = Integer.MAX_VALUE;
				for (Edge e : v.revAdj) {
					if(e.From.active && e.Weight < min){
						min = e.Weight;
					}
				}
				for (Edge e : v.revAdj) {
					if(e.From.active && (e.Weight - min) == 0){
						v.revZeroEdges = e;
						if(!e.From.zeroEdges.contains(e))
							e.From.zeroEdges.add(e);
						break;
					}
				}
				if(v.revZeroEdges == null)
					System.out.println("This should be an error");
			}
		}
	}

	private static Edge findEdgeBetweenVertices(Vertex from, Vertex To) {
		for(Edge e : from.Adj) {
			if(e.To.equals(To)) {
				return e;
			}
		}

		return null;
	}

}
