package github.businessdirt.axite.vanadium.core.dag

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Represents a Directed Acyclic Graph (DAG) used for task scheduling and resource lifetime analysis.
 * @param T The type of data stored in each node.
 */
open class DirectedAcyclicGraph<T> {

    val nodes: MutableList<Node<T>> = arrayListOf()
    val layers = mutableListOf<List<Node<T>>>()
    val resourceLifetimes = mutableMapOf<String, IntRange>()
    var sortedNodes: List<Node<T>> = emptyList()
        private set

    /**
     * Compiles the graph by performing topological sorting, layer calculation (barriers),
     * and resource lifetime analysis.
     */
    open fun compile() {
        sortedNodes = sortTopologically()
        groupIntoLayers()
        analyzeResourceLifetimes()

        // TODO: add like a keybind to do this once or something
        //if (LoggingConfigurator.isDebugMode) logger.debug(toJson())
    }

    /**
     * Automatically resolves dependencies between nodes based on resource usage.
     * If a node reads a resource, it will depend on the last node that wrote to it.
     */
    protected fun resolveDependencies() {
        val lastWriters = mutableMapOf<String, Node<T>>()

        nodes.forEach { node ->
            node.dependencies.clear()
            val resourceUser = node as? ResourceUser ?: node.data as? ResourceUser
            if (resourceUser != null) {
                // Track nodes we already depend on to avoid duplicates
                val currentDependencies = mutableSetOf<Node<T>>()

                // For each resource read, add dependency to the last writer
                resourceUser.readResources.forEach { res ->
                    lastWriters[res]?.let { writer ->
                        if (writer != node && currentDependencies.add(writer)) {
                            node.dependencies.add(writer)
                        }
                    }
                }

                // Update last writers for resources written by this node
                // If a resource is already written by another node, we depend on it to ensure correct ordering (in-place writes)
                resourceUser.writeResources.forEach { res ->
                    lastWriters[res]?.let { writer ->
                        if (writer != node && currentDependencies.add(writer)) {
                            node.dependencies.add(writer)
                        }
                    }
                    lastWriters[res] = node
                }
            }
        }
    }

    /**
     * Performs a topological sort of the nodes in the graph.
     * @return A list of nodes in topological order.
     * @throws IllegalStateException if the graph contains cycles.
     */
    private fun sortTopologically(): List<Node<T>> {
        val result = mutableListOf<Node<T>>()
        val inDegree = nodes.associateWith { it.dependencies.size }.toMutableMap()

        // Pre-calculate successors to optimize topological sort lookup
        val successors = nodes.associateWith { mutableListOf<Node<T>>() }
        nodes.forEach { node ->
            node.dependencies.forEach { successors[it]?.add(node) }
        }

        val queue: Queue<Node<T>> = LinkedList(nodes.filter { inDegree[it] == 0 })

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: break
            result.add(current)

            successors[current]?.forEach { successor ->
                inDegree[successor] = (inDegree[successor] ?: 1) - 1
                if (inDegree[successor] == 0) {
                    queue.add(successor)
                }
            }
        }

        if (result.size != nodes.size) throw IllegalStateException("Graph has cycles!")
        return result
    }

    /**
     * Groups nodes into layers (barriers) where each node in a layer only depends on nodes in previous layers.
     */
    private fun groupIntoLayers() {
        layers.clear()
        val processed = mutableSetOf<Node<T>>()
        val remaining = sortedNodes.toMutableList()

        while (remaining.isNotEmpty()) {
            val currentLayer = remaining.filter { node ->
                node.dependencies.all { processed.contains(it) }
            }

            if (currentLayer.isEmpty()) break

            layers.add(currentLayer)
            processed.addAll(currentLayer)
            remaining.removeAll(currentLayer)
        }
    }

    /**
     * Analyses the lifetime of resources used by the nodes in the graph.
     * Considers nodes that implement [ResourceUser] or whose data implements [ResourceUser].
     */
    private fun analyzeResourceLifetimes() {
        val firstSeen = mutableMapOf<String, Int>()
        val lastSeen = mutableMapOf<String, Int>()

        sortedNodes.forEachIndexed { index, node ->
            val resourceUser = node as? ResourceUser ?: node.data as? ResourceUser
            if (resourceUser != null) {
                val resources = resourceUser.readResources + resourceUser.writeResources
                resources.forEach { res ->
                    firstSeen.putIfAbsent(res, index)
                    lastSeen[res] = index
                }
            }
        }

        firstSeen.keys.forEach { res ->
            resourceLifetimes[res] = firstSeen[res]!!..lastSeen[res]!!
        }
    }

    /**
     * Dumps the graph structure to a JSON string for debugging purposes.
     * @return A JSON representation of the graph.
     */
    @Suppress("unused")
    fun toJson(): String = Json.encodeToString(GraphDump(
        nodeCount = nodes.size,
        layers = layers.size,
        nodes = sortedNodes.map { node ->
            NodeDump(
                data = node.data.toString(),
                dependencies = node.dependencies.size
            )
        }
    ))

    /**
     * Data class used for JSON dumping of the graph structure.
     */
    @Serializable
    private data class GraphDump(
        val nodeCount: Int,
        val layers: Int,
        val nodes: List<NodeDump>
    )

    /**
     * Data class used for JSON dumping of a single node.
     */
    @Serializable
    private data class NodeDump(
        val data: String,
        val dependencies: Int
    )
}
