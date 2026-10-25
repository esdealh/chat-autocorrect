# Chat Autocorrect (Fabric, Minecraft 1.21 - 1.21.11)

Fixes your chat messages right before they're sent:

- Capitalizes the start of each sentence: `hey guys` -> `Hey guys.`
- Fixes `i`, `im`, `dont`, `thats`, `teh`, `alot` and ~100 other common slips
- Spell check with a 50,000-word English dictionary plus Minecraft and chat slang: `diaomds` -> `diamonds`, `creaper` -> `creeper`, `obsidain` -> `obsidian`
- Adds a `.` at the end, or a `?` if it looks like a question (`where is the diamond` -> `Where is the diamond?`)
- Leaves alone: commands, links, emoticons like `:)` / `xD`, online players' names, and slang like `gg`, `lol`, `brb`
- Start a message with `\` to send it exactly as typed
- `/autocorrect on`, `/autocorrect off`, `/autocorrect toggle`, `/autocorrect spelling on|off` (saved between sessions)

Client-side only, so it works on any server.

## Your own words

- **Mod Menu** (optional): Mods → Chat Autocorrect → **Configure**. Turn things on/off, add your own fixes (`gn = good night`, one per line) and words it should never change (nicknames, slang).
- **Commands**:
  - `/autocorrect add gn good night` - always change "gn" to "good night"
  - `/autocorrect ignore esdealh` - never change this word
  - `/autocorrect remove gn` - take a word off your lists
- Or edit `config/chat-autocorrect/fixes.txt` and `never-change.txt` in your .minecraft folder.

## Build the .jar

Each group of Minecraft versions gets its own jar. The settings for each are in `versions/`:

| Build target | Works on |
|---|---|
| `1.21.1` | 1.21 - 1.21.1 |
| `1.21.3` | 1.21.2 - 1.21.3 |
| `1.21.4` | 1.21.4 |
| `1.21.5` | 1.21.5 |
| `1.21.8` | 1.21.6 - 1.21.8 |
| `1.21.10` | 1.21.9 - 1.21.10 |
| `1.21.11` | 1.21.11 |

- **On GitHub:** every push builds all of them. Download them from the **Actions** tab.
- **On your computer** (Java 21): `gradlew build -Ptarget=1.21.11`. The jar is in `build/libs/`.

## Install

1. Install **Fabric Loader** for your Minecraft version (https://fabricmc.net/use/installer/).
2. Put the jar **and** **Fabric API** (https://modrinth.com/mod/fabric-api, version matching your Minecraft) in your `.minecraft/mods` folder.
3. Launch the Fabric profile.

## Adding your own word fixes

- Exact fixes: edit the `pairs` list in `src/main/java/com/rina/chatautocorrect/AutoCorrect.java`.
- Words the spell checker should know (so it never changes them): add them to `src/main/resources/assets/chat-autocorrect/extra-words.txt`.

Then rebuild.

## Credits

Word frequency list from [SymSpell](https://github.com/wolfgarbe/SymSpell) by Wolf Garbe (MIT license).
