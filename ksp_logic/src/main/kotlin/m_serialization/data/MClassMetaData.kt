package m_serialization.data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import io.netty.buffer.ByteBuf
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.importSerializer


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

        if (!classDec.modifiers.contains(Modifier.SEALED)) {
            val funcWriteTo = FunSpec.builder("writeTo")
                .receiver(typeName)
                .addParameter(
                    ParameterSpec.builder("buffer", ByteBuf::class).build()
                )
            val allImports = mutableSetOf<String>()

            // phải check class này nếu là sealed thì gen ra function writeToAbstract()


            for (prop in constructorProps) {
                funcWriteTo.addStatement(prop.getWriteStatement())
                allImports.addAll(prop.addImport())
            }
            for (prop in otherProps) {
                funcWriteTo.addStatement(prop.getWriteStatement())
                allImports.addAll(prop.addImport())
            }


            allImports.forEach {
                fileBuilder.addImport(it, "")
            }
            fileBuilder.addType(
                TypeSpec.objectBuilder(objectName)
                    .addFunction(
                        funcWriteTo.build()
                    )
                    .build()
            )
        } else {
            val objectToWriteVarName = "objectToWrite"
            val funcWriteTo = FunSpec.builder("writeToAbstract")
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", ByteBuf::class).build()
                )
            val allImports = mutableSetOf<String>()


            // add expression when for all case
            val whenExpression = StringBuilder()
            whenExpression.append("when($objectToWriteVarName){\n")
            val allChildren = classDec.getSealedSubclasses().toList()
            allChildren.forEach { childClass ->

                /*whenExpression.append(
                    "is ${childClass.simpleName.asString()} -> {}\n"
                )*/
                val serializerObjectName = childClass.getSerializerObjectName()

                val isExpressionForThisChild: String = if (childClass.modifiers.contains(Modifier.SEALED)) {
                    val format = "is ${childClass.simpleName.asString()} -> %s\n"

                    val callMethod = "${serializerObjectName}.${writeToAbstract}($objectToWriteVarName,buffer)"

                    // chèn import
                    //allImports.add("${classDec.packageName.asString()}.${serializerObjectName}")
                    String.format(format, callMethod)
                } else {
                    //allImports.add("${classDec.packageName.asString()}.${serializerObjectName}")
                    val format = "is ${childClass.simpleName.asString()} -> %s\n"
                    val callMethod = "${objectToWriteVarName}.$writeTo(buffer)"
                    String.format(format, callMethod)
                }
                allImports.addAll(childClass.importSerializer())

                whenExpression.append(isExpressionForThisChild)
            }

            whenExpression.append("}")

            funcWriteTo.addStatement(whenExpression.toString())


            allImports.forEach {
                fileBuilder.addImport(it, "")
            }

            fileBuilder
                .addType(
                    TypeSpec.objectBuilder(objectName)
                        .addFunction(
                            funcWriteTo.build()
                        )
                        .build()
                )
        }


        return fileBuilder.build()
    }

    companion object {
        val writeTo = "writeTo"
        val writeToAbstract = "writeToAbstract"
    }
}