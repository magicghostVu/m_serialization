package m_serialization.data.gen_protocol_version

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

class KotlinGenProtocolVersion : IGenFileProtocolVersion {
    override fun genFileProtocolVersion(codeGenerator: CodeGenerator, protocolVersion: Int) {
        val classToGen = ClassName("mserialization.metadata", "MProtocolVersion")
        val fileGen = FileSpec.builder(classToGen)

        val objectToGen = TypeSpec.objectBuilder(classToGen)
        objectToGen.addProperty(
            PropertySpec
                .builder("protocolVersion", Int::class, KModifier.CONST)
                .initializer("$protocolVersion")
                .build()
        )

        fileGen.addType(objectToGen.build())

        fileGen.build().writeTo(codeGenerator, Dependencies(true))
    }
}