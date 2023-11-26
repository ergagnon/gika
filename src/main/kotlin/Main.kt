
import kotlinx.coroutines.*
import org.apache.tika.io.TikaInputStream
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.apache.tika.sax.WriteOutContentHandler
import java.io.*
import java.nio.CharBuffer

fun main(): Unit = runBlocking {
    //val filePath = "C:\\Users\\egagn\\OneDrive\\Documents\\Offre Éric Gagnon 2015.docx" // Change this to the path of your file

    val pipedOutputStream = PipedOutputStream()
    val pipedInputStream = PipedInputStream(pipedOutputStream)

    launch { read(pipedOutputStream) }
    launch { parse(pipedInputStream) }


    /*try {
        FileInputStream(filePath).use { fileStream ->
            val parser = AutoDetectParser()
            val handler = BodyContentHandler()
            val metadata = org.apache.tika.metadata.Metadata()

            parser.parse(fileStream, handler, metadata)

            println("Content:\n${handler}")
            println("Metadata:")
            metadata.names().forEach { name ->
                println("$name: ${metadata.get(name)}")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }*/
}
suspend fun read(po: PipedOutputStream) = coroutineScope {
    val filePath = "C:\\Users\\egagn\\OneDrive\\Documents\\Offre Éric Gagnon 2015.docx" // Change this to the path of your file
    val chunkSize = 1024

    launch {
        try {
            FileInputStream(filePath).buffered().use { reader ->
                var bytesRead: Int
                val buffer = ByteArray(chunkSize)

                do {
                    bytesRead = reader.read(buffer, 0, chunkSize)

                    if (bytesRead > 0) {
                        val end = bytesRead - 1
                        val read = buffer.slice(0.rangeTo(end))
                        val ba = read.toByteArray()
                        po.write(ba)
                    }

                } while (bytesRead != -1)

                reader.close()
                po.close()

            }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
suspend fun parse(pi: PipedInputStream) = coroutineScope {

    val parser = AutoDetectParser()
    val metadata = org.apache.tika.metadata.Metadata()
    val context = ParseContext()
    val pipedWriter = PipedWriter()
    val pipedReader = PipedReader(pipedWriter)
    val handler = WriteOutContentHandler(pipedWriter)

    launch(newSingleThreadContext("Parse")) {
        println("Start parse")
        parser.parse(TikaInputStream.get(pi), BodyContentHandler(handler), metadata, context)
        println("End parse")
        pi.close()
        pipedWriter.close()
    }

    launch {
        println("Start read")
        val charBuffer = CharBuffer.allocate(1024)

        while (pipedReader.read(charBuffer) != -1) {
            println("Enter read loop")
            charBuffer.flip()
            val charArray = CharArray(charBuffer.remaining())
            charBuffer.get(charArray)
            val result = String(charArray)
            println(result)
            charBuffer.clear()
        }
        println("End read")
        pipedReader.close()
    }
}


/*fun main() {
    val inputStream = FileInputStream("C:\\Users\\egagn\\OneDrive\\Documents\\Offre Éric Gagnon 2015.docx")

    try {
        val chunkSize = 1024


        val parser = AutoDetectParser()
        val metadata = org.apache.tika.metadata.Metadata()
        val context = ParseContext()

        val buffer = ByteArray(chunkSize)
        var bytesRead = 0;

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val chunk = buffer.copyOfRange(0, bytesRead)

            val handler = BodyContentHandler()
            parser.parse(ByteArrayInputStream(chunk), handler, metadata, context)

            // Get the parsed content for this chunk
            val text = handler.toString()
            println("Text for this chunk: $text")

            // Process metadata if needed
            println("Metadata:")
            metadata.names().forEach { name ->
                println("$name: ${metadata.get(name)}")
            }

            // Clear metadata for the next iteration
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}*/