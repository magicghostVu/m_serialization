package m_serialization.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import m_serialization.annotations.MTransient
import m_serialization.data.prop_meta_data.*
import m_serialization.data.prop_meta_data.PrimitiveType.Companion.isPrimitive
import m_serialization.data.prop_meta_data.PrimitiveType.Companion.toPrimitiveType
import java.util.*

object KSClassDecUtils {

    // get all con, cháu ...
    // nếu con(cháu...) là sealed thì không add??

    lateinit var logger: KSPLogger

    fun KSClassDeclaration.getAllChildRecursive(
        context: KSClassDeclaration,
        propName: String
    ): List<KSClassDeclaration> {
        //val className = qualifiedName?.asString();
        if (!this.modifiers.contains(Modifier.SEALED)) {
            return emptyList()
        }

        val result = mutableListOf<KSClassDeclaration>()

        val q = LinkedList<KSClassDeclaration>()
        q.add(this)
        while (q.isNotEmpty()) {
            val tmp = q.removeFirst()
            val allChildren = tmp.getSealedSubclasses().toList()
            allChildren.forEach {
                if (it.modifiers.contains(Modifier.SEALED)) {
                    q.add(it)
                } else {
                    /*logger.warn("at context ${context.qualifiedName?.asString()}, prop name $propName," +
                            "add child ${it.qualifiedName?.asString()}, " +
                            "parent is ${tmp.qualifiedName?.asString()}")*/
                    result.add(it)
                }
            }
        }
        return result
    }

    fun KSPropertyDeclaration.getAllAnnotationName(): Set<String> {
        return annotations
            .map { anno ->
                anno.annotationType
                    .resolve()
                    .declaration
                    .qualifiedName
            }
            .filterNotNull()
            .map {
                it.asString()
            }.toSet()
    }

    fun KSClassDeclaration.getAllAnnotationName(): Set<String> {
        return annotations
            .map { anno ->
                anno.annotationType
                    .resolve()
                    .declaration
                    .qualifiedName
            }
            .filterNotNull()
            .map {
                it.asString()
            }.toSet()
    }


    fun KSClassDeclaration.getAllPropMetaData(): Map<String, AbstractPropMetadata> {
        val allPropWillAnalyze = getAllProperties()
            .filter {
                it.hasBackingField
            }
            .filter {
                !it.getAllAnnotationName().contains(MTransient::class.java.name)
            }.toList()

        val tmpMap = mutableMapOf<String, AbstractPropMetadata>()

        allPropWillAnalyze.forEach {
            val type = it.type.resolve()
            val propName = it.simpleName.asString()
            if (type.isPrimitive()) {
                tmpMap[propName] = PrimitivePropMetaData(propName, it, type.toPrimitiveType())
                return@forEach
            }

            val propMetaData: AbstractPropMetadata = when (type.declaration.typeParameters.size) {
                0 -> {// object prop
                    processObjectProp(it, type)
                }

                1 -> {// list prop
                    processListProp(it, type)
                }

                2 -> {// map prop
                    processMapProp(it, type)
                }

                else -> {
                    throw IllegalArgumentException("impossible review code")
                }

            }
            tmpMap[propName] = propMetaData
        }

        return emptyMap()
    }

    private fun processListProp(propDec: KSPropertyDeclaration, listType: KSType): ListPropMetaData {
        val elementType = listType.arguments[0].type!!.resolve()
        return if (elementType.isPrimitive()) {
            ListPrimitivePropMetaData(propDec.simpleName.asString(), propDec, elementType.toPrimitiveType())
        } else {
            ListObjectPropMetaData(
                propDec.simpleName.asString(),
                propDec,
                elementType.declaration as KSClassDeclaration
            )
        }
    }

    private fun processMapProp(propDec: KSPropertyDeclaration, mapType: KSType): MapPropMetaData {
        TODO()
    }

    private fun processObjectProp(propDec: KSPropertyDeclaration, objectType: KSType): ObjectPropMetaData {
        TODO()
    }

}