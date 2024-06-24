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
        applicationId = "com.hyundai.homescreen"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "platform2"
            keyPassword = "test1234"
            storeFile = file("platform2.jks")
            storePassword = "android"
        }
        create("release") {
            keyAlias = "platform2"
            keyPassword = "test1234"
            storeFile = file("platform2.jks")
            storePassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(files("libs/launcher3.jar"))
    compileOnly(files("libs/android.car.jar"))
    implementation(files("libs/framework.jar"))
    implementation(files("libs/WindowManager-Shell.jar"))

//    implementation(files("libs/car-telephony-common.aar"))
//    implementation(files("libs/car-media-common.aar"))
//    implementation(files("libs/car-apps-common.aar"))

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.androidx.activity)

    implementation(libs.android.car.ui)

    implementation(libs.timber)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

//project.tasks.preBuild.get().doLast {
//    // 在 preBuild 任务执行完之后处理
//    // 定义修改 .iml 文件中 Android SDK 优先级方法
//    fun changeSdkOrder(path: String) {
//        runCatching {
//            val imlFile = File(path)
//            with(XmlParser().parse(imlFile)) {
//                // 从 .iml 文件中读取 NewModuleRootManager 节点
//                val rootManagerComponent = getAt(QName.valueOf("component"))
//                    .map { it as Node }
//                    .first { it.attribute("name") == "NewModuleRootManager" }
//                // 从 NewModuleRootManager 节点中获取 Android SDK 配置节点
//                val jdkEntry = rootManagerComponent.getAt(QName.valueOf("orderEntry"))
//                    .map { it as Node }
//                    .first { it.attribute("type") == "jdk" }
//                // 保存节点参数
//                val jdkName = jdkEntry.attribute("jdkName")
//                val jdkType = jdkEntry.attribute("jdkType")
//                println("> Task :${project.name}:preBuild:doLast:changedSdkOrder jdkEntry = $jdkEntry")
//                // 从 NewModuleRootManager 节点中移除 Android SDK 配置节点
//                rootManagerComponent.remove(jdkEntry)
//                // 重新将 Android SDK 配置节点添加到 NewModuleRootManager 的最后
//                rootManagerComponent.appendNode(
//                    "orderEntry", mapOf(
//                        "type" to "jdk",
//                        "jdkName" to jdkName,
//                        "jdkType" to jdkType
//                    )
//                )
//                // 将新生成的 .iml 写入文件
//                XmlUtil.serialize(this, FileOutputStream(imlFile))
//            }
//        }
//    }
//
//    // 修改 .iml 文件
//    println("> Task :${project.name}:preBuild:doLast:changedSdkOrder")
//    changeSdkOrder(rootDir.absolutePath + "/.idea/modules/app/DemoTaskView.app.iml")
//}


