# Chat Autocorrect (Fabric, Minecraft 1.21.1)

Fixes your chat messages right before they're sent:

- Capitalizes the start of each sentence: `hey guys` -> `Hey guys.`
- Fixes `i`, `im`, `dont`, `thats`, `teh`, `alot` and ~100 other common slips
- Spell check with a 50,000-word English dictionary plus Minecraft and chat slang: `diaomds` -> `diamonds`, `creaper` -> `creeper`, `obsidain` -> `obsidian`
- Adds a `.` at the end, or a `?` if it looks like a question (`where is the diamond` -> `Where is the diamond?`)
- Leaves alone: commands, links, emoticons like `:)` / `xD`, online players' names, and slang like `gg`, `lol`, `brb`
- Start a message with `\` to send it exactly as typed
- `/autocorrect on`, `/autocorrect off`, `/autocorrect toggle`, `/autocorrect spelling on|off` (saved between sessions)

Client-side only, so it works on any server.

## Build the .jar

You need **Java 21** (for example from https://adoptium.net).

1. Unzip this folder and open a terminal inside it.
2. Run:
   - Windows: `gradlew build`
   - Mac/Linux: `./gradlew build`
3. The first build downloads Minecraft and Fabric, so it takes a few minutes.
4. Your mod is `build/libs/chat-autocorrect-1.1.0.jar`.

**No terminal?** Upload this folder to a GitHub repo. The included GitHub Action builds the jar automatically; download it from the repo's **Actions** tab.

## Install

1. Install **Fabric Loader** for 1.21.1 (https://fabricmc.net/use/installer/).
2. Put the jar **and** **Fabric API** (https://modrinth.com/mod/fabric-api, 1.21.1 version) in your `.minecraft/mods` folder.
3. Launch the Fabric profile.

## Adding your own word fixes

- Exact fixes: edit the `pairs` list in `src/main/java/com/rina/chatautocorrect/AutoCorrect.java`.
- Words the spell checker should know (so it never changes them): add them to `src/main/resources/assets/chat-autocorrect/extra-words.txt`.

Then rebuild.
