package github.businessdirt.axite.vanadium.data.dag

/**
 * Interface for objects that use resources, used for lifetime analysis in the DAG.
 */
interface ResourceUser {
    /**
     * Set of resources that are read by this object.
     */
    val readResources: Set<String>

    /**
     * Set of resources that are written by this object.
     */
    val writeResources: Set<String>
}
