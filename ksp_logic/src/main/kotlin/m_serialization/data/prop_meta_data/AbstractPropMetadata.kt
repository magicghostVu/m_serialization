package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

sealed class AbstractPropMetadata() {
    abstract val name: String
    abstract val propDec: KSPropertyDeclaration


    abstract fun getWriteStatement(objectNameContainThisProp:String): String
    abstract fun addImport(): List<String>

    companion object {

        //suffix name of object will be generated
        // eg: AClassMSerializer, AClass is target class
        const val serializerObjectNameSuffix = "MSerializer";


    }
}

