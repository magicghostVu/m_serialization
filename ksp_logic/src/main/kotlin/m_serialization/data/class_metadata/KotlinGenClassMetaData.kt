package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import m_serialization.data.prop_meta_data.*
import m_serialization.utils.KSClassDecUtils
import m_serialization.utils.KSClassDecUtils.getAllActualChild
import m_serialization.utils.KSClassDecUtils.getFunctionNameWriteInternal
import m_serialization.utils.KSClassDecUtils.getSerializerObjectName
import m_serialization.utils.KSClassDecUtils.importSerializer


// mỗi class được đánh dấu là m_serialization sẽ sinh ra một object này
class KotlinGenClassMetaData() : ClassMetaData() {


    override fun doGenCode(codeGenerator: CodeGenerator) {
        val objectName = classDec.getSerializerObjectName()
        val fileBuilder = FileSpec.builder(
            classDec.packageName.asString(),
            objectName
        )
        val className = ClassName(classDec.packageName.asString(), classDec.simpleName.asString())
        val objectBuilder = TypeSpec.objectBuilder(objectName)


        if (!classDec.modifiers.contains(Modifier.SEALED)) {
            objectBuilder.addProperty(
                PropertySpec.builder("uniqueTag", Short::class.java)
                    .initializer("$protocolUniqueId")
                    .build()
            )
        }


        // chỉ gen get id và from id
        if (classDec.classKind == ClassKind.ENUM_CLASS) {


            /*val allEntry = classDec.getAllEnumEntrySimpleName()
            logger.warn("all entry of ${classDec.qualifiedName!!.asString()} is $allEntry")*/

            // gen code for enum
            val enumSimpleName = classDec.simpleName.asString()

            val typeNameForThisEnum = classDec.toClassName()

            val typeForMap = Map::class.asTypeName()
                .parameterizedBy(Short::class.asTypeName(), typeNameForThisEnum)



            objectBuilder.addProperty(
                PropertySpec
                    .builder(
                        "map",
                        typeForMap,
                        KModifier.PRIVATE
                    )
                    .initializer(
                        buildCodeBlock {
                            add(
                                "${enumSimpleName}.values()\n" +
                                        ".asSequence()\n" +
                                        ".associateBy { it.ordinal.toShort() }"
                            )
                        }
                    )
                    .build()
            )

            objectBuilder.addFunction(
                FunSpec.builder("toId")
                    .returns(Short::class)
                    .addParameter(
                        ParameterSpec
                            .builder("mEnum", typeNameForThisEnum)
                            .build()
                    )
                    .addStatement("return mEnum.ordinal.toShort()")
                    .build()
            )

            objectBuilder.addFunction(
                FunSpec.builder("fromId")
                    .returns(typeNameForThisEnum)
                    .addParameter(
                        ParameterSpec
                            .builder("id", Short::class)
                            .build()
                    )
                    .addStatement(
                        "return map.getValue(id)"
                    )
                    .build()
            )

            fileBuilder.addType(objectBuilder.build())
            fileBuilder.build().writeTo(codeGenerator, Dependencies(true))

        } else {
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


            val (funcCalSerializeSize, allImportSerializeSize) = genFuncSerializeSize(className)

            funcCalSerializeSize.forEach {
                objectBuilder.addFunction(it)
            }

            allImportSerializeSize.forEach {
                fileBuilder.addImport(it, "")
            }


            fileBuilder.addType(objectBuilder.build())
            fileBuilder.build().writeTo(codeGenerator, Dependencies(true))
        }
    }


    private fun genDeserializer(typeName: TypeName): Pair<List<FunSpec>, Set<String>> {


        val funcRead = FunSpec.builder(AbstractPropMetadata.readFromFuncName)
            .returns(typeName)
            .addParameter(
                ParameterSpec.builder("buffer", byteBufTypeName).build()
            )
        // switch case read theo các con
        return if (classDec.modifiers.contains(Modifier.SEALED)) {


            val allImport = mutableSetOf<String>()

            funcRead.addStatement("val className = \"${classDec.qualifiedName!!.asString()}\"")

            funcRead.addStatement("val tag = buffer.readShort()\n")
            val whenExpression = StringBuilder()

            whenExpression.append("val result = when(tag){\n")


            val allActualChild = classDec.getAllActualChild()
            allActualChild.forEach {
                allImport.addAll(it.importSerializer())

                val isExpressionFormat = "%s.uniqueTag -> %s.readFrom(buffer)"

                val isExpression = String.format(
                    isExpressionFormat,
                    it.getSerializerObjectName(),
                    it.getSerializerObjectName()
                )
                whenExpression.append("$isExpression\n");
            }

            whenExpression.append("else -> throw IllegalArgumentException(\"tag \$tag is not recognized for \$className\")\n")


            whenExpression.append("}\n")
            funcRead.addStatement(whenExpression.toString())

            funcRead.addStatement("return result\n")
            Pair(listOf(funcRead.build()), emptySet())
        } else {
            val readPropInConstructor = StringBuilder()
            //var indexMark = 0


            val allImport = mutableSetOf<String>()

            constructorProps.forEachIndexed { index, propMetaData ->
                val tmpVarName = "v$index"
                readPropInConstructor.append(
                    propMetaData.getReadStatement("buffer", tmpVarName, true)
                ).append("\n")

                allImport.addAll(propMetaData.addImportForRead())
            }

            // call constructor


            funcRead.addStatement(readPropInConstructor.toString())

            val allVarInConstructor = List(constructorProps.size) { index ->
                "v$index"
            }.joinToString(",")

            val callConstructorExpression = "val result =${classDec.simpleName.asString()}($allVarInConstructor)\n"

            funcRead.addStatement(callConstructorExpression)


            // re-assign other remain prop

            otherProps.forEach {
                val readForThisProp = it.getReadStatement(
                    "buffer",
                    "result.${it.name}",
                    false
                )
                funcRead.addStatement("$readForThisProp\n")
                allImport.addAll(it.addImportForRead())
            }


            funcRead.addStatement("return result;")
            Pair(listOf(funcRead.build()), allImport)
        }

    }


    private fun genFunctionSerializer(typeName: TypeName): Pair<List<FunSpec>, Set<String>> {

        val objectToWriteVarName = "objectToWrite"

        return if (!classDec.modifiers.contains(Modifier.SEALED)) {
            val funcWriteToInternal = FunSpec.builder(classDec.getFunctionNameWriteInternal())
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", byteBufTypeName).build()
                )
            val allImports = mutableSetOf<String>()

            for (prop in constructorProps) {
                funcWriteToInternal.addStatement(prop.getWriteStatement(objectToWriteVarName))
                allImports.addAll(prop.addImportForWrite())
            }
            for (prop in otherProps) {
                funcWriteToInternal.addStatement(prop.getWriteStatement(objectToWriteVarName))
                allImports.addAll(prop.addImportForWrite())
            }


            val funcWriteUserCall = FunSpec.builder(KSClassDecUtils.writeTo)
                .receiver(typeName)
                .addParameter(
                    ParameterSpec
                        .builder("buffer", byteBufTypeName)
                        .build()
                )
                .addStatement(
                    "${classDec.getFunctionNameWriteInternal()}(this, buffer)"
                )


            val funcWriteWithTag = FunSpec.builder("${classDec.getFunctionNameWriteInternal()}WithTag")
            funcWriteWithTag
                .addParameter(
                    ParameterSpec.builder(objectToWriteVarName, typeName).build()
                )
                .addParameter(
                    ParameterSpec.builder("buffer", byteBufTypeName).build()
                )

            funcWriteWithTag.addStatement(
                "buffer.writeShort(uniqueTag.toInt())"
            )
            funcWriteWithTag.addStatement(
                "${classDec.getFunctionNameWriteInternal()}($objectToWriteVarName,buffer)"
            )



            Pair(
                listOf(
                    funcWriteToInternal.build(),
                    funcWriteUserCall.build(),
                    funcWriteWithTag.build()
                ),
                allImports
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
                        .builder("buffer", byteBufTypeName)
                        .build()
                )

            val allImports = mutableSetOf<String>()

            //write tag


            val whenExpression = StringBuilder()
            whenExpression.append("when($objectToWriteVarName){\n")

            val allRealChild = classDec.getAllActualChild()
            allRealChild.forEach {
                val format = "is ${it.simpleName.asString()} -> %s\n"
                val callMethod = String.format(
                    format,
                    "${it.getSerializerObjectName()}.${it.getFunctionNameWriteInternal()}WithTag($objectToWriteVarName, buffer)\n"
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
                    ParameterSpec.builder("buffer", byteBufTypeName).build()
                )
                .addStatement("${classDec.getFunctionNameWriteInternal()}($objectToWriteVarName, buffer)")


            Pair(listOf(funWriteInternal.build(), funcWriteTo.build()), allImports)
        }
    }

    private fun genFuncSerializeSize(typeName: TypeName): Pair<List<FunSpec>, Set<String>> {
        return if (classDec.classKind == ClassKind.ENUM_CLASS) {
            Pair(emptyList(), emptySet())
        } else {

            // gen fun khác với func của các con
            // switch case các con
            if (classDec.modifiers.contains(Modifier.SEALED)) {
                val paramNames = "objectToCalSize"
                val funCalculateSerializeSpec = FunSpec
                    .builder(AbstractPropMetadata.serializeSizeFuncName)
                    .receiver(typeName)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                paramNames,
                                typeName
                            ).build()
                    )
                    .returns(Int::class)


                // gen when expression for all child

                val allImport = mutableSetOf<String>()

                val expressionBuilder = StringBuilder()

                val childSizeVarName = "childSize"

                expressionBuilder.append(
                    "val $childSizeVarName:Int = when($paramNames){\n"
                )

                classDec.getAllActualChild().forEach {
                    expressionBuilder.append(
                        "is ${it.simpleName.asString()} -> $paramNames.${AbstractPropMetadata.serializeSizeFuncName}()\n"
                    )
                    allImport.addAll(
                        it.importSerializer().map { import ->
                            "$import.${AbstractPropMetadata.serializeSizeFuncName}"
                        }
                    )
                }

                expressionBuilder.append("}")



                funCalculateSerializeSpec.addStatement(expressionBuilder.toString())

                funCalculateSerializeSpec.addStatement(
                    "return $childSizeVarName + 2"
                )

                Pair(listOf(funCalculateSerializeSpec.build()), allImport)
            } else {

                val allImport = mutableSetOf<String>()
                val funCalculateSerializeSpec = FunSpec
                    .builder(AbstractPropMetadata.serializeSizeFuncName)
                    .receiver(typeName)
                    .returns(Int::class)

                val allSizeVarName = mutableListOf<String>()

                val allProp = constructorProps + otherProps

                if (allProp.isNotEmpty()) {
                    allProp.forEachIndexed { index, prop ->
                        val sizeVarNameForThisProp = "s$index"
                        allSizeVarName.add(sizeVarNameForThisProp)
                        when (prop) {
                            is EnumPropMetaData -> {
                                funCalculateSerializeSpec.addStatement(
                                    "var $sizeVarNameForThisProp = 2//enum"
                                )
                            }

                            is PrimitivePropMetaData -> {
                                val expressionAndImport = prop.type.expressionAndImportForCalSerializeSize(
                                    sizeVarNameForThisProp,
                                    prop.name
                                )
                                funCalculateSerializeSpec.addStatement(expressionAndImport.first + "\n")
                                allImport.addAll(expressionAndImport.second)
                            }

                            is ListPropMetaData -> {
                                funCalculateSerializeSpec.addStatement(prop.expressionForCalSize(sizeVarNameForThisProp))
                                allImport.addAll(prop.addImportForCalculateSize())
                            }


                            is MapPropMetaData -> {
                                when (prop) {
                                    is MapEnumKeyEnumValue -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "val $sizeVarNameForThisProp = 2 + ${prop.name}.size * 4//map enum key value"
                                        )
                                    }

                                    is MapEnumKeyObjectValuePropMetaData -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "var $sizeVarNameForThisProp = 2"
                                        )
                                        funCalculateSerializeSpec.addStatement(
                                            "$sizeVarNameForThisProp += ${prop.name}.size * 2// key size"
                                        )
                                        val tempVarNameForIter = "v"
                                        val expressionCalSizeOfObject =
                                            if (prop.valueType.modifiers.contains(Modifier.SEALED)) {
                                                "$tempVarNameForIter.${AbstractPropMetadata.serializeSizeFuncName}($tempVarNameForIter)"
                                            } else {
                                                "$tempVarNameForIter.${AbstractPropMetadata.serializeSizeFuncName}()"
                                            }

                                        funCalculateSerializeSpec.addStatement(
                                            """
                                        for($tempVarNameForIter in ${prop.name}.values){ //value size 
                                            $sizeVarNameForThisProp += $expressionCalSizeOfObject
                                        }
                                    """.trimIndent()
                                        )

                                        allImport.addAll(
                                            prop.valueType.importSerializer().map {
                                                "$it.${AbstractPropMetadata.serializeSizeFuncName}"
                                            }
                                        )
                                    }

                                    is MapEnumKeyPrimitiveValuePropMetaData -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "var $sizeVarNameForThisProp = 2"
                                        )

                                        funCalculateSerializeSpec.addStatement(
                                            "$sizeVarNameForThisProp += 2 * ${prop.name}.size// key size"
                                        )

                                        when (val valueType = prop.valueType) {
                                            PrimitiveType.INT,
                                            PrimitiveType.SHORT,
                                            PrimitiveType.DOUBLE,
                                            PrimitiveType.BYTE,
                                            PrimitiveType.BOOL,
                                            PrimitiveType.FLOAT,
                                            PrimitiveType.LONG -> {
                                                val sizeSerialize = valueType.serializeSize()
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.size * $sizeSerialize//value size"
                                                )
                                            }

                                            PrimitiveType.STRING -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                for(e in ${prop.name}.values){
                                                    $sizeVarNameForThisProp += e.strSerializeSize()
                                                }
                                            """.trimIndent()
                                                )

                                                allImport.add(
                                                    "m_serialization.utils.ByteBufUtils.strSerializeSize"
                                                )
                                            }

                                            PrimitiveType.BYTE_ARRAY -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                for(e in ${prop.name}.values){
                                                    $sizeVarNameForThisProp += e.byteArraySerializeSize()
                                                }
                                            """.trimIndent()
                                                )

                                                allImport.add(
                                                    "m_serialization.utils.ByteBufUtils.byteArraySerializeSize"
                                                )
                                            }
                                        }

                                    }

                                    is MapPrimitiveKeyEnumValue -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "var $sizeVarNameForThisProp = 2// map size"
                                        )

                                        funCalculateSerializeSpec.addStatement(
                                            "$sizeVarNameForThisProp += ${prop.name}.size * 2 // value size"
                                        )


                                        val varNameForKeyIter = "k"

                                        when (val keyType = prop.keyType) {
                                            PrimitiveType.INT,
                                            PrimitiveType.SHORT,
                                            PrimitiveType.DOUBLE,
                                            PrimitiveType.BYTE,
                                            PrimitiveType.BOOL,
                                            PrimitiveType.FLOAT,
                                            PrimitiveType.LONG -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${keyType.serializeSize()} * ${prop.name}.size // key size"
                                                )
                                            }

                                            PrimitiveType.STRING -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                        for($varNameForKeyIter in ${prop.name}.keys){ //key size
                                                            $sizeVarNameForThisProp += ${varNameForKeyIter}.strSerializeSize()
                                                        }
                                                    """.trimIndent()
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.strSerializeSize")
                                            }

                                            PrimitiveType.BYTE_ARRAY -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                        for($varNameForKeyIter in ${prop.name}.keys){ // key size
                                                            $sizeVarNameForThisProp += ${varNameForKeyIter}.byteArraySerializeSize()
                                                        }
                                                    """.trimIndent()
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.byteArraySerializeSize")
                                            }
                                        }

                                    }

                                    is MapPrimitiveKeyObjectValueMetaData -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "var $sizeVarNameForThisProp = 2// map size"
                                        )
                                        when (val keyType = prop.keyType) {
                                            PrimitiveType.INT,
                                            PrimitiveType.SHORT,
                                            PrimitiveType.DOUBLE,
                                            PrimitiveType.BYTE,
                                            PrimitiveType.BOOL,
                                            PrimitiveType.FLOAT,
                                            PrimitiveType.LONG -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.size * ${keyType.serializeSize()} // key size"
                                                )
                                            }

                                            PrimitiveType.STRING -> {
                                                val varNameForKeyIter = "k"
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                        for($varNameForKeyIter in ${prop.name}.keys){ // key size
                                                            $sizeVarNameForThisProp += k.strSerializeSize()
                                                        }
                                                    """.trimIndent()
                                                )

                                                allImport.add("m_serialization.utils.ByteBufUtils.strSerializeSize")
                                            }

                                            PrimitiveType.BYTE_ARRAY -> {
                                                val varNameForKeyIter = "k"
                                                funCalculateSerializeSpec.addStatement(
                                                    """
                                                        for($varNameForKeyIter in ${prop.name}.keys){ // key size
                                                            $sizeVarNameForThisProp += k.byteArraySerializeSize()
                                                        }
                                                    """.trimIndent()
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.byteArraySerializeSize")
                                            }
                                        }
                                        val varNameForValueIter = "v"
                                        val expressionCalSizeOfObject =
                                            if (prop.valueClassDec.modifiers.contains(Modifier.SEALED)) {
                                                "$varNameForValueIter.${AbstractPropMetadata.serializeSizeFuncName}($varNameForValueIter)"
                                            } else {
                                                "$varNameForValueIter.${AbstractPropMetadata.serializeSizeFuncName}()"
                                            }

                                        funCalculateSerializeSpec.addStatement(
                                            """
                                        for($varNameForValueIter in ${prop.name}.values){ //value size 
                                            $sizeVarNameForThisProp += $expressionCalSizeOfObject
                                        }
                                    """.trimIndent()
                                        )
                                        allImport.addAll(
                                            prop.valueClassDec.importSerializer().map {
                                                "$it.${AbstractPropMetadata.serializeSizeFuncName}"
                                            }
                                        )
                                    }

                                    is MapPrimitiveKeyValueMetaData -> {
                                        funCalculateSerializeSpec.addStatement(
                                            "var $sizeVarNameForThisProp = 2// map size"
                                        )
                                        when (val keyType = prop.keyType) {
                                            PrimitiveType.INT,
                                            PrimitiveType.SHORT,
                                            PrimitiveType.DOUBLE,
                                            PrimitiveType.BYTE,
                                            PrimitiveType.BOOL,
                                            PrimitiveType.FLOAT,
                                            PrimitiveType.LONG -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${keyType.serializeSize()} * ${prop.name}.size // key type"
                                                )
                                            }

                                            PrimitiveType.STRING -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.keys.asSequence().map{it.strSerializeSize()}.sum() // key size"
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.strSerializeSize")
                                            }

                                            PrimitiveType.BYTE_ARRAY -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.keys.asSequence().map{it.byteArraySerializeSize()}.sum() // key size"
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.byteArraySerializeSize")
                                            }
                                        }

                                        when (val valueType = prop.valueType) {
                                            PrimitiveType.INT,
                                            PrimitiveType.SHORT,
                                            PrimitiveType.DOUBLE,
                                            PrimitiveType.BYTE,
                                            PrimitiveType.BOOL,
                                            PrimitiveType.FLOAT,
                                            PrimitiveType.LONG -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${valueType.serializeSize()} * ${prop.name}.size// value size"
                                                )
                                            }

                                            PrimitiveType.STRING -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.values.asSequence().map{it.strSerializeSize()}.sum() // key size"
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.strSerializeSize")
                                            }

                                            PrimitiveType.BYTE_ARRAY -> {
                                                funCalculateSerializeSpec.addStatement(
                                                    "$sizeVarNameForThisProp += ${prop.name}.values.asSequence().map{it.byteArraySerializeSize()}.sum() // key size"
                                                )
                                                allImport.add("m_serialization.utils.ByteBufUtils.byteArraySerializeSize")
                                            }
                                        }
                                    }
                                }
                            }

                            is ObjectPropMetaData -> {
                                // call serialize size của class này
                                funCalculateSerializeSpec.addStatement(
                                    prop.expressionForCalSize(sizeVarNameForThisProp)
                                )
                                allImport.addAll(prop.addImportForCalculateSize())
                            }
                        }
                    }
                    val sum = allSizeVarName.joinToString(" + ")
                    funCalculateSerializeSpec.addStatement("return $sum")
                    Pair(listOf(funCalculateSerializeSpec.build()), allImport)
                } else {
                    funCalculateSerializeSpec.addStatement("return 0")
                    Pair(listOf(funCalculateSerializeSpec.build()), allImport)
                }
            }
        }

    }


    companion object {


    }
}