package m_serialization

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import m_serialization.annotations.MSerialization
import m_serialization.annotations.MTransient
import m_serialization.data.class_metadata.CommonPropForMetaCodeGen
import m_serialization.data.class_metadata.JSGenClassMetaData
import m_serialization.data.class_metadata.KotlinGenClassMetaData
import m_serialization.data.prop_meta_data.AbstractPropMetadata
import m_serialization.data.prop_meta_data.PrimitiveType
import m_serialization.data.prop_meta_data.PrimitiveType.Companion.isPrimitive
import m_serialization.data.prop_meta_data.PrimitiveType.Companion.toPrimitiveType
import m_serialization.utils.GraphUtils
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllAnnotationName
import m_serialization.utils.KSClassDecUtils.getAllChildRecursive

import m_serialization.utils.KSClassDecUtils.getAllPropMetaData
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

    override fun finish() {
        super.finish()
        JSGenClassMetaData.save(env.codeGenerator)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {


        /*val testClass = resolver.getSymbolsWithAnnotation(
            TestEnum::class.qualifiedName.toString()
        ).toList()


        if (testClass.isNotEmpty()) {

            testClass.forEach{
                val cc = it as KSClassDeclaration
                cc.getAllEnumEntrySimpleName();
            }


            return emptyList()
        }*/


        val allClassWillProcess = resolver.getSymbolsWithAnnotation(
            MSerialization::class
                .qualifiedName
                .toString()
        )

        val setAllClass = allClassWillProcess
            .map { it as KSClassDeclaration }
            .toSet()


        //val allEnumClass = mutableListOf<KSClassDeclaration>()

        setAllClass
            .asSequence()
            /*.map { c ->
                if (c.classKind == ClassKind.ENUM_CLASS) {
                    throwErr("temporary not support enum class ${c.qualifiedName?.asString()}")
                }
                c
            }*/
            .map {

                val numTypeParam = it.typeParameters.size
                if (numTypeParam > 0) {
                    throwErr("class ${it.qualifiedName?.asString()} had type param, can not serializable")
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
        // add tất cả các class làm đỉnh
        setAllClass.forEach {
            graph.addVertex(it.qualifiedName!!.asString())
        }


        setAllClass
            .asSequence()
            .map {
                Pair(it.qualifiedName!!.asString(), it)
            }.forEach { (name, classDec) ->
                val allDependencies = classDec.getAllDirectDependencies()
                allDependencies.forEach {
                    graph.addEdge(name, it.qualifiedName!!.asString())
                }
            }


        /*allEnumClass.forEach {

            val allEntry = it.declarations
            val listName = allEntry
                .map { e -> e::class }
                .toList()


            logger.warn("list name is $listName")


            //val allChild = it.
            *//*val allName = allChild
                .map { entry ->
                    entry.simpleName.asString()
                }.toList()
            logger.warn("class ${it.qualifiedName?.asString()} had $allName")*//*
        }*/

        exportDependenciesGraph(graph)
        val allCycle = GraphUtils.findCycle(graph)
        if (allCycle.isNotEmpty()) {
            //logger.error("cyclic reference detected $allCycle")
            throwErr("cyclic reference detected $allCycle")
        }

        // check

        // gen code

        // gen kt
        // khi gen đến code class nào
        // thì coi như các class dependencies của nó đã được gen rồi

        // sinh tag
        val autoTag = MutableShort()


        val classDecToUniqueTag = setAllClass
            .asSequence()
            .map {
                val tag = autoTag.andIncrement
                Pair(it, tag)
            }.toMap()

        setAllClass
            .asSequence()
            .map {
                Pair(it, it.getAllPropMetaData())
            }.map { it ->
                val (classDec, allPropMeta) = it

                // xử lý constructor
                val tmpMap = mutableMapOf<String, AbstractPropMetadata>()
                tmpMap.putAll(allPropMeta)


                val constructor = classDec.primaryConstructor


                val listPropInConstructor = mutableListOf<AbstractPropMetadata>()
                val listPropNotInConstructor = mutableListOf<AbstractPropMetadata>()

                constructor!!.parameters.forEach { param ->
                    val propName = param.name!!.asString()
                    val propMeteData = tmpMap.remove(propName)
                    if (propMeteData != null) {
                        listPropInConstructor.add(propMeteData)
                    }
                }

                tmpMap.forEach { (_, prop) ->
                    listPropNotInConstructor.add(prop)
                }

                listPropNotInConstructor.forEach { propMeta ->
                    if (!propMeta.propDec.isMutable) {
                        throwErr(
                            "prop ${propMeta.name} at ${classDec.qualifiedName!!.asString()} is not in constructor so can not be immutable"
                        )
                    }
                }


                val kotlinCodeGen = KotlinGenClassMetaData(logger)
                val jsCodeGen = JSGenClassMetaData(logger)


                //val m = MyCodeGen(listPropInConstructor, listPropNotInConstructor, it.first)
                //todo: add other code gen here
                //  c++, gdscript, c#


                val commonProp = CommonPropForMetaCodeGen(
                    listPropInConstructor,
                    listPropNotInConstructor,
                    classDec,
                    classDecToUniqueTag.getValue(classDec),
                    classDecToUniqueTag
                )

                Pair(listOf(kotlinCodeGen, jsCodeGen), commonProp)


            }
            .forEach { (list, commonProp) ->
                list.forEach { metaCodeGen ->

                    metaCodeGen.constructorProps = commonProp.constructorProps
                    metaCodeGen.otherProps = commonProp.otherProps
                    metaCodeGen.classDec = commonProp.classDec
                    metaCodeGen.protocolUniqueId = commonProp.protocolUniqueId
                    metaCodeGen.classDec = commonProp.classDec


                    metaCodeGen.doGenCode(env.codeGenerator)
                }
            }
        return emptyList()
    }

    private fun exportDependenciesGraph(graph: DefaultDirectedGraph<String, DefaultEdge>) {
        val exporter: DOTExporter<String, DefaultEdge> = DOTExporter<String, DefaultEdge>()

        exporter.setVertexAttributeProvider {
            val map: MutableMap<String, Attribute> = LinkedHashMap()
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
                    /*logger.warn(
                        "context is ${qualifiedName?.asString()}, " +
                                "prop ${prop.simpleName.asString()}," +
                                " all child of ${classDec.qualifiedName?.asString()} is ${ss.size}"
                    )*/
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
                    throwErr("prop $propName at class $containerClassName is primitive so can not nullable")
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
                throwErr("class $className is open interface, can not serialize")
            } else {
                val allModifier = classDec.modifiers
                if (allModifier.contains(Modifier.ABSTRACT) || allModifier.contains(Modifier.OPEN)) {
                    throwErr("class $className is open, can not serialize")
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
                throwErr("child/children $childNotSerializable of $className is not serializable")
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
                    throwErr("generic prop $propName at $containerClassName can not be null")
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
                        if (classOfElement.isMarkedNullable) {
                            throwErr("list prop $propName at $containerClassName had element nullable")
                        }
                        classOfElement.isPrimitiveOrSerializable()
                    }

                    GenericTypeSupport.MAP -> {
                        val keyClass = type.arguments[0].type!!.resolve()
                        val valueClass = type.arguments[1].type!!.resolve()


                        if (keyClass.isMarkedNullable) {
                            throwErr("map prop $propName at $containerClassName had key nullable")
                        }

                        if (valueClass.isMarkedNullable) {
                            throwErr("map prop $propName at $containerClassName had value nullable")
                        }


                        if (keyClass.isPrimitive()) {
                            if (keyClass.toPrimitiveType() == PrimitiveType.BYTE_ARRAY) {
                                throwErr("key element at prop $propName at $containerClassName can not be byte array")
                            }
                        } else {
                            val classDecOfKeyClass = keyClass.declaration as KSClassDeclaration
                            if (classDecOfKeyClass.classKind != ClassKind.ENUM_CLASS) {
                                throwErr("key element at prop $propName at $containerClassName can not be object")
                            } else {
                                val allAnnoName = classDecOfKeyClass.getAllAnnotationName()
                                if (!allAnnoName.contains(MSerialization::class.java.name)) {
                                    throwErr("key element at prop $propName at $containerClassName can not serializable")
                                }

                            }
                        }
                        valueClass.isPrimitiveOrSerializable()

                    }
                }
                if (!allElementValid) {
                    throwErr("prop $propName at class $containerClassName had element not serializable")
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

    private fun throwErr(msg: String) {
        logger.error(msg)
        throw IllegalArgumentException(msg)
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

                //logger.warn("verifyAllPropNotGenericsSerializable cc ${classDec.simpleName.asString()}")

                if (allAnnoName.isNotEmpty()) {
                    //val valid = allAnnoName.contains("m_serialization.annotations.MSerialization")
                    val valid = allAnnoName.contains(MSerialization::class.java.name)
                    if (valid) {
                        //logger.warn("prop ${prop.simpleName.asString()} at ${clazz.qualifiedName?.asString()} valid")
                    } else {
                        throwErr("prop $propName at $clazzName is not serializable")

                    }
                } else {
                    throwErr("prop $propName at $clazzName is not serializable")
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
            throwErr("some prop transient at ${clazz.qualifiedName?.asString()} had not default value")
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
                throwErr("class ${clazz.qualifiedName?.asString()} had some transient properties in constructor not at last position")
            }
        }

    }


}

data class TempPropData(val name: String, val hadDefaultValue: Boolean, var hadTransient: Boolean = false)