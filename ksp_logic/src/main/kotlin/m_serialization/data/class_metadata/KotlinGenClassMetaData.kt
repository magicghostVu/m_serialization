package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import io.netty.buffer.ByteBuf
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getFunctionNameWriteInternal
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.importSerializer
import java.lang.StringBuilder


// mỗi class được đánh dấu là m_serialization sẽ sinh ra một object này
class KotlinGenClassMetaData(
    constructorProps: List<AbstractPropMetadata>,
    otherProps: List<AbstractPropMetadata>,
    classDec: KSClassDeclaration,
    protocolUniqueId: Short,
    globalUniqueTag: Map<KSClassDeclaration, Short>
) : ClassMetaData(constructorProps, otherProps, classDec, protocolUniqueId, globalUniqueTag) {


    override fun doGenCode(codeGenerator: CodeGenerator) {
        val objectName = classDec.getSerializerObjectName()
        val fileBuilder = FileSpec.builder(
            classDec.packageName.asString(),
            objectName
        )
        val className = ClassName(classDec.packageName.asString(), classDec.simpleName.asString())
        val objectBuilder = TypeSpec.objectBuilder(objectName)
        val (funcSerialize, allImport) = genFunctionSerializer(className)
        funcSerialize.forEach {
            objectBuilder.addFunction(it)
        }
        allImport.forEach {
            fileBuilder.addImport(it, "")
        }


        val (funcDeserializers, allImportDeserializer) = genDeserializer(className)
        funcDeserializers.forEach {
            objectBuilder.addFunction(it)
        }

        allImportDeserializer.forEach {
            fileBuilder.addImport(it, "")
        }


        fileBuilder.addType(objectBuilder.build())
        fileBuilder.build().writeTo(codeGenerator, Dependencies(true))

    }


    private fun genDeserializer(typeName: TypeName): Pair<List<FunSpec>, Set<String>> {

        val funcRead = FunSpec.builder("readFrom")



        return Pair(emptyList(), emptySet())
    }


    private fun genFunctionSerializer(typeName: TypeName): Pair<List<FunSpec>, Set<String>> {

        val objectToWriteVarName = "objectToWrite"

        return if (!classDec.modifiers.contains(Modifier.SEALED)) {
            val funcWriteToInternal = FunSpec.builder(classDec.getFunctionNameWriteInternal())
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", ByteBuf::class).build()
                )
            val allImports = mutableSetOf<String>()

            for (prop in constructorProps) {
                funcWriteToInternal.addStatement(prop.getWriteStatement(objectToWriteVarName))
                allImports.addAll(prop.addImport())
            }
            for (prop in otherProps) {
                funcWriteToInternal.addStatement(prop.getWriteStatement(objectToWriteVarName))
                allImports.addAll(prop.addImport())
            }


            val funcWriteUserCall = FunSpec.builder(KSClassDecUtils.writeTo)
                .receiver(typeName)
                .addParameter(
                    ParameterSpec
                        .builder("buffer", ByteBuf::class.java)
                        .build()
                )
                .addStatement(
                    "${classDec.getFunctionNameWriteInternal()}(this, buffer)"
                )


            Pair(listOf(funcWriteToInternal.build(), funcWriteUserCall.build()), allImports)

        } else {


            val funWriteInternal = FunSpec.builder(classDec.getFunctionNameWriteInternal())
                .addParameter(
                    ParameterSpec
                        .builder(objectToWriteVarName, typeName)
                        .build()
                )
                .addParameter(
                    ParameterSpec
                        .builder("buffer", ByteBuf::class.java)
                        .build()
                )

            val allImports = mutableSetOf<String>()

            val whenExpression = StringBuilder()
            whenExpression.append("when($objectToWriteVarName){\n")

            val allRealChild = classDec.getAllActualChild()
            allRealChild.forEach {
                val format = "is ${it.simpleName.asString()} -> %s\n"
                val callMethod = String.format(
                    format,
                    "${it.getSerializerObjectName()}.${it.getFunctionNameWriteInternal()}($objectToWriteVarName, buffer)\n"
                )

                allImports.addAll(it.importSerializer())
                whenExpression.append(callMethod)
            }

            whenExpression.append("}")

            funWriteInternal.addStatement(whenExpression.toString())


            val funcWriteTo = FunSpec.builder(KSClassDecUtils.writeTo)
                .receiver(typeName)
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", ByteBuf::class.java).build()
                )
                .addStatement("${classDec.getFunctionNameWriteInternal()}($objectToWriteVarName, buffer)")


            Pair(listOf(funWriteInternal.build(), funcWriteTo.build()), allImports)
        }
    }


    companion object {


    }
}