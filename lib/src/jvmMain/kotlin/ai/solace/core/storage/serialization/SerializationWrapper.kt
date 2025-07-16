package ai.solace.core.storage.serialization

import kotlinx.serialization.Serializable

/**
 * A simple wrapper class for serializing objects that don't have built-in serialization support.
 *
 * This class is used to wrap objects that need to be serialized but don't implement the Serializable interface.
 * It simply stores the string representation of the object.
 *
 * @property value The string representation of the wrapped object.
 */
@Serializable
data class SerializationWrapper(val value: String)