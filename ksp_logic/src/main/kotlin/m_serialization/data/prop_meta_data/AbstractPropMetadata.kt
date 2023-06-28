package m_serialization.data.prop_meta_data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

sealed class AbstractPropMetadata() {
    abstract val name: String
    abstract val propDec: KSPropertyDeclaration


    abstract fun getWriteStatement(): String
    abstract fun addImport(): List<String>

    companion object {

        //suffix name of object will be generated
        // eg: AClassMSerializer, AClass is target class
        const val serializerObjectNameSuffix = "MSerializer";

        fun getSerializerObjectName(classDec: KSClassDeclaration): String {
            return classDec.simpleName.asString() + serializerObjectNameSuffix
        }

        // chỉ áp dụng cho object
        fun KSClassDeclaration.getWriteObjectStatement(bufferVarName: String, objectVarName: String): String {
            return if (modifiers.contains(Modifier.SEALED)) {
                val serializerObjectName = getSerializerObjectName(this)
                val format = "%s.writeToAbstract(%s,%s)"
                String.format(format, serializerObjectName, objectVarName, bufferVarName)
            } else {
                val format = "%s.writeTo(%s)";
                String.format(format, objectVarName, bufferVarName)
            }
        }

        // chỉ áp dụng cho object
        fun KSClassDeclaration.importSerializer(): List<String> {
            val packageName = this.packageName.asString()
            // import object
            return if (this.modifiers.contains(Modifier.SEALED)) {
                listOf(
                    packageName + "." + getSerializerObjectName(this)
                )
            } else {// import object and method
                listOf(
                    packageName + "." + getSerializerObjectName(this) + "." + "writeTo"
                )
            }
        }
    }
}

