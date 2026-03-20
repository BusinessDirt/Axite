package github.businessdirt.axite.vanadium.assets.model

data class MeshData(
    val id: String,
    val positions: FloatArray,
    val textureCoordinates: FloatArray,
    val indices: IntArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshData

        if (id != other.id) return false
        if (!positions.contentEquals(other.positions)) return false
        if (!textureCoordinates.contentEquals(other.textureCoordinates)) return false
        if (!indices.contentEquals(other.indices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + positions.contentHashCode()
        result = 31 * result + textureCoordinates.contentHashCode()
        result = 31 * result + indices.contentHashCode()
        return result
    }
}