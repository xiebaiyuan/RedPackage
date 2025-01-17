import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.nsyw.autoredpack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nsyw.autoredpack"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val process = Runtime.getRuntime().exec("git symbolic-ref --short -q HEAD")
            val branch = process.text()
            val apkName = "redPackage"
            applicationVariants.all {
                outputs.all {
                    (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                        "$apkName-${branch}.apk"
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    dataBinding {
        enable = true
    }
}

fun Process.text(): String {
    // 输出 Shell 执行结果
    val inputStream = this.inputStream
    val insReader = InputStreamReader(inputStream)
    val bufReader = BufferedReader(insReader)

    var output = ""
    var line: String? = ""
    while (null != line) {
        // 逐行读取shell输出，并保存到变量output
        output += line
        line = bufReader.readLine()
    }
    return output
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation(project(":realpack"))
    implementation(project(":base"))
}