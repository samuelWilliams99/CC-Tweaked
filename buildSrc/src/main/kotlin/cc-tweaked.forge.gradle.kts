// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/** Default configuration for Forge projects. */

import cc.tweaked.gradle.CCTweakedExtension
import cc.tweaked.gradle.CCTweakedPlugin
import cc.tweaked.gradle.IdeaRunConfigurations
import cc.tweaked.gradle.MinecraftConfigurations

plugins {
    id("cc-tweaked.java-convention")
    id("net.neoforged.gradle.userdev")
    // id("org.parchmentmc.librarian.forgegradle")
}

plugins.apply(CCTweakedPlugin::class.java)

val mcVersion: String by extra

minecraft {
    modIdentifier("computercraft")
}
// minecraft {
//     val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
//     mappings("parchment", "${libs.findVersion("parchmentMc").get()}-${libs.findVersion("parchment").get()}-$mcVersion")
// }

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    implementation("net.neoforged:neoforge:${libs.findVersion("neoForge").get()}")
}

MinecraftConfigurations.setup(project)

extensions.configure(CCTweakedExtension::class.java) {
    linters(minecraft = true, loader = "forge")
}
