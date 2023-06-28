package m_serialization.data

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import io.netty.buffer.ByteBuf
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName


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

        }


        return fileBuilder.build()
    }
}