import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class EdwardsAlgo {

	public static void main(String[] args) throws IOException {
		Graph g = Graph.readGraph(new Scanner(new File("test/lp2-ck.txt")), true);
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
		FileWriter writer = new FileWriter("test/output.txt"); 
		writer.write(String.valueOf(sum)+"\n");
		for (Edge edge : spanningTreeEdges) {
			writer.write(edge.toString()+"\n");
		}
		writer.close();
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
		}
		if(x==null)
			return spanningTreeEdges;//cycles are expanded after returning
		
		//now we need to go backward from x to find the zero weight cycle
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
		HashMap<Vertex, Edge> to_set = new HashMap<>();
		HashMap<Vertex, Edge> from_set = new HashMap<>();
		do{//add edges that are not within C.cycleVerts
			for (Edge e : current.Adj) {
				if(e.To.superVertex != C && e.To.active){
					if(!to_set.containsKey(e.To))
						to_set.put(e.To, addNewEdge(C, e.To, e));
					else{
						Edge edge = to_set.get(e.To);
						if(e.Weight < edge.Weight){
							edge.Weight = e.Weight;
							edge.oldEdge = e;
						}
					}
				}
			}
			for (Edge e : current.revAdj) {
				if(e.From.superVertex != C && e.From.active){
					if(!from_set.containsKey(e.From))
						from_set.put(e.From, addNewEdge(e.From, C, e));
					else{
						Edge edge = from_set.get(e.From);
						if(e.Weight < edge.Weight){
							edge.Weight = e.Weight;
							edge.oldEdge = e;
						}
					}
				}
			}
			current = current.revZeroEdge.From;
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
			Edge ed = current.revZeroEdge;
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

	private static Edge addNewEdge(Vertex from, Vertex to, Edge ed) {
		Edge e = new Edge(from, to, ed.Weight);
		e.auxEdge = true;
		e.oldEdge = ed;
		from.Adj.add(e);
		to.revAdj.add(e);
		return e;
	}

	private static void reduceIncommingEdgeWeights(Graph g, Vertex root) {
		for (Vertex v : g) {
			if(v!=root && v.active){
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

	private static Edge findEdgeBetweenVertices(Vertex from, Vertex To) {
		for(Edge e : from.Adj) {
			if(e.To.equals(To)) {
				return e;
			}
		}

		return null;
	}

}
