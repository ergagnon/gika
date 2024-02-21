
import com.gika.rawtext.FileRequest
import com.gika.rawtext.RawTextGrpcKt
import com.gika.rawtext.RawTextReply
import com.gika.rawtext.rawTextReply
import com.google.protobuf.kotlin.toByteStringUtf8
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.tika.io.TikaInputStream
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.sax.WriteOutContentHandler
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PipedReader
import java.io.PipedWriter
import java.nio.CharBuffer

fun main(): Unit = runBlocking {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = GikaServer(port)
    server.start()
    server.blockUntilShutdown()
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
suspend fun parse(pi: PipedInputStream, pw: PipedWriter, metadata: org.apache.tika.metadata.Metadata) = coroutineScope {
    val parser = AutoDetectParser()
    val context = ParseContext()
    val handler = WriteOutContentHandler(pw)

    launch(newSingleThreadContext("Parse")) {
        parser.parse(TikaInputStream.get(pi), BodyContentHandler(handler), metadata, context)
        pi.close()
        pw.close()
    }
}

class GikaServer(private val port: Int) {
    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(RawTextService())
            .addService(ProtoReflectionService.newInstance())
            .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@GikaServer.stop()
                println("*** server shut down")
            },
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    internal class RawTextService : RawTextGrpcKt.RawTextCoroutineImplBase() {
        override fun extract(requests: Flow<FileRequest>): Flow<RawTextReply> =

            flow {
                coroutineScope {
                    val pipedOutputStream = PipedOutputStream()
                    val pipedWriter = PipedWriter()

                    val pipedInputStream = PipedInputStream(pipedOutputStream)
                    val pipedReader = PipedReader(pipedWriter)
                    val metadata = org.apache.tika.metadata.Metadata()
                    launch { parse(pipedInputStream, pipedWriter, metadata) }

                    launch {
                        requests.collect { data ->
                            withContext(Dispatchers.IO) {
                                pipedOutputStream.write(data.content.toByteArray())
                            }
                        }
                        pipedOutputStream.close()
                    }

                    val charBuffer = CharBuffer.allocate(1024)

                    while (pipedReader.read(charBuffer) != -1) {
                        charBuffer.flip()
                        val charArray = CharArray(charBuffer.remaining())
                        charBuffer.get(charArray)
                        val result = String(charArray)

                        emit(
                            rawTextReply {
                                content = result.toByteStringUtf8()
                                type = metadata.get("Content-Type")
                            }
                        )
                        charBuffer.clear()
                    }
                    pipedReader.close()
                }
            }
    }
}
