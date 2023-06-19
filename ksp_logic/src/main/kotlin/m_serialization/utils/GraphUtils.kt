package m_serialization.utils

import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.util.*

internal object GraphUtils {
    enum class VertexState {
        NOT_VISITED,
        IN_STACK,
        DONE
    }


    fun findCycle(graph: DefaultDirectedGraph<String, DefaultEdge>): List<List<String>> {
        val visited = mutableMapOf<String, VertexState>()
        val result = mutableListOf<List<String>>()
        for (v in graph.vertexSet()) {
            val state = visited.getOrDefault(v, VertexState.NOT_VISITED)
            if (state == VertexState.NOT_VISITED) {
                val stack = Stack<String>()
                stack.push(v)
                visited[v] = VertexState.IN_STACK
                processDFSTree(graph, stack, visited, result)
            }
        }
        return result
    }


    private fun processDFSTree(
        graph: DefaultDirectedGraph<String, DefaultEdge>,
        stack: Stack<String>,
        visited: MutableMap<String, VertexState>,
        result: MutableList<List<String>>
    ) {
        val currentV = stack.peek()
        val allChild = currentV.getAllChild(graph)
        for (v in allChild) {
            val state = visited.getOrDefault(v, VertexState.NOT_VISITED)
            if (state == VertexState.IN_STACK) {
                val r = extractCycle(stack, v)
                result.add(r)
            } else if (state == VertexState.NOT_VISITED) {
                stack.push(v)
                visited[v] = VertexState.IN_STACK
                processDFSTree(graph, stack, visited, result)
            }
        }
        visited[currentV] = VertexState.DONE
        stack.pop()
    }


    private fun extractCycle(stack: Stack<String>, vertex: String): List<String> {
        val tmpStack = Stack<String>()
        tmpStack.push(stack.pop())
        while (tmpStack.peek() != vertex) {
            tmpStack.push(stack.pop())
        }

        val list = mutableListOf<String>()

        while (tmpStack.isNotEmpty()) {
            val tmpVertex = tmpStack.pop()
            list.add(tmpVertex)
            stack.push(tmpVertex)
        }

        return list
    }


    private fun String.getAllChild(graph: DefaultDirectedGraph<String, DefaultEdge>): Set<String> {
        val allEdge = graph.edgesOf(this)
        return allEdge
            .asSequence()
            .filter {
                val source = graph.getEdgeSource(it)
                source == this
            }
            .map {
                graph.getEdgeTarget(it)
            }
            .toSet()
    }
}