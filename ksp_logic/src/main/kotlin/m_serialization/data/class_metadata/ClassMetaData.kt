package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import m_serialization.data.export_json_meta.EnumClassJsonMeta
import m_serialization.data.export_json_meta.JsonClassMeta
import m_serialization.data.export_json_meta.NormalClassJsonMeta
import m_serialization.data.export_json_meta.SealedClassJsonMeta
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllEnumEntrySimpleName

sealed class ClassMetaData(

) {

    lateinit var constructorProps: List<AbstractPropMetadata>
    lateinit var otherProps: List<AbstractPropMetadata>
    lateinit var classDec: KSClassDeclaration
    var protocolUniqueId: Short = 0
    lateinit var globalUniqueTag: Map<KSClassDeclaration, Short>


    lateinit var logger: KSPLogger


    abstract fun languageGen(): LanguageGen

    // cha trên trực tiếp của class này
    var parent: ClassMetaData? = null

    // gen ra serializer và deserializer cho class này
    abstract fun doGenCode(codeGenerator: CodeGenerator): Unit

    companion object {

        val byteBufTypeName = ClassName("io.netty.buffer", "ByteBuf")
    }


    private fun mToString(): String {
        val allProp = constructorProps + otherProps
        val header = "${classDec.qualifiedName!!.asString()}:"
        val allPropStr = allProp.joinToString(separator = ",") { it.mtoString() }
        return header + allPropStr
    }

    override fun hashCode(): Int {
        //logger.warn("gen hash for ${classDec.qualifiedName!!.asString()}")
        return mToString().hashCode()
    }

    fun toJsonClassMetaData(): JsonClassMeta {
        return if (classDec.classKind == ClassKind.ENUM_CLASS) {
            EnumClassJsonMeta(
                classDec.qualifiedName!!.asString(),
                classDec.getAllEnumEntrySimpleName()
            )
        } else {


            val constructorProps = constructorProps
                .asSequence()
                .map {
                    it.toJsonPropMetaJson()
                }.toList()


            val otherProps = otherProps
                .asSequence()
                .map { it.toJsonPropMetaJson() }
                .toList()
            if (classDec.modifiers.contains(Modifier.SEALED)) {


                val allChildren = classDec
                    .getSealedSubclasses()
                    .map {
                        it.qualifiedName!!.asString()
                    }.toList()

                SealedClassJsonMeta(
                    classDec.qualifiedName!!.asString(),
                    constructorProps,
                    otherProps,
                    allChildren
                )

            } else {

                NormalClassJsonMeta(
                    classDec.qualifiedName!!.asString(),
                    constructorProps,
                    otherProps,
                    parent?.classDec?.qualifiedName?.asString(),
                    protocolUniqueId
                )
            }
        }
    }
}