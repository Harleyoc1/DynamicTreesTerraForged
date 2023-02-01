import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import java.time.Instant
import java.time.format.DateTimeFormatter

fun property(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("net.minecraftforge.gradle")
    id("org.parchmentmc.librarian.forgegradle")
    id("idea")
    id("com.matthewprenger.cursegradle") version "1.4.0"
}

apply {
    from("https://raw.githubusercontent.com/SizableShrimp/Forge-Class-Remapper/main/classremapper.gradle")
    from("https://gist.githubusercontent.com/Harleyoc1/4d23d4e991e868d98d548ac55832381e/raw/applesiliconfg.gradle")
}

repositories {
    mavenLocal()
    maven("https://ldtteam.jfrog.io/ldtteam/modding/")
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://harleyoconnor.com/maven")
    maven("https://squiddev.cc/maven/")
}

val modName = property("modName")
val modId = property("modId")
val modVersion = property("modVersion")
val mcVersion = property("mcVersion")

version = "$mcVersion-$modVersion"
group = property("group")

minecraft {
    mappings("parchment", "${property("mappingsVersion")}-$mcVersion")

    runs {
        create("client") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            if (project.hasProperty("mcUuid")) {
                args("--uuid", property("mcUuid"))
            }
            if (project.hasProperty("mcUsername")) {
                args("--username", property("mcUsername"))
            }
            if (project.hasProperty("mcAccessToken")) {
                args("--accessToken", property("mcAccessToken"))
            }

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${buildDir}/createSrgToMcp/output.srg")

            args(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources"),
                "--existing-mod", "dynamictrees"
            )

            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

dependencies {
    minecraft("net.minecraftforge:forge:${mcVersion}-${property("forgeVersion")}")

    implementation(fg.deobf("curse.maven:jade-324717:3970956"))

    compileOnly(fg.deobf("mezz.jei:jei-$mcVersion:${property("jeiVersion")}:api"))
    runtimeOnly(fg.deobf("mezz.jei:jei-$mcVersion:${property("jeiVersion")}"))

    implementation(fg.deobf("com.ferreusveritas.dynamictrees:DynamicTrees-${mcVersion}:${property("dynamicTreesVersion")}"))
    runtimeOnly(fg.deobf("curse.maven:TerraForged-363820:3811021"))

    runtimeOnly(fg.deobf("com.ferreusveritas.dynamictreesplus:DynamicTreesPlus-$mcVersion:${property("dynamicTreesPlusVersion")}"))
    runtimeOnly(fg.deobf("vazkii.patchouli:Patchouli:${property("patchouliVersion")}"))
    runtimeOnly(fg.deobf("org.squiddev:cc-tweaked-$mcVersion:${property("ccVersion")}"))
    runtimeOnly(fg.deobf("com.harleyoconnor.suggestionproviderfix:SuggestionProviderFix-1.18.1:${property("suggestionProviderFixVersion")}"))
}

tasks.jar {
    manifest.attributes(
        "Specification-Title" to project.name,
        "Specification-Vendor" to "ferreusveritas",
        "Specification-Version" to "1",
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "ferreusveritas",
        "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    )

    archiveBaseName.set(modName)
    finalizedBy("reobfJar")
}

java {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

curseforge {
    if (project.hasProperty("curseApiKey") && project.hasProperty("curseFileType")) {
        apiKey = property("curseApiKey")

        project {
            id = "488347"

            addGameVersion(mcVersion)

            changelog = "Changelog will be added shortly..."
            changelogType = "markdown"
            releaseType = property("curseFileType")

            addArtifact(tasks.findByName("sourcesJar"))

            mainArtifact(tasks.findByName("jar")) {
                relations {
                    requiredDependency("dynamictrees")
                    requiredDependency("terraforged")
                    optionalDependency("dynamictreesplus")
                }
            }
        }
    } else {
        project.logger.log(LogLevel.WARN, "API Key and file type for CurseForge not detected; uploading will be disabled.")
    }
}

// Extensions to make CurseGradle extension slightly neater.

fun com.matthewprenger.cursegradle.CurseExtension.project(action: CurseProject.() -> Unit) {
    this.project(closureOf(action))
}

fun CurseProject.mainArtifact(artifact: Task?, action: CurseArtifact.() -> Unit) {
    this.mainArtifact(artifact, closureOf(action))
}

fun CurseArtifact.relations(action: CurseRelation.() -> Unit) {
    this.relations(closureOf(action))
}
