# Example Mod Template

[![Build Status](https://github.com/2123jik/examplemod-template-1.21.1/actions/workflows/build.yml/badge.svg)](https://github.com/2123jik/examplemod-template-1.21.1/actions/workflows/build.yml)
[![NeoForge Version](https://img.shields.io/badge/NeoForge-21.1.215-blue.svg)](https://neoforge.net/)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net/)
[![Mod Version](https://img.shields.io/badge/Version-1.0.0-orange.svg)](https://github.com/2123jik/examplemod-template-1.21.1/releases)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

This repository serves as a comprehensive NeoForge mod template for Minecraft 1.21.1, demonstrating a wide array of advanced modding features. It includes examples for complex spell systems, data-driven affix and gem mechanics, custom entities, UI/UX enhancements, client-side shaders, and deep vanilla integration through mixins, making it an ideal starting point for ambitious mod developers.

---

## 📚 Table of Contents

*   [About](#-about)
*   [Showcased Modding Concepts](#-showcased-modding-concepts)
*   [Installation (For Developers)](#-installation-for-developers)
*   [Dependencies](#-dependencies)
*   [Configuration](#-configuration)
*   [Commands & Permissions](#-commands--permissions)
*   [Building from Source](#-building-from-source)
*   [Contributing](#-contributing)
*   [License](#-license)
*   [Credits](#-credits)

---

## 📖 About

This project is designed as a robust template for developing NeoForge mods for Minecraft 1.21.1. It goes beyond basic item and block registration, diving into more intricate game mechanics and client-side enhancements. If you're looking to build a mod with custom spellcasting, dynamic item properties, unique entities, or advanced UI, this template provides a solid foundation and numerous practical examples.

**Key Highlights:**
*   **NeoForge 1.21.1 Compatibility:** Built specifically for the latest stable NeoForge version.
*   **Gradle-based Build System:** Easy to set up and manage with standard Gradle tasks.
*   **Comprehensive Examples:** Features a wide range of modding techniques, from data-driven content to complex client-server interactions.
*   **Clean Structure:** Organized codebase to facilitate understanding and extension.

---

## ✨ Showcased Modding Concepts

This template demonstrates the implementation of various advanced modding features:

*   **Advanced Spell System:**
    *   Custom spell registration (`SpellRegistries`).
    *   Example spells like `FlamingStrikeSpell` and `GateOfBabylonSpell`.
    *   Integration with custom `SpellBonusData` components.
    *   Event handling for spells (`SpellHealEventMixin`).
    *   Utilities for spell casting and discovery.
*   **Comprehensive Affix System:**
    *   Data-driven affixes defined in JSON (`data/examplemod/affixes/`).
    *   Affixes for various item types (armor, magic weapons, melee, ranged, shield).
    *   Elemental school-based affixes (Abyssal, Aqua, Blood, Eldritch, Fire, Ice, Holy, etc.).
    *   Affixes influencing attributes, mob effects, and spell triggers.
*   **Custom Gem System:**
    *   Data-driven gem definitions (`data/examplemod/gems/`).
    *   Core and external gems with unique properties.
    *   Extra gem bonuses (`data/examplemod/extra_gem_bonuses/`).
    *   Rarity overrides for specific items.
    *   Integration with Apotheosis-like gem mechanics (inferred from `assets/apotheosis/models/item/gems/`).
*   **Time Manipulation Mechanics:**
    *   Client-side rendering for time echoes (`TimeEchoRenderer`).
    *   `TimeTravelManager` for managing time-related effects.
    *   Server-side handling for echo magic (`ServerEchoMagicHandler`).
*   **In-Game Chess System:**
    *   A unique client-side implementation of a chess game (`client/chess/`).
    *   Includes UI configurations, AI helpers, and local game management.
*   **Custom Entities & Effects:**
    *   Registration and rendering of custom entities (`GoldenGateEntity`, `SwordProjectileEntity`).
    *   Custom mob effects (`FearEffect`, `MakenPowerEffect`) with server-side logic.
    *   Custom damage sources (`ModDamageSources`).
*   **Enhanced UI/UX:**
    *   Custom inventory rendering (`InventoryModelRenderer`).
    *   Advanced tooltip rendering (`InventoryTooltipRenderer`).
    *   Client-side configuration screens (`OffsetConfigScreen`, `RenderOffsetConfig`).
    *   Modernized vanilla screens (`ModernBrewingStandScreen`, `ModernMerchantScreen`).
    *   Integration with Curios API for additional equipment slots (`CuriosBackRenderLayer`).
*   **Custom Structures:**
    *   Loading and rendering of custom NBT structures (`kisegi_sanctuary_main_0.nbt`, `plains_small_house_1.nbt`).
*   **Client-Side Shaders:**
    *   Examples of custom post-processing shaders (`crt`, `reddish`, `ripple`).
*   **Deep Vanilla Integration:**
    *   Extensive use of Mixins to modify core game mechanics (e.g., combat, loot tables, inventory, food, mob effects, player behavior).
*   **Data Components:**
    *   Utilizes Minecraft 1.20.5+ Data Components for flexible item/block data storage.
*   **Custom Commands & Networking:**
    *   Examples of custom server commands (`ModCommands`, `IronsApothicCommands`).
    *   Custom networking messages for client-server communication.
*   **Localization:**
    *   Support for multiple languages (English and Simplified Chinese).

---

## ⚙️ Installation (For Developers)

This project is a template intended for development. To get started:

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/2123jik/examplemod-template-1.21.1.git
    cd examplemod-template-1.21.1
    ```

2.  **Set up your IDE:**
    *   **IntelliJ IDEA:**
        *   Open IntelliJ IDEA.
        *   Select `File > Open...` and navigate to the cloned directory.
        *   Select `build.gradle` and click `Open as Project`.
        *   Allow Gradle to sync. This may take some time.
    *   **Eclipse:**
        *   Eclipse setup for NeoForge projects typically involves running `gradlew eclipse` (or `gradlew.bat eclipse` on Windows) from the command line in your project directory, then importing the project into Eclipse.

3.  **Run Development Environment:**
    Once your IDE is set up, you can use the provided Gradle tasks to run Minecraft:
    *   `gradlew genSources` (or `gradlew.bat genSources`): Generates source code for Minecraft and NeoForge.
    *   `gradlew runClient`: Launches a Minecraft client with your mod loaded.
    *   `gradlew runServer`: Launches a dedicated Minecraft server with your mod loaded.
    *   `gradlew runData`: Runs data generators to create resources (e.g., recipes, loot tables).

---

## 🔗 Dependencies

This template demonstrates integration with several common modding libraries and APIs:

*   **NeoForge:** The core mod loader for Minecraft 1.21.1.
*   **Parchment:** For official Mojang mappings.
*   **Registrate:** A powerful library for simplified registry object management.
*   **Curios API:** (Inferred) For custom equipment slots and inventory layers.
*   **Apotheosis:** (Inferred) For advanced enchantment, affix, and gem mechanics.
*   **Iron's Spellbooks:** (Inferred from mixins) For spell-related events and mechanics.
*   **LLibrary:** (Inferred from mixins) A common library for various modding utilities.

*Note: The `build.gradle` includes a local dependency path for development purposes. These are not public dependencies for end-users.*

---

## 🛠️ Configuration

This template showcases data-driven configuration through extensive JSON files for affixes, gems, and rarity overrides located in `src/main/resources/data/examplemod/`.

Client-side configuration for UI elements and rendering offsets can be found in `src/main/java/com/example/examplemod/client/gui/RenderOffsetConfig.java` and accessed via `OffsetConfigScreen`.

Developers can extend these systems to create highly customizable mod content.

---

## 📜 Commands & Permissions

This template includes examples of custom commands registered via `ModCommands.java` and `IronsApothicCommands.java`. Specific commands and their usage would need to be defined within the source code.

*   **Commands:** (Refer to source code for exact commands)
    *   `/<modid> <subcommand>`
    *   `/<ironsapothic> <subcommand>`
*   **Permissions:** (Refer to source code for exact permissions, if implemented)
    *   Permissions are typically managed by server-side permission plugins.

---

## 🏗️ Building from Source

To build the mod JAR file:

1.  Ensure you have Java Development Kit (JDK) 21 installed.
2.  Open a terminal or command prompt in the project's root directory.
3.  Run the Gradle build command:
    ```bash
    gradlew build
    ```
    (On Windows, use `gradlew.bat build`)

The compiled mod JAR will be located in `build/libs/`.

---

## 🤝 Contributing

As a template, contributions are highly welcome! If you have ideas for demonstrating new modding concepts, improving existing examples, or fixing issues, feel free to open an issue or submit a pull request.

Please ensure your contributions adhere to the existing code style and maintain the template's educational purpose.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

*   **2123jik:** Original author and maintainer.
*   **NeoForge Team:** For providing the modding framework.
*   **Parchment Team:** For providing mappings.
*   **Registrate, Curios API, Apotheosis, Iron's Spellbooks, LLibrary:** For their excellent libraries and inspiration for complex modding techniques.
