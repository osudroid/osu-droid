package org.andengine.graphics

/** Data type of one vertex attribute as stored in the vertex buffer. */
enum class VertexFormat(val sizeBytes: Int) {
    Float1(4),
    Float2(8),
    Float3(12),
    Float4(16),

    /** Four unsigned bytes mapped to 0..1, e.g. a packed RGBA color. */
    UByte4Normalized(4),

    /** One unsigned 16-bit integer, not normalized, read as a float in the shader. */
    UShort1(2)
}

/** One attribute of a vertex: shader input [location], data [format] and byte offset in the vertex. */
class VertexAttribute(val location: Int, val format: VertexFormat, val offsetBytes: Int) {

    init {
        require(location >= 0) { "Attribute location must not be negative, got $location" }
        require(offsetBytes >= 0) { "Attribute offset must not be negative, got $offsetBytes" }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is VertexAttribute && location == other.location && format == other.format && offsetBytes == other.offsetBytes

    override fun hashCode(): Int = (location * 31 + format.hashCode()) * 31 + offsetBytes

    override fun toString() = "VertexAttribute($location, $format, +$offsetBytes)"
}

/**
 * How interleaved vertices are laid out in a vertex buffer: [strideBytes] per vertex and the
 * [attributes] read from each. Immutable; compared by content.
 */
class VertexLayout(val strideBytes: Int, attributes: List<VertexAttribute>) {

    /** Read-only copy of the attributes, so the cached hash and VAO keys cannot change. */
    val attributes: List<VertexAttribute> = java.util.Collections.unmodifiableList(ArrayList(attributes))

    private val hash: Int

    init {
        require(strideBytes > 0) { "Stride must be positive, got $strideBytes" }
        require(this.attributes.isNotEmpty()) { "A vertex layout needs at least one attribute" }
        val list = this.attributes
        var h = strideBytes
        for (i in list.indices) {
            val a = list[i]
            require(a.offsetBytes + a.format.sizeBytes <= strideBytes) {
                "Attribute at location ${a.location} ends after the stride of $strideBytes bytes"
            }
            for (j in 0 until i) {
                require(list[j].location != a.location) { "Attribute location ${a.location} is used twice" }
            }
            h = h * 31 + a.hashCode()
        }
        hash = h
    }

    // Index loop instead of List.equals: no iterator allocation on the draw path.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexLayout || hash != other.hash || strideBytes != other.strideBytes) return false
        val a = attributes
        val b = other.attributes
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (a[i] != b[i]) return false
        }
        return true
    }

    override fun hashCode(): Int = hash

    override fun toString() = "VertexLayout(stride=$strideBytes, $attributes)"
}
