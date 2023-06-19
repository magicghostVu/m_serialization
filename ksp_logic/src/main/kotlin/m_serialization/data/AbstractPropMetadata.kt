package m_serialization.data

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

sealed class AbstractPropMetadata() {
    abstract val name: String
    abstract val propDec: KSPropertyDeclaration


    abstract fun getWriteStatement(): String
    abstract fun addImport(): List<String>
}

