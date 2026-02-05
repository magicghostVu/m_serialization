package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.data.export_json_meta.PropJsonMeta

sealed class AbstractPropMetadata() {
    abstract val name: String
    abstract val propDec: KSPropertyDeclaration


    abstract fun getWriteStatement(objectNameContainThisProp: String): String
    abstract fun addImportForWrite(): List<String>


    abstract fun getReadStatement(bufferVarName: String, varNameToAssign: String, declareNewVar: Boolean): String

    abstract fun addImportForRead(): List<String>


    abstract fun addImportForCalculateSize(): List<String>


    abstract fun expressionForCalSize(varNameToAssign: String): String


    abstract fun mtoString(): String

    abstract fun toJsonPropMetaJson(): PropJsonMeta

    companion object {

        //suffix name of object will be generated
        // eg: AClassMSerializer, AClass is target class
        const val serializerObjectNameSuffix = "MSerializer";

        const val readFromFuncName = "readFrom"

        const val serializeSizeFuncName = "serializeSize"


    }
}

