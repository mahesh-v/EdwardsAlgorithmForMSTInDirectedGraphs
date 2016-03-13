import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class EdwardsAlgo {

	public static void main(String[] args) throws FileNotFoundException {
		Graph g = Graph.readGraph(new Scanner(new File("test/sample.txt")), true);
		List<Edge> spanningTreeEdges = findMST(g, g.verts.get(1));
		System.out.println(spanningTreeEdges);
	}

	public static List<Edge> findMST(Graph g, Vertex root) {
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
			Vertex cycle_vert = edge.oldOrigin;
			for (Edge ed : cycle_vert.zeroEdges) {//it would have been in zeroEdges list
				if(ed.To.equals(edge.To)){
					sTreeEdges.add(ed);
					break;
				}
			}
		}
		//Step 2, remove incoming edge into C and replace with that which was incoming
		sTreeEdges.remove(C.revZeroEdges);
		Vertex cycle_vert = C.revZeroEdges.oldTo;
		for (Edge ed : cycle_vert.revAdj) {
			if(ed.From.equals(C.revZeroEdges.From)){
				sTreeEdges.add(ed);
				break;
			}
		}
		
		//Step 3, add all the edges that were shrunk back to MST
		current = cycle_vert;
		do{
			Edge ed = current.revZeroEdges;
			if(!ed.To.equals(cycle_vert)) //do not complete the cycle only in this case
				sTreeEdges.add(ed);
			current = ed.From;
		}while (current!=cycle_vert);
		
//		do{//expand cycle, deleting new edges
//			current.active = true;
//			Iterator<Edge> iter = current.Adj.iterator();
//			while (iter.hasNext()) {
//				Edge e = iter.next();
//				if(e.auxEdge){
//					iter.remove();
//					if(sTreeEdges.contains(e))
//						sTreeEdges.remove(e);
//				}
//				else//other 0 wt edges within cycle are added to MST
//					sTreeEdges.add(e);
//			}
//			iter = current.revAdj.iterator();
//			while (iter.hasNext()) {
//				Edge e = iter.next();
//				if(e.auxEdge){
//					iter.remove();
//					if(sTreeEdges.contains(e))
//						sTreeEdges.remove(e);
//				}
//				else//other 0 wt edges within cycle are added to MST
//					sTreeEdges.add(e);
//			}
//			current = current.revZeroEdges.From;
//		} while(current != cycleStart);
		g.verts.remove(C);
		g.numNodes--;
//		Iterator<Edge> iter = sTreeEdges.iterator();
//		while (iter.hasNext()) {
//			Edge edge = iter.next();
//			if(edge.auxEdge){//need to remove this edge, and expand C
//				iter.remove();
//				if(edge.To.cycleVerts.size() > 0){
//					for (Vertex v : edge.To.cycleVerts) {
//						v.active = true;
//						Edge zeroEdge = v.revZeroEdges.get(0);//we only ever used the first zero edge. Can optimize by making this a single edge
//						zeroEdge.active = true;
//						sTreeEdges.add(zeroEdge);
//					}
//					g.verts.remove(edge.To);
//				}
//				if(edge.From.cycleVerts.size() > 0){
//					for (Vertex v : edge.From.cycleVerts) {
//						v.active = true;
//						Edge zeroEdge = v.zeroEdges.get(0);//we only ever used the first zero edge. Can optimize by making this a single edge
//						zeroEdge.active = true;
//						sTreeEdges.add(zeroEdge);
//					}
//					g.verts.remove(edge.From);
//				}
//			}
//		}
		return sTreeEdges;
	}

	private static void addNewEdge(Graph g, Vertex from, Vertex to, Edge ed) {
		for (Edge edge : from.Adj) {
			if(edge.auxEdge && edge.From.equals(from) && edge.To.equals(to)){
				if(edge.Weight > ed.Weight){
					edge.Weight = ed.Weight;//simply replace the weight with the lesser weight
					edge.oldOrigin = ed.From;
					edge.oldTo = ed.To;
				}
				return;
			}
		}
		Edge e = new Edge(from, to, ed.Weight);
//		ed.active = false;
		e.auxEdge = true;
		e.oldOrigin = ed.From;
		e.oldTo = ed.To;
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

}
