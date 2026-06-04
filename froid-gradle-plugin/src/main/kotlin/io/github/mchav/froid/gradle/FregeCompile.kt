package io.github.mchav.froid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Runs the vendored Frege compiler (libs/frege-compiler-snapshot.jar) over a
 * module's `src/main/frege` tree, emitting Java into [outputDir]. AGP then
 * compiles that Java with javac as part of the variant. This replaces the old
 * froid.gradle hooks (dexOptions/applicationVariants/bootClasspath), all removed
 * in AGP 8.
 */
@CacheableTask
abstract class FregeCompile : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    /** The Frege compiler+runtime jar; also on the JVM classpath that runs it. */
    @get:Classpath
    abstract val fregeCompiler: ConfigurableFileCollection

    /** android.jar + library/compose deps, so Frege can resolve native types. */
    @get:Classpath
    abstract val compileClasspath: ConfigurableFileCollection

    @get:Input
    abstract val target: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun compile() {
        val out = outputDir.get().asFile
        // Clear stale generated Java (e.g. from a since-deleted .fr) — Frege's
        // -make leaves outputs for removed sources, which then break javac.
        out.deleteRecursively()
        out.mkdirs()
        val src = sourceDir.get().asFile

        val runClasspath = fregeCompiler + compileClasspath
        val fregePath = runClasspath.files.joinToString(System.getProperty("path.separator"))

        execOperations.javaexec {
            classpath = runClasspath
            mainClass.set("frege.compiler.Main")
            jvmArgs("-Xss8m")
            args(
                "-d", out.absolutePath,
                "-target", target.get(),
                "-make",
                "-sp", src.absolutePath,
                "-fp", fregePath,
                src.absolutePath,
            )
        }
    }
}
