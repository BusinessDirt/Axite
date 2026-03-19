package github.businessdirt.axite.vanadium.graph

class Model(val id: String) {

    val meshList: MutableList<Mesh> = mutableListOf()

    fun cleanup() = meshList.forEach(Mesh::cleanup)
}