import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    tasks.withType(KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    plugins.withType(com.android.build.gradle.BasePlugin::class.java).configureEach {
        project.extensions.getByType<BaseExtension>().apply {
            setCompileSdkVersion(libs.versions.compileSdk.get().toInt())

            defaultConfig {
                versionCode = libs.versions.versionCode.get().toInt()
                versionName = libs.versions.versionName.get()
                minSdk = libs.versions.minSdk.get().toInt()
                targetSdk = libs.versions.targetSdk.get().toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
            composeOptions {
                kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
            }
        }
    }

    beforeEvaluate {
        // framework.jar 路径
        val path = rootDir.absolutePath + "/app/sdk/framework-14.jar"
        tasks.withType<JavaCompile> {
            // 低版本 gradle 的方案
            options.compilerArgs.add("-Xbootclasspath/p:$path")
            // 高版本 gradle 的方案
            val newFileList = mutableListOf<File>()
            newFileList.add(File(path))
            options.bootstrapClasspath = files(*newFileList.toTypedArray())
        }
    }
}

