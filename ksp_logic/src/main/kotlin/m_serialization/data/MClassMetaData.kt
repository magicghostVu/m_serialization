package m_serialization.data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import io.netty.buffer.ByteBuf
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getFunctionNameWriteInternal
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.importSerializer
import java.lang.StringBuilder


// mỗi class được đánh dấu là m_serialization sẽ sinh ra một object này
class MClassMetaData(
    val constructorProps: List<AbstractPropMetadata>,
    val otherProps: List<AbstractPropMetadata>,
    val classDec: KSClassDeclaration
) {


    fun genSerializer(): FileSpec {

        val objectName = classDec.getSerializerObjectName()
        val fileBuilder = FileSpec.builder(
            classDec.packageName.asString(),
            objectName
        )
        val typeName = ClassName(classDec.packageName.asString(), classDec.simpleName.asString())

        val objectToWriteVarName = "objectToWrite"

        if (!classDec.modifiers.contains(Modifier.SEALED)) {
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


            allImports.forEach {
                fileBuilder.addImport(it, "")
            }
            fileBuilder.addType(
                TypeSpec.objectBuilder(objectName)
                    .addFunction(
                        funcWriteToInternal.build()
                    )
                    .addFunction(funcWriteUserCall.build())
                    .build()
            )
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

            allImports.forEach {
                fileBuilder.addImport(it, "")
            }

            val funcWriteTo = FunSpec.builder(KSClassDecUtils.writeTo)
                .receiver(typeName)
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", ByteBuf::class.java).build()
                )
                .addStatement("${classDec.getFunctionNameWriteInternal()}($objectToWriteVarName, buffer)")


            fileBuilder
                .addType(
                    TypeSpec.objectBuilder(objectName)
                        .addFunction(
                            funWriteInternal.build()
                        )
                        .addFunction(funcWriteTo.build())
                        .build()
                )
        }

        return fileBuilder.build()
    }

    companion object {


    }
}