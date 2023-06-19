package m_serialization

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import m_serialization.data.PrimitiveType.Companion.isPrimitive
import m_serialization.utils.GraphUtils
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllChildRecursive
import org.apache.commons.lang3.mutable.MutableShort
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files


enum class GenericTypeSupport {
    LIST,
    MAP
}

class MSerializationSymbolProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = env.logger

    private var invoke = false


    // tạm thời chưa hỗ trợ object làm key
    // tất cả các key phải là primitive
    // xem có thể hỗ trợ trong tương lai
    private val fullNameToTypeGenericSupport: Map<String, GenericTypeSupport> = mapOf(
        "kotlin.collections.MutableList" to GenericTypeSupport.LIST,
        "java.util.LinkedList" to GenericTypeSupport.LIST,
        "kotlin.collections.List" to GenericTypeSupport.LIST,
        "kotlin.collections.MutableMap" to GenericTypeSupport.MAP,
        "kotlin.collections.Map" to GenericTypeSupport.MAP,
        "java.util.TreeMap" to GenericTypeSupport.MAP
    )


    init {
        KSClassDecUtils.logger = logger
        logger.warn("init ksp logic")
    }


    override fun process(resolver: Resolver): List<KSAnnotated> {

        if (!invoke) {
            invoke = true
        } else {
            return emptyList()
        }

        val allClassWillProcess = resolver.getSymbolsWithAnnotation(
            MSerialization::class
                .qualifiedName
                .toString()
        )

        val setAllClass = allClassWillProcess
            .map { it as KSClassDeclaration }
            .toSet()
        setAllClass
            .asSequence()
            .map {

                val numTypeParam = it.typeParameters.size
                if (numTypeParam > 0) {
                    logger.error("class ${it.qualifiedName?.asString()} had type param, can not serializable")
                }

                it
            }
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
            .map {
                verifySealedClass(it)
                it
            }
            .map {
                verifyPrimitiveNotNull(it)
                it
            }
            .forEach { _ -> }


        // tạo graph

        val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
        // add tất cả các class
        setAllClass.forEach {
            graph.addVertex(it.qualifiedName!!.asString())
        }


        setAllClass
            .asSequence()
            .map {
                Pair(it.qualifiedName!!.asString(), it)
            }.forEach { (name, classDec) ->
                val allDependencies = classDec.getAllDirectDependencies()

                val allName = allDependencies.map {
                    it.qualifiedName!!.asString()
                }

                //logger.warn("all dep of $name is $allName")

                allDependencies.forEach {
                    graph.addEdge(name, it.qualifiedName!!.asString())
                }
            }

        val exporter: DOTExporter<String, DefaultEdge> = DOTExporter<String, DefaultEdge>()

        exporter.setVertexAttributeProvider {
            val map: MutableMap<String, Attribute> = LinkedHashMap<String, Attribute>()
            map["label"] = DefaultAttribute.createAttribute(it)
            map
        }

        val writer: Writer = StringWriter()
        exporter.exportGraph(graph, writer)


        val graphStr = writer.toString()

        val file = File(System.getProperty("user.dir") + "/relation.gv")
        if (file.exists()) {
            file.delete()
        }

        Files.write(file.toPath(), graphStr.toByteArray(StandardCharsets.UTF_8))

        logger.warn("class relationship write to ${file.absolutePath}")


        val allCycle = GraphUtils.findCycle(graph)

        if (allCycle.isNotEmpty()) {
            logger.error("cyclic reference detected $allCycle")
        }

        // check

        // gen code

        // gen kt
        // khi gen đến code class nào
        // thì coi như các class dependencies của nó đã được gen rồi

        // sinh tag
        val autoTag = MutableShort()
        //env.codeGenerator.
        val fileSpec = FileSpec.builder("pack.protocols", "S")

        fileSpec.build().writeTo(env.codeGenerator, Dependencies(true))



        return emptyList()
    }


    private fun KSClassDeclaration.getAllDirectDependencies(): List<KSClassDeclaration> {
        val result = getAllProperties()
            .filter {
                it.hasBackingField
            }
            // lọc transient
            .filter {
                !it.getAllAnnotationName().contains(MTransient::class.java.name)
            }
            // lọc primitive
            .filter {
                val type = it.type.resolve()
                !type.isPrimitive()
            }
            // lọc những thằng list primitive và map primitive
            .filter {
                val type = it.type.resolve()
                val allTypeParam = type.arguments
                if (allTypeParam.isEmpty()) {
                    true
                } else {
                    val r = when (allTypeParam.size) {
                        1 -> { // list
                            val listElement = allTypeParam[0].type
                            !listElement!!.resolve().isPrimitive()
                        }

                        2 -> { // map
                            val valueElementType = allTypeParam[1].type
                            !valueElementType!!.resolve().isPrimitive()
                        }

                        else -> {
                            throw IllegalArgumentException("impossible, review code")
                        }
                    }
                    //logger.warn("r1 of ${it.simpleName.asString()} at ${qualifiedName?.asString()} is $r")
                    r
                }
            }.map {
                //map sang class
                //
                val type = it.type.resolve()
                val allTypeParam = type.arguments
                if (allTypeParam.isEmpty()) {
                    val classDec = it.type.resolve().declaration as KSClassDeclaration
                    Pair(it, classDec)
                } else {
                    val classDec = when (allTypeParam.size) {
                        1 -> {
                            allTypeParam[0].type!!.resolve().declaration as KSClassDeclaration
                        }

                        2 -> {
                            allTypeParam[1].type!!.resolve().declaration as KSClassDeclaration
                        }

                        else -> {
                            throw IllegalArgumentException("impossible, review code")
                        }
                    }
                    Pair(it, classDec)
                }

            }
            .flatMap { (prop, classDec) ->
                val allModifier = classDec.modifiers
                if (allModifier.contains(Modifier.SEALED)) {
                    val ss = classDec.getAllChildRecursive(this, prop.simpleName.asString())
                    logger.warn(
                        "context is ${qualifiedName?.asString()}, " +
                                "prop ${prop.simpleName.asString()}," +
                                " all child of ${classDec.qualifiedName?.asString()} is ${ss.size}"
                    )
                    ss.asSequence()
                } else {
                    sequenceOf(classDec)
                }
            }.toList()


        return result;
    }


    private fun verifyPrimitiveNotNull(classDec: KSClassDeclaration) {
        if (classDec.modifiers.contains(Modifier.SEALED)) return
        classDec
            .getAllProperties()
            .filter {
                it.hasBackingField
            }
            .filter {
                !it.getAllAnnotationName()
                    .contains(MTransient::class.java.name)
            }
            .map {
                Pair(it.type.resolve(), it)
            }.filter {
                it.first.isPrimitive()
            }
            .forEach { (type, prop) ->
                if (type.isMarkedNullable) {
                    val propName = prop.simpleName.asString()
                    val containerClassName = classDec.qualifiedName?.asString()
                    logger.error("prop $propName at class $containerClassName is primitive so can not nullable")
                }
            }
    }

    private fun verifySealedClass(classDec: KSClassDeclaration) {

        // không chứa sealed
        // nếu là open thì không được
        // nếu là abstract thì không được
        // nếu là interface thì không được

        val className = classDec.qualifiedName?.asString()
        if (!classDec.modifiers.contains(Modifier.SEALED)) {

            if (classDec.classKind == ClassKind.INTERFACE) {
                logger.error("class $className is open interface, can not serialize")
            } else {
                val allModifier = classDec.modifiers
                if (allModifier.contains(Modifier.ABSTRACT) || allModifier.contains(Modifier.OPEN)) {
                    logger.error("class $className is open, can not serialize")
                }
            }

        } else {
            val allDirectChild = classDec.getSealedSubclasses()
            val childNotSerializable = allDirectChild
                .filter {
                    !it.getAllAnnotationName().contains(MSerialization::class.java.name)
                }
                .map { it.qualifiedName!!.asString() }
                .toList()
            if (childNotSerializable.isNotEmpty()) {
                logger.error("child/children $childNotSerializable of $className is not serializable")
            }
        }
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

                if (type.isMarkedNullable) {
                    logger.error("generic prop $propName at $containerClassName can not be null")
                }

                val typeGenericTypeSupport = fullNameToTypeGenericSupport[classNameOfProp]
                    ?: throw IllegalArgumentException("prop $propName at class $containerClassName not serializable")


                // check tham số kiểu của khai báo
                //classDecOfProp.as


                fun KSType.isPrimitiveOrSerializable(): Boolean {
                    return if (isPrimitive()) {
                        true
                    } else {
                        val classDec = declaration as KSClassDeclaration
                        classDec.getAllAnnotationName().contains(MSerialization::class.java.name)
                    }
                }

                val allElementValid: Boolean = when (typeGenericTypeSupport) {
                    GenericTypeSupport.LIST -> {
                        val classOfElement = type.arguments[0].type!!.resolve()
                        classOfElement.isPrimitiveOrSerializable()
                    }

                    GenericTypeSupport.MAP -> {
                        val keyClass = type.arguments[0].type!!.resolve()
                        val valueClass = type.arguments[1].type!!.resolve()


                        if (keyClass.isMarkedNullable) {
                            logger.error("map prop $propName at $containerClassName had key nullable")
                        }

                        if (valueClass.isMarkedNullable) {
                            logger.error("map prop $propName at $containerClassName had value nullable")
                        }

                        keyClass.isPrimitive() && valueClass.isPrimitiveOrSerializable()

                    }
                }
                if (!allElementValid) {
                    logger.error("prop $propName at class $containerClassName had element not serializable")
                }
            }
    }


    // tất cả các tham số trong constructor phải là var hoặc val
    private fun verifyClassConstructor(clazz: KSClassDeclaration) {

        if (clazz.classKind == ClassKind.INTERFACE) {
            return
        }

        val primaryConstructor = clazz.primaryConstructor
        val className = clazz.qualifiedName?.asString()
        primaryConstructor
            ?: throw IllegalArgumentException("class $className not had primary constructor")

        val allParams = primaryConstructor.parameters
        if (allParams.isNotEmpty()) {
            val allParamIsProp = allParams.all {
                it.isVal || it.isVar
            }
            if (!allParamIsProp) throw IllegalArgumentException("class $className at primary constructor had a param not a property")
        }

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
                !prop.getAllAnnotationName().contains(MTransient::class.java.name)
            }
            .forEach { (prop, _, classDec) ->
                // check class khai báo phải có tag MSerializable ở class khai báo
                val allAnnoName = classDec.getAllAnnotationName()

                val propName = prop.simpleName.asString()
                val clazzName = clazz.qualifiedName?.asString()

                if (allAnnoName.isNotEmpty()) {
                    //val valid = allAnnoName.contains("m_serialization.annotations.MSerialization")
                    val valid = allAnnoName.contains(MSerialization::class.java.name)
                    if (valid) {
                        //logger.warn("prop ${prop.simpleName.asString()} at ${clazz.qualifiedName?.asString()} valid")
                    } else {
                        logger.error("prop $propName at $clazzName is not serializable")
                    }
                } else {
                    logger.error("prop $propName at $clazzName is not serializable")
                }
            }
    }

    // tất cả các prop có MTransient ở constructor của class này phải có giá trị mặc định
    // tất cả các prop transient trong constructor phải được đặt ở cuối -> đơn giản hoá việc gọi constructor lúc gen code
    private fun verifyAllTransientProp(clazz: KSClassDeclaration) {

        if (clazz.classKind == ClassKind.INTERFACE) {
            return
        }

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
                logger.error("class ${clazz.qualifiedName?.asString()} had some transient properties in constructor not at last position")
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