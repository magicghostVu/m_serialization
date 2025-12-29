package m_serialization.data.export_json_meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import m_serialization.data.prop_meta_data.PrimitiveType

@Serializable
sealed class PropJsonMeta {

    abstract val name: String
}

@Serializable
@SerialName("object")
// contain enum
class ObjectPropJsonMeta(
    override val name: String,
    val className: String,
) : PropJsonMeta()


@Serializable
@SerialName("primitive")
class PrimitivePropJsonMeta(
    override val name: String,
    val primitiveType: PrimitiveType
) : PropJsonMeta() {}


@Serializable
sealed class ListPropJsonMeta() : PropJsonMeta()

@Serializable
@SerialName("list_primitive")
class ListPrimitivePropJsonMeta(
    override val name: String,
    val elementType: PrimitiveType
) : ListPropJsonMeta()

@Serializable
@SerialName("list_object")
class ListObjectPropJsonMeta(
    override val name: String,
    val classElement: String
) : ListPropJsonMeta()

@Serializable
sealed class MapPropJsonMeta() : PropJsonMeta()

@Serializable
sealed class MapPrimitiveKeyPropJsonMeta() : MapPropJsonMeta() {

    abstract val keyType: PrimitiveType;

}

@Serializable
@SerialName("map_primitive_key_value")
class MapPrimitiveKeyValueJsonMeta(
    override val name: String,
    override val keyType: PrimitiveType,
    val valueType: PrimitiveType
) : MapPrimitiveKeyPropJsonMeta() {

}

@Serializable
@SerialName("map_primitive_key_object_value")
class MapPrimitiveKeyObjectValueJsonMeta(
    override val name: String,
    override val keyType: PrimitiveType,
    val valueType: String
) : MapPrimitiveKeyPropJsonMeta() {

}

@Serializable
sealed class MapEnumKeyJsonMeta() : MapPropJsonMeta() {
    abstract val enumKey: String;
}

@Serializable
@SerialName("map_enum_key_value_primitive_value")
class MapEnumKeyPrimitiveValueJsonMeta(
    override val name: String,
    override val enumKey: String,
    private val valueType: PrimitiveType
) : MapEnumKeyJsonMeta() {

}

@Serializable
@SerialName("map_enum_key_value_object_value")
class MapEnumKeyObjectValueJsonMeta(
    override val name: String,
    override val enumKey: String,
    private val valueType: String
) : MapEnumKeyJsonMeta()