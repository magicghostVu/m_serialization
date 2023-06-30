package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.AbstractPropMetadata

sealed class ClassMetaData(

) {

    lateinit var constructorProps: List<AbstractPropMetadata>
    lateinit var otherProps: List<AbstractPropMetadata>
    lateinit var classDec: KSClassDeclaration
    var protocolUniqueId: Short = 0
    lateinit var globalUniqueTag: Map<KSClassDeclaration, Short>

    // gen ra serializer và deserializer cho class này
    abstract fun doGenCode(codeGenerator: CodeGenerator): Unit
}