/**
 * Class to represent a vertex of a graph
 * 
 *
 */

import java.util.*;

public class Vertex {
    public int name; // name of the vertex
    public boolean seen; // flag to check if the vertex has already been visited
    public Vertex parent; // parent of the vertex
    public int distance; // distance to the vertex from the source vertex
    public List<Edge> Adj, revAdj; // adjacency list; use LinkedList or ArrayList
    public List<Edge> zeroEdges;
    public Edge revZeroEdge;
    boolean active;
    Vertex superVertex;//if this vertex is contained inside a super vertex

    /**
     * Constructor for the vertex
     * 
     * @param n
     *            : int - name of the vertex
     */
    Vertex(int n) {
	name = n;
	seen = false;
	parent = null;
	Adj = new ArrayList<Edge>();
	revAdj = new ArrayList<Edge>();   /* only for directed graphs */
	zeroEdges = new ArrayList<Edge>();
	active = true;
    }

    /**
     * Method to represent a vertex by its name
     */
    public String toString() {
	return Integer.toString(name);
    }
    
    @Override
    public boolean equals(Object t){
    	if(t==null)
    		return false;
    	Vertex u = (Vertex)t;
    	if(this.name == u.name)
    		return true;
    	else
    		return false;
    }
    
    @Override
    public int hashCode(){
    	return this.name;
    }
}
