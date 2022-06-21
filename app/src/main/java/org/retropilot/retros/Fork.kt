package org.retropilot.retros

import org.kohsuke.github.GitHub
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.*

val gitHub: GitHub = GitHub.connectAnonymously()

val openpilotDataDir: Path = Path("/data/openpilot")
val openpilotBackupDataDir: Path = Path("/data/openpilot.bak")


@Throws(IOException::class)
fun unzip(inputStream: InputStream, destinationPath: Path) {
    /// Create destination path (does not throw if it already exists)
    destinationPath.createDirectories()

    /// Open zip file
    ZipInputStream(inputStream).use { stream ->
        /// Read each entry
        while (true) {
            val entry = stream.nextEntry ?: break

            /// Get entry path
            val entryPath = destinationPath / entry.name

            if (entry.isDirectory) {
                /// Create parent directories if needed
                entryPath.createDirectories()
            } else {
                /// Copy file stream to destination path
                entryPath.outputStream().use { output ->
                    stream.copyTo(output)
                }
            }
        }
    }
}

data class Fork(
    val username: String,
    val repository: String,
    val ref: String?,
)

fun Fork.install() {
    /// If an existing openpilot installation exists, move it to the backup path.
    /// This will overwrite any existing backup.
    if (openpilotDataDir.exists()) {
        openpilotDataDir.moveTo(openpilotBackupDataDir, overwrite = true)
    }

    try {
        /// Fetch the GitHub repository archive stream and unzip it to the openpilot data directory.
        gitHub
            .getRepository("${username}/${repository}")
            .readZip({ unzip(it, openpilotDataDir) }, ref)
    } catch (e: Exception) {
        println("Failed to install openpilot: ${e.message}")

        /// If an error occurs, move the backup back to the openpilot directory.
        openpilotBackupDataDir.moveTo(openpilotDataDir, overwrite = true)
    }
}
