package m_serialization.data.export_json_meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class JsonClassMeta {


    abstract val fullName: String
}

@Serializable
sealed class ClassBasedJsonMeta() : JsonClassMeta() {
    abstract val constructorProps: List<PropJsonMeta>
    abstract val otherProps: List<PropJsonMeta>
}

@Serializable
@SerialName("sealed_class")
class SealedClassJsonMeta(
    override val fullName: String,

    override val constructorProps: List<PropJsonMeta>,
    override val otherProps: List<PropJsonMeta>,

    val allChildren: List<String>

) : ClassBasedJsonMeta() {
}

@Serializable
@SerialName("normal_class")
class NormalClassJsonMeta(
    override val fullName: String,
    override val constructorProps: List<PropJsonMeta>,
    override val otherProps: List<PropJsonMeta>,
    val directParent: String? = null,
    val protocolUniqueId: Short = -1,
) : ClassBasedJsonMeta() {}

@Serializable
@SerialName("enum_class")
class EnumClassJsonMeta(
    override val fullName: String,
    val values: List<String>
) : JsonClassMeta() {}