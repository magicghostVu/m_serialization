package m_serialization

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import m_serialization.data.PrimitiveType.Companion.isPrimitive


class MSerializationSymbolProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = env.logger


    // tạm thời chưa hỗ trợ object làm key
    // tất cả các key phải là primitive
    // xem có thể hỗ trợ trong tương lai
    val setClassGenericsAccept: Set<String> = setOf(
        "kotlin.collections.MutableList",
        "java.util.LinkedList",
        "kotlin.collections.List",
        "kotlin.collections.MutableMap",
        "kotlin.collections.Map",
        "java.util.TreeMap"
    )


    init {
        logger.warn("init ksp logic")
    }


    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allClassWillProcess = resolver.getSymbolsWithAnnotation(
            MSerialization::class
                .qualifiedName
                .toString()
        )

        val setAllClass = allClassWillProcess
            .map { it as KSClassDeclaration }
            .toSet()

        /* allClassWillProcess.forEach {
             val classDeclaration = it as KSClassDeclaration

             val name = classDeclaration.qualifiedName?.asString()


             // get các props được khai báo tại class hiện tại
             //classDeclaration.getDeclaredProperties()


             // get các prop được khai báo cả ở class cha
             val allProps = classDeclaration.getAllProperties()

             val listAllPropData = mutableListOf<MPropData>()
             allProps.forEach { prop ->
                 listAllPropData.add(
                     MPropData(prop.simpleName.asString(), prop.hasBackingField)
                 )

                 // chú ý phương thức này để tìm kiểu của type parameter
                 //prop.asMemberOf()
                 //prop.asMemberOf()
             }

             //logger.warn("class $name, props $listAllPropData")
         }*/
        // verify source


        setAllClass
            .asSequence()
            .map {
                verifyAllPropNotGenericsSerializable(it)
                it
            }
            .map {
                verifyClassConstructor(it)
                it
            }
            .map {
                verifyAllTransientProp(it)
                it
            }
            .map {
                verifyGenericsProp(it)
                it
            }
            .forEach { _ -> }


        // gen code


        return emptyList()
    }

    //hỗ trợ map(MutableMap/Map ->HashMap, TreeMap)
    // list (LinkedList, List/MutableList -> mutableListOf())
    // tạm thời chưa hỗ trợ nested generics
    private fun verifyGenericsProp(clazz: KSClassDeclaration) {
        clazz.getAllProperties()
            // lọc transient
            .filter {
                val allAnno = it.getAllAnnotationName()
                !allAnno.contains(MTransient::class.java.name)
            }
            .map {
                val type = it.type.resolve()
                Pair(it, type)
            }
            .filter { (_, type) ->
                type.arguments.isNotEmpty()
            }
            .forEach { (prop, type) ->
                // check class của khai báo
                val classDecOfProp = type.declaration as KSClassDeclaration
                val propName = prop.simpleName.asString()
                val containerClassName = clazz.qualifiedName?.asString()
                val classNameOfProp = classDecOfProp.qualifiedName!!.asString()
                logger.warn("prop $propName at $containerClassName is $classNameOfProp")

                if (!setClassGenericsAccept.contains(classNameOfProp)) {
                    logger.error("prop $propName at class $containerClassName not serializable")
                }

                // check tham số kiểu của khai báo
                //classDecOfProp.as

            }
    }


    // tất cả các tham số trong constructor phải là var hoặc val
    private fun verifyClassConstructor(clazz: KSClassDeclaration) {
        val primaryConstructor = clazz.primaryConstructor
        val className = clazz.qualifiedName?.asString()
        primaryConstructor
            ?: throw IllegalArgumentException("class $className not had primary constructor")

        val allParams = primaryConstructor.parameters
        val allParamIsProp = allParams.all {
            it.isVal || it.isVar
        }
        if (!allParamIsProp) throw IllegalArgumentException("class $className at primary constructor had a param not a property")
    }

    // tất cả các prop(không có type param) phải là primitive hoặc là serializable hoặc là có MTransient

    private fun verifyAllPropNotGenericsSerializable(clazz: KSClassDeclaration) {
        val allProps = clazz.getAllProperties()
        allProps
            .filter {
                it.hasBackingField
            }
            .map {
                val type = it.type.resolve()
                Triple(it, type, type.declaration as KSClassDeclaration)
            }
            // lọc những thằng primitive ra
            .filter { (_, type, _) ->
                !type.isPrimitive()
            }
            // lọc những thằng có generics ra
            // những thằng có generic sẽ được check ở phase sau
            .filter { (_, type, _) ->
                type.arguments.isEmpty()
            }
            //lọc những thằng có MTransient
            .filter { (prop, _, _) ->
                val allAnnoThisProps = prop.annotations
                    .map { a ->
                        a.annotationType
                            .resolve()
                            .declaration
                            .qualifiedName!!.asString()
                    }.toSet()
                !allAnnoThisProps.contains(MTransient::class.java.name)
            }
            .forEach { (prop, _, classDec) ->
                // check class khai báo phải có tag MSerializable ở class khai báo
                val allAnnoName = classDec.getAllAnnotationName()

                if (allAnnoName.isNotEmpty()) {
                    //val valid = allAnnoName.contains("m_serialization.annotations.MSerialization")
                    val valid = allAnnoName.contains(MSerialization::class.java.name)
                    if (valid) {
                        //logger.warn("prop ${prop.simpleName.asString()} at ${clazz.qualifiedName?.asString()} valid")
                    } else {
                        logger.error("prop ${prop.simpleName.asString()} at ${clazz.qualifiedName?.asString()} is not serializable")
                    }
                } else {
                    logger.error("prop ${prop.simpleName.asString()} at ${clazz.qualifiedName?.asString()} is not serializable")
                }
            }
    }

    // tất cả các prop có MTransient ở constructor của class này phải có giá trị mặc định
    // tất cả các prop transient trong constructor phải được đặt ở cuối -> đơn giản hoá việc gọi constructor lúc gen code
    private fun verifyAllTransientProp(clazz: KSClassDeclaration) {
        val constructor = clazz.primaryConstructor!!

        // tại đây tất cả các param đều là prop rồi
        val allPropInConstructor = constructor
            .parameters
            .asSequence()
            .map {
                val name = it.name!!.asString()
                val hadDefaultValue = it.hasDefault
                TempPropData(name, hadDefaultValue)
            }
            .toList()


        val nameToProp = clazz
            .getAllProperties()
            .map {
                Pair(it.simpleName.asString(), it)
            }
            .toMap()

        allPropInConstructor
            .forEach {
                val prop = nameToProp.getValue(it.name)
                val hadTransient = prop.getAllAnnotationName().contains(MTransient::class.java.name)
                it.hadTransient = hadTransient
            }

        val allTransientHadDefault = allPropInConstructor
            .asSequence()
            .filter {
                it.hadTransient
            }
            .all { it.hadDefaultValue }
        if (!allTransientHadDefault) {
            logger.error("some prop transient at ${clazz.qualifiedName?.asString()} had not default value")
        }

        // check các prop transient phải ở cuối
        val firstIndexHadTransient = allPropInConstructor
            .withIndex()
            .asSequence()
            .filter {
                it.value.hadTransient
            }
            .firstOrNull()



        if (firstIndexHadTransient != null) {
            val index = firstIndexHadTransient.index
            val subList = allPropInConstructor.subList(index, allPropInConstructor.size)
            val anyLastNotTransient = subList.any { !it.hadTransient }
            if (anyLastNotTransient) {
                logger.error("class ${clazz.qualifiedName?.asString()} had some transient properties in constructor not last position")
            }
        }

    }

    private fun KSPropertyDeclaration.getAllAnnotationName(): Set<String> {
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

    private fun KSClassDeclaration.getAllAnnotationName(): Set<String> {
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


}

data class TempPropData(val name: String, val hadDefaultValue: Boolean, var hadTransient: Boolean = false)