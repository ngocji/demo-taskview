import groovy.namespace.QName
import groovy.util.Node
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    kotlin("kapt")
}

android {
    namespace = "com.example.demotaskview"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.demotaskview"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
        aidl = true
    }

    useLibrary("android.car")
}

dependencies {
    compileOnly(rootProject.files("app/sdk/framework-14.jar"))
    implementation(files("libs/WindowManager-Shell-14.jar"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.jakewharton.timber:timber:5.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(projects.iconloaderlib)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.constraint.layout)
    implementation(libs.androidx.navigation.compose)
}

project.tasks.preBuild.get().doLast {
    // 在 preBuild 任务执行完之后处理
    // 定义修改 .iml 文件中 Android SDK 优先级方法
    fun changeSdkOrder(path: String) {
        runCatching {
            val imlFile = File(path)
            with(XmlParser().parse(imlFile)) {
                // 从 .iml 文件中读取 NewModuleRootManager 节点
                val rootManagerComponent = getAt(QName.valueOf("component"))
                    .map { it as Node }
                    .first { it.attribute("name") == "NewModuleRootManager" }
                // 从 NewModuleRootManager 节点中获取 Android SDK 配置节点
                val jdkEntry = rootManagerComponent.getAt(QName.valueOf("orderEntry"))
                    .map { it as Node }
                    .first { it.attribute("type") == "jdk" }
                // 保存节点参数
                val jdkName = jdkEntry.attribute("jdkName")
                val jdkType = jdkEntry.attribute("jdkType")
                println("> Task :${project.name}:preBuild:doLast:changedSdkOrder jdkEntry = $jdkEntry")
                // 从 NewModuleRootManager 节点中移除 Android SDK 配置节点
                rootManagerComponent.remove(jdkEntry)
                // 重新将 Android SDK 配置节点添加到 NewModuleRootManager 的最后
                rootManagerComponent.appendNode(
                    "orderEntry", mapOf(
                        "type" to "jdk",
                        "jdkName" to jdkName,
                        "jdkType" to jdkType
                    )
                )
                // 将新生成的 .iml 写入文件
                XmlUtil.serialize(this, FileOutputStream(imlFile))
            }
        }
    }

    // 修改 .iml 文件
    println("> Task :${project.name}:preBuild:doLast:changedSdkOrder")
    changeSdkOrder(rootDir.absolutePath + "/.idea/modules/app/DemoTaskView.app.iml")
}


