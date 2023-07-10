package m_serialization.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.toClassName
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

                    val classDecOfType = type.declaration as KSClassDeclaration
                    if (classDecOfType.classKind == ClassKind.ENUM_CLASS) {
                        processEnumProp(it, type, classDecOfType)
                    } else {
                        processObjectProp(it, type)
                    }
                }

                1 -> {// list prop
                    processListProp(it, type)
                }

                2 -> {// map prop
                    processMapProp(it, type)
                }

                else -> {
                    throw IllegalArgumentException("impossible, review code")
                }

            }
            tmpMap[propName] = propMetaData
        }
        return tmpMap
    }

    private fun processListProp(propDec: KSPropertyDeclaration, listType: KSType): ListPropMetaData {
        val elementType = listType.arguments[0].type!!.resolve()
        return if (elementType.isPrimitive()) {
            ListPrimitivePropMetaData(
                propDec.simpleName.asString(),
                propDec,
                elementType.toPrimitiveType(),
                listType
            )
        } else {

            val classDecOfElement = elementType.declaration as KSClassDeclaration
            if (classDecOfElement.classKind == ClassKind.ENUM_CLASS) {
                ListEnumPropMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    classDecOfElement,
                    listType
                )
            } else {
                ListObjectPropMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    classDecOfElement,
                    listType
                )
            }

        }
    }

    private fun processMapProp(propDec: KSPropertyDeclaration, mapType: KSType): MapPropMetaData {
        val keyType = mapType.arguments[0].type!!.resolve()
        val valueType = mapType.arguments[1].type!!.resolve()

        val mapTypeAtSource = MapTypeAtSource.fromType(mapType)


        val keyClassDec = keyType.declaration as KSClassDeclaration

        val valueClassDec = valueType.declaration as KSClassDeclaration


        return if (keyClassDec.classKind == ClassKind.ENUM_CLASS) {
            if (valueType.isPrimitive()) {
                MapEnumKeyPrimitiveValuePropMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    keyClassDec,
                    valueType.toPrimitiveType(),
                    mapTypeAtSource
                )
            } else if (valueClassDec.classKind == ClassKind.ENUM_CLASS) {
                MapEnumKeyEnumValue(
                    propDec.simpleName.asString(),
                    propDec,
                    keyClassDec,
                    valueClassDec,
                    mapTypeAtSource
                )
            } else {
                MapEnumKeyObjectValuePropMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    keyClassDec,
                    valueClassDec,
                    mapTypeAtSource
                )
            }
        } else if (keyType.isPrimitive()) {

            val primitiveKeyType = keyType.toPrimitiveType()

            if (valueClassDec.classKind == ClassKind.ENUM_CLASS) {
                MapPrimitiveKeyEnumValue(
                    propDec.simpleName.asString(),
                    propDec,
                    primitiveKeyType,
                    valueClassDec,
                    mapTypeAtSource
                )
            } else if (valueType.isPrimitive()) {
                MapPrimitiveKeyValueMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    primitiveKeyType,
                    valueType.toPrimitiveType(),
                    mapTypeAtSource
                )
            } else {
                MapPrimitiveKeyObjectValueMetaData(
                    propDec.simpleName.asString(),
                    propDec,
                    primitiveKeyType,
                    valueClassDec,
                    mapTypeAtSource
                )
            }
        } else {
            throw IllegalArgumentException("impossible, review code")
        }


    }

    private fun processObjectProp(propDec: KSPropertyDeclaration, objectType: KSType): ObjectPropMetaData {
        return ObjectPropMetaData(propDec.simpleName.asString(), propDec, objectType.declaration as KSClassDeclaration)
    }


    private fun processEnumProp(
        propDec: KSPropertyDeclaration,
        objectType: KSType,
        enumClass: KSClassDeclaration
    ): EnumPropMetaData {
        return EnumPropMetaData(propDec.simpleName.asString(), propDec, enumClass)
    }


    fun KSClassDeclaration.getSerializerObjectName(): String {
        return this.simpleName.asString() + AbstractPropMetadata.serializerObjectNameSuffix
    }

    // chỉ áp dụng cho object
    fun KSClassDeclaration.getWriteObjectStatement(bufferVarName: String, objectVarName: String): String {
        val serializerObjectName = getSerializerObjectName()
        val format = "%s.${getFunctionNameWriteInternal()}(%s,%s)"
        return String.format(format, serializerObjectName, objectVarName, bufferVarName)
    }

    // chỉ áp dụng cho object
    fun KSClassDeclaration.importSerializer(): List<String> {
        val packageName = this.packageName.asString()
        return listOf(
            packageName + "." + getSerializerObjectName()
        )
    }


    // lấy tất cả các con không phải là sealed của một class
    // sẽ duyệt đệ quy đến cháu, chắt, etc...
    fun KSClassDeclaration.getAllActualChild(): List<KSClassDeclaration> {
        val queue = LinkedList<KSClassDeclaration>()
        val result = mutableSetOf<KSClassDeclaration>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val allDirectChild = current.getSealedSubclasses().toList()
            for (child in allDirectChild) {
                if (child.modifiers.contains(Modifier.SEALED)) {
                    queue.add(child)
                } else {
                    result.add(child)
                }
            }
        }
        return result.toList()
    }


    fun KSClassDeclaration.getAllEnumEntrySimpleName(): List<String> {
        val result = mutableListOf<String>()
        declarations.forEach {
            if (it is KSClassDeclaration) {
                //logger.warn("entry of ${this.qualifiedName!!.asString()} is ${it.qualifiedName!!.asString()}")
                result.add(it.simpleName.asString())
            }
        }
        return result
    }

    fun KSClassDeclaration.getAllEnumEntryWithIndex(): List<Pair<String,Int>> {
        val result = mutableListOf<Pair<String,Int>>()
        declarations.forEach {
            if (it is KSClassDeclaration) {
                //logger.warn("entry of ${this.qualifiedName!!.asString()} is ${it.qualifiedName!!.asString()}")
                result.add(Pair(it.simpleName.asString(), result.size))
            }
        }
        return result
    }


    fun KSClassDeclaration.getFunctionNameWriteInternal(): String {
        return writeToInternal + simpleName.asString()
    }

    fun KSClassDeclaration.getSuperClass(): KSType {
        return this.superTypes.first {  !it.javaClass.isInterface }.resolve()
    }
    fun KSClassDeclaration.getSuperClassNameJS():String{
       val sup = this.getSuperClass()
        if(sup.toClassName().toString() == "kotlin.Any")
            return "JavaClass"
        return sup.toClassName().toString();
    }

    // nó sẽ gọi hàm writeToInternal của class đó
    val writeTo = "writeTo"// người dùng call, và là extension function


    // xem xét có thêm nối tên class vào để tránh nhầm lẫn
    private const val writeToInternal = "writeToInternal"

}