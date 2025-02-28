package m_serialization.data.gen_protocol_version

import com.google.devtools.ksp.processing.CodeGenerator

interface IGenFileProtocolVersion {
    fun genFileProtocolVersion(codeGenerator: CodeGenerator, protocolVersion: Int)
}