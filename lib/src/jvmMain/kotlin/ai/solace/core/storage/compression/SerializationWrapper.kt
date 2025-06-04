package ai.solace.core.storage.compression

import kotlinx.serialization.Serializable

/**
 * A wrapper class for serializing arbitrary objects as strings.
 *
 * This class is used as a fallback when direct serialization is not possible.
 */
@Serializable
data class SerializationWrapper(val value: String)
