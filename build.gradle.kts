// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
}

allprojects {
    beforeEvaluate {
        // framework.jar 路径
        val path = rootDir.absolutePath + "/app/libs/framework-14.jar"
        tasks.withType<JavaCompile> {
            // 低版本 gradle 的方案
            options.compilerArgs.add("-Xbootclasspath/p:$path")
            // 高版本 gradle 的方案
            val newFileList = mutableListOf<File>()
            newFileList.add(File(path))
            options.bootstrapClasspath?.files?.let { oldFileList ->
                newFileList.addAll(oldFileList)
            }
            options.bootstrapClasspath = files(*newFileList.toTypedArray())
        }
    }
}

