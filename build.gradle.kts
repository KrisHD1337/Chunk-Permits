import java.util.*

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("me.modmuss50.mod-publish-plugin")
    id("com.gradleup.shadow")
}

val minecraft = stonecutter.current.version
val loader = loom.platform.get().name.lowercase()

version = "${mod.version}+$minecraft"
group = mod.group
base {
    archivesName.set("${mod.id}-$loader")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else it.prop("loom.platform")
})

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.nucleoid.xyz/")
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    // 26.1 targets are deobfuscated and do not publish official mappings artifacts.
    // Loom still requires the mappings configuration to have a dependency, so use
    // Fabric intermediary there and keep Mojang mappings for all other versions.
    if (minecraft == "26.1") {
        mappings("net.fabricmc:intermediary:$minecraft:v2")
    } else {
        mappings(loom.officialMojangMappings())
    }

    modImplementation("dev.architectury:architectury-${loader}:${mod.dep("architectury_api")}")
    runtimeOnly("org.xerial:sqlite-jdbc:${mod.dep("sqlite_jdbc")}")
    shadowBundle("org.xerial:sqlite-jdbc:${mod.dep("sqlite_jdbc")}")

    if (loader == "fabric") {
        modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${mod.dep("fabric_version")}")
        modCompileOnly("com.terraformersmc:modmenu:${mod.dep("modmenu_version")}")
        modRuntimeOnly("com.terraformersmc:modmenu:${mod.dep("modmenu_version")}")
    }
    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${mod.dep("neoforge_loader")}")
    }
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/${mod.id}.accesswidener")

    decompilers {
        get("vineflower").apply {
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

publishMods {
    val modrinthToken = localProperties.getProperty("publish.modrinthToken", "")
    val curseforgeToken = localProperties.getProperty("publish.curseforgeToken", "")

    file = project.tasks.remapJar.get().archiveFile
    dryRun = modrinthToken.isBlank() || curseforgeToken.isBlank()

    displayName = "${mod.name} ${loader.replaceFirstChar { it.uppercase() }} ${property("mod.mc_title")}-${mod.version}"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = BETA

    modLoaders.add(loader)

    val targets = property("mod.mc_targets").toString().split(' ')
    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = modrinthToken
        targets.forEach(minecraftVersions::add)
        if (loader == "fabric") {
            requires("fabric-api")
            optional("modmenu")
        }
        requires("architectury-api")
    }

    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = curseforgeToken
        targets.forEach(minecraftVersions::add)
        if (loader == "fabric") {
            requires("fabric-api")
            optional("modmenu")
        }
        requires("architectury-api")
    }
}

val javaVersion = JavaVersion.toVersion(mod.prop("java_version"))

java {
    withSourcesJar()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion.majorVersion.toInt())
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
}

tasks.remapJar {
    injectAccessWidener = true
    input = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier = "dev"
}

val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
    from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    dependsOn("build")
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(buildAndCollect)
    }

    rootProject.tasks.register("runActive") {
        group = "project"
        dependsOn(tasks.named("runClient"))
    }
}

tasks.processResources {
    properties(
        listOf("fabric.mod.json"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to mod.prop("mc_dep_fabric"),
        "architectury_api" to mod.dep("architectury_api"),
        "java_version" to mod.prop("java_version")
    )
    properties(
        listOf("META-INF/neoforge.mods.toml", "pack.mcmeta"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to mod.prop("mc_dep_forgelike"),
        "architectury_api" to mod.dep("architectury_api"),
        "java_version" to mod.prop("java_version")
    )
    properties(
        listOf("chunkpermits.mixins.json"),
        "mixin_compat" to mod.prop("mixin_compat")
    )
}

tasks.build {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
}
