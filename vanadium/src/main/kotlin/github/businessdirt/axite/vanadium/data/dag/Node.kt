package github.businessdirt.axite.vanadium.data.dag

/**
 * Represents a node in the Directed Acyclic Graph.
 * @param T The type of data stored in the node.
 * @property data The data stored in this node.
 */
abstract class Node<T>(
    val data: T
) {
    /**
     * The list of nodes that this node depends on.
     */
    val dependencies: MutableList<Node<T>> = arrayListOf()

    /**
     * Executes the task associated with this node.
     */
    abstract fun execute()
}
