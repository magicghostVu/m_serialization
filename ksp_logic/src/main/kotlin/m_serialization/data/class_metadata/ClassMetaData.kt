package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.AbstractPropMetadata

sealed class ClassMetaData(
    val constructorProps: List<AbstractPropMetadata>,
    val otherProps: List<AbstractPropMetadata>,
    val classDec: KSClassDeclaration
) {
    // gen ra serializer và deserializer cho class này
    abstract fun doGenCode(codeGenerator: CodeGenerator): Unit
}