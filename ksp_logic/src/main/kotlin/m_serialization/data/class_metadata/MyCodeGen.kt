package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.AbstractPropMetadata

class MyCodeGen(
    constructorProps: List<AbstractPropMetadata>,
    otherProps: List<AbstractPropMetadata>,
    classDec: KSClassDeclaration
) : ClassMetaData(constructorProps, otherProps, classDec) {
    override fun doGenCode(codeGenerator: CodeGenerator) {
        codeGenerator.createNewFile(
            Dependencies(true),
            "",
            "aa",
            "gd"
        )
    }
}