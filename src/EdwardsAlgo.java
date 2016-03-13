import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class EdwardsAlgo {

	public static void main(String[] args) throws FileNotFoundException {
		Graph g = Graph.readGraph(new Scanner(new File("test/lp2-m.txt")), true);
		System.out.println("Starting algo");
		Timer t = new Timer();
		List<Edge> spanningTreeEdges = findMST(g, g.verts.get(1));
		t.end();
		long sum = 0;
		for (Edge edge : spanningTreeEdges) {
			sum+=edge.Weight;
		}
		System.out.println(sum);
		System.out.println(t);
		Collections.sort(spanningTreeEdges, new EdgeToComparator());
//		System.out.println(spanningTreeEdges);
	}

	public static List<Edge> findMST(Graph g, Vertex root) {
		for (Vertex vertex : g) {
			if(vertex.active){
				vertex.zeroEdges.clear();
				vertex.parent = null;
				vertex.seen =  false;
			}
		}
		reduceIncommingEdgeWeights(g, root);
		List<Edge> spanningTreeEdges = new ArrayList<Edge>();
		LinkedList<Vertex> queue = new LinkedList<Vertex>();
		queue.add(root);
		while(!queue.isEmpty()){
			Vertex v = queue.removeFirst();
			v.seen = true;
			for (Edge e : v.zeroEdges) {
				if(!e.To.active)
					continue;
				e.isInMST = true;
				spanningTreeEdges.add(e);
				Vertex u = e.To;
				if(!u.seen && u.active){
					u.parent=v;
					queue.add(u);
				}
			}
		}
		Vertex x = null;
		for (Vertex v : g) {
			if(!v.seen && v.active){
				x = v;
				break;
			}
//			v.seen = false;//for future use
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
//		x.seen = false;
//		while(x!=cycleStart){ //reset all edges up to that cycle to false for later use
//			x = x.revZeroEdges.From;
//			x.seen = false;
//		}
		Vertex C = new Vertex(++(g.numNodes));
		do{//add all vertices in cycle to C.cycleVerts
//			current.seen = false;//unrelated. for future use
			
			C.cycleVerts.add(current);
			current.active = false;//make it inactive for the future
			current = current.revZeroEdges.From;
		} while(current != cycleStart);
		
		g.verts.add(C);
		do{//add edges that are not within C.cycleVerts
			for (Edge e : current.Adj) {
				if(!C.cycleVerts.contains(e.To) && e.To.active){
					boolean modified = false;
					for (Edge edge : C.Adj) {
						if(edge.To.equals(e.To)){
							if(e.Weight < edge.Weight){
								edge.Weight =e.Weight;
								edge.oldEdge = e;
							}
							modified = true;
							break;
						}
					}
					if(!modified)
						addNewEdge(C, e.To, e);
				}
			}
			for (Edge e : current.revAdj) {
				if(!C.cycleVerts.contains(e.From) && e.From.active){
					boolean modified = false;
					for (Edge edge : C.revAdj) {
						if(edge.From.equals(e.From)){
							if(e.Weight < edge.Weight){
								edge.Weight =e.Weight;
								edge.oldEdge = e;
							}
							modified = true;
							break;
						}
					}
					if(!modified)
						addNewEdge(e.From, C, e);
				}
			}
			current = current.revZeroEdges.From;
		}while(current != cycleStart);
		//call recursively with new graph
		List<Edge> sTreeEdges = findMST(g, root);
		//expand the cycle
		//Step 1, remove out-going edges from C and replace with outgoing edges from cycle.
		for (Edge edge : C.zeroEdges) {
			if(edge.isInMST){
				edge.isInMST = false;
			sTreeEdges.remove(edge);
			edge.oldEdge.isInMST = true;
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
			}
		}
		//Step 2, remove incoming edge into C and replace with that which was incoming
		Edge incoming = findEdgeBetweenVertices(C.parent, C);
		incoming.isInMST = false;
		sTreeEdges.remove(incoming);
		incoming.oldEdge.isInMST = true;
		sTreeEdges.add(incoming.oldEdge);
		Vertex cycle_vert = incoming.oldEdge.To;
		cycle_vert.parent = C.parent;
		
		//Step 3, add all the edges that were shrunk back to MST
		current = cycle_vert;
		do{
			Edge ed = current.revZeroEdges;
			current.active = true;
			if(current != cycle_vert) {
				current.parent = ed.From;
			}
			if(!ed.To.equals(cycle_vert)){ //do not complete the cycle only in this case
				ed.isInMST = true;
				sTreeEdges.add(ed);
			}
			current = ed.From;
		}while (current!=cycle_vert);
		
		C.active=false;
		return sTreeEdges;
	}

	private static void addNewEdge(Vertex from, Vertex to, Edge ed) {
		Edge e = new Edge(from, to, ed.Weight);
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
