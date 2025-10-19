// app/build.gradle.kts

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"
    kotlin("plugin.serialization") version "1.9.23" //Kotlin 序列化插件
}

android {
    namespace = "cc.bbq.xq" // 修正包名以匹配项目
    compileSdk = 34

    defaultConfig {
        applicationId = "cc.bbq.xq" // 修正包名以匹配项目
        minSdk = 21
        targetSdk = 34
        versionCode = 311
        versionName = "10.7-QUBOT" // 更新版本名以作区分
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"GPLv3\"")
        resourceConfigurations.add("zh-rCN")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        android.buildFeatures.buildConfig=true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    // ===== 基础依赖 =====
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
//移除Retrofit    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // ===== 移除Moshi 依赖 =====
//    implementation("com.squareup.moshi:moshi:1.15.1")
//    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
//    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
//    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // ===== Compose 全家桶 =====
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.compose.material:material")

    // ===== 图片加载方案 =====
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")

    // ===== 播放器依赖 =====
    implementation("com.sdtv.haikan:ijkplayer:0.0.2")
    implementation(project(":DanmakuFlameMaster"))
    
     // 用于颜色提取
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    // ===== 新增：ROOM 数据库依赖 =====
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Kotlin Coroutines 支持
    implementation("io.insert-koin:koin-androidx-compose:4.0.4")
    ksp("androidx.room:room-compiler:$room_version") // 使用 KSP 注解处理器
        // ===== Ktor 客户端依赖 =====
    val ktor_version = "2.3.13"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version") // 使用 OkHttp 引擎
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version") // 认证支持

    // ===== kotlinx.serialization =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

}