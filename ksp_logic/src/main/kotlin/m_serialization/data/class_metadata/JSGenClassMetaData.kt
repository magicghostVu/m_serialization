package m_serialization.data.class_metadata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import m_serialization.data.prop_meta_data.*
import java.io.BufferedWriter

class JSGenClassMetaData(val logger: KSPLogger) : ClassMetaData() {
    companion object {
        var outputFile = JSFile(AbstractPropMetadata.serializerObjectNameSuffix)
        fun save(codeGenerator: CodeGenerator) {
            outputFile.writeTo(
                codeGenerator.createNewFile(
                    Dependencies(false),
                    "javascript", AbstractPropMetadata.serializerObjectNameSuffix, "js"
                ).bufferedWriter(Charsets.UTF_8)
            );
        }

    }

    override fun doGenCode(codeGenerator: CodeGenerator) {
        outputFile.addClass(JSClass(protocolUniqueId, classDec, constructorProps))
    }

}

class JSClass(val classId: Short, val classDec: KSClassDeclaration, val constructorProps: List<AbstractPropMetadata>) {
    fun bufferVar(): String {
        return "buffer";
    }

    fun rawClassName(): String {
        return "_${AbstractPropMetadata.serializerObjectNameSuffix}_${classId}";
    }

    fun rawFunName(): String {
        return classDec.simpleName.asString();
    }

    private fun rawFunName(classDec: KSClassDeclaration): String {
        return classDec.simpleName.asString()
    }

    private fun getExtractFunction(it: AbstractPropMetadata): String {
        return when (it) {
            is PrimitivePropMetaData -> getExtractPrimitive(it.type)
            is ListObjectPropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return this.${rawFunName(it.elementClass)}0(${bufferVar()})"
            }}.bind(this,${bufferVar()}))"
            is ListPrimitivePropMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return ${getExtractPrimitive(it.type)}"
            }}.bind(this,${bufferVar()}))"
            is MapPrimitiveValueMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType)},value:${getExtractPrimitive(it.valueType)}}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is MapObjectValueMetaData -> "Array(${bufferVar()}.readSize()).fill(0).map(function(${bufferVar()}){${
                "return {key:${getExtractPrimitive(it.keyType)},value: this.${rawFunName(it.valueClassDec)}0(${bufferVar()})}"
            }}.bind(this,${bufferVar()})).reduce(this.arrayToMap.bind(this), {})"
            is ObjectPropMetaData -> TODO()
        }
    }

    private fun getExtractPrimitive(it: PrimitiveType): String {
        return when (it) {
            PrimitiveType.BOOL -> "${bufferVar()}.readBool()"
            PrimitiveType.INT -> "${bufferVar()}.readInt()"
            PrimitiveType.SHORT -> "${bufferVar()}.readShort()"
            PrimitiveType.DOUBLE -> "${bufferVar()}.readDouble()"
            PrimitiveType.BYTE -> "${bufferVar()}.readByte()"
            PrimitiveType.FLOAT -> "${bufferVar()}.readFloat()"
            PrimitiveType.LONG -> "${bufferVar()}.readLong()"
            PrimitiveType.STRING -> "${bufferVar()}.readString()"
            PrimitiveType.BYTE_ARRAY -> "${bufferVar()}.readBytes()"
        }
    }

    fun writeUnpackFunction(file: BufferedWriter) {
        file.write(
            "${rawFunName()}0 :function(${bufferVar()}){ return ${rawFunName()}1(${
                constructorProps.map { getExtractFunction(it) }.joinToString(",")
            });}"
        )
    }

}

class JSFile(val fileName: String) {
    private var classes = mutableListOf<JSClass>()

    fun writeTo(file: BufferedWriter) {
        classes.forEach {
            it.writeUnpackFunction(file)
        }

        file.flush()
        file.close()
    }

    fun addClass(jsClass: JSClass) {
        this.classes.add(jsClass)
    }

}