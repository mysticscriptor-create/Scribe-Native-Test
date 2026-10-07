package com.primaloptima.scribe.util

import com.primaloptima.scribe.util.model.ShortcutAction

object DefaultShortcuts {

    const val CAT_GENERAL = "General"
    const val CAT_FORMATTING = "Formatting"
    const val CAT_BRACKETS = "Brackets"
    const val CAT_SCENE_BREAKS = "Scene Breaks"
    const val CAT_PUNCTUATION = "Punctuation"

    val all: List<ShortcutAction> = listOf(
        ShortcutAction(id = "tab",        label = "Tab",     kind = "insert", payload = "    ", category = CAT_FORMATTING),
        ShortcutAction(id = "h1",         label = "H1",      kind = "insert", payload = "# ", category = CAT_FORMATTING),
        ShortcutAction(id = "h2",         label = "H2",      kind = "insert", payload = "## ", category = CAT_FORMATTING),
        ShortcutAction(id = "bold",       label = "B",       kind = "wrap",   payload = "**",  closing = "**", category = CAT_FORMATTING),
        ShortcutAction(id = "italic",     label = "I",       kind = "wrap",   payload = "*",   closing = "*", category = CAT_FORMATTING),
        ShortcutAction(id = "code",       label = "‹›",      kind = "wrap",   payload = "`",   closing = "`", category = CAT_FORMATTING),
        ShortcutAction(id = "quote",      label = "\u201C\u201D",  kind = "pair", payload = "\u201C", closing = "\u201D", category = CAT_PUNCTUATION),
        ShortcutAction(id = "smartquote", label = "\u2018\u2019",  kind = "pair", payload = "\u2018", closing = "\u2019", category = CAT_PUNCTUATION),
        ShortcutAction(id = "paren",      label = "( )",     kind = "pair",   payload = "(",   closing = ")", category = CAT_BRACKETS),
        ShortcutAction(id = "bracket",    label = "[ ]",     kind = "pair",   payload = "[",   closing = "]", category = CAT_BRACKETS),
        ShortcutAction(id = "brace",      label = "{ }",     kind = "pair",   payload = "{",   closing = "}", category = CAT_BRACKETS),
        ShortcutAction(id = "blockquote", label = "Quote",   kind = "insert", payload = "\n> ", category = CAT_FORMATTING),
        ShortcutAction(id = "list",       label = "•",       kind = "insert", payload = "\n- ", category = CAT_FORMATTING),
        ShortcutAction(id = "hr",         label = "—",       kind = "insert", payload = "\n\n---\n\n", category = CAT_SCENE_BREAKS),
        ShortcutAction(id = "emdash",     label = "—",       kind = "insert", payload = " \u2014 ", category = CAT_PUNCTUATION),
        ShortcutAction(id = "ellipsis",   label = "…",       kind = "insert", payload = "\u2026", category = CAT_PUNCTUATION)
    )

    val defaultSnippets: List<ShortcutAction> = listOf(
        ShortcutAction(
            id = "snip_pov_break",
            label = "Scene Break (***)",
            kind = "insert",
            payload = "\n\n***\n\n",
            itemType = "snippet",
            description = "Triple-asterisk horizontal scene divider",
            keywords = listOf("scene", "break", "divider", "asterisk")
        ),
        ShortcutAction(
            id = "snip_dialogue_quote",
            label = "Dialogue Line",
            kind = "pair",
            payload = "“",
            closing = "”",
            itemType = "snippet",
            description = "Wrap or insert formatted dialogue speech marks",
            keywords = listOf("dialogue", "speech", "quotes", "talk")
        ),
        ShortcutAction(
            id = "snip_internal_monologue",
            label = "Internal Thought",
            kind = "wrap",
            payload = "*",
            closing = "*",
            itemType = "snippet",
            description = "Italicized inner monologue or unspoken thought",
            keywords = listOf("thought", "italic", "monologue", "mind")
        ),
        ShortcutAction(
            id = "snip_action_pause",
            label = "Beat Pause",
            kind = "insert",
            payload = "A heavy silence fell over the room. ",
            itemType = "snippet",
            description = "Pacing beat for dramatic tension",
            keywords = listOf("pause", "beat", "silence", "pacing")
        ),
        ShortcutAction(
            id = "snip_scene_metadata",
            label = "Scene Header",
            kind = "insert",
            payload = "\n**[ SCENE: Setting | POV: Character | Goal: Intent ]**\n\n",
            itemType = "snippet",
            description = "Author metadata block for scene goal and POV",
            keywords = listOf("metadata", "scene", "header", "pov")
        ),
        ShortcutAction(
            id = "snip_cliffhanger",
            label = "Cliffhanger Beat",
            kind = "insert",
            payload = "\n> *And that was the moment everything went irrevocably wrong.*\n",
            itemType = "snippet",
            description = "Highlighted foreshadowing or chapter end hook",
            keywords = listOf("cliffhanger", "hook", "end", "tension")
        ),
        ShortcutAction(
            id = "snip_footnote",
            label = "Footnote Anchor",
            kind = "insert",
            payload = "[^1]\n\n[^1]: ",
            itemType = "snippet",
            description = "Markdown footnote reference and definition",
            keywords = listOf("footnote", "citation", "reference")
        ),
        ShortcutAction(
            id = "snip_sensory_anchor",
            label = "Sensory Cue",
            kind = "insert",
            payload = "**[SIGHT / SOUND / SCENT]:** ",
            itemType = "snippet",
            description = "Drafting placeholder for deepening sensory prose",
            keywords = listOf("sensory", "draft", "placeholder", "notes")
        )
    )

    val defaultTemplates: List<ShortcutAction> = listOf(
        ShortcutAction(
            id = "tmpl_chapter_outline",
            label = "Chapter Outline",
            kind = "insert",
            payload = "# Chapter [Number]: [Title]\n\n## Goal & Stakes\n- **Protagonist Goal:** \n- **Antagonist Force:** \n- **The Stakes:** \n\n## Scene Breakdown\n1. **Hook:** \n2. **Inciting Shift:** \n3. **Core Conflict:** \n4. **Climax / Twist:** \n5. **Ending Disaster / Choice:** \n\n---\n\n",
            itemType = "template",
            description = "Complete 5-stage structural outline for a chapter",
            keywords = listOf("chapter", "outline", "structure", "plot")
        ),
        ShortcutAction(
            id = "tmpl_character_dossier",
            label = "Character Dossier",
            kind = "insert",
            payload = "# Character Dossier: [Full Name]\n\n- **Role in Narrative:** Protagonist / Antagonist / Foil\n- **External Desire (Want):** \n- **Internal Need:** \n- **Fatal Flaw / Lie Believed:** \n- **Distinct Voice & Idiosyncrasies:** \n- **Key Relationships & Tension:** \n\n---\n\n",
            itemType = "template",
            description = "Deep character profile and motivational psychology sheet",
            keywords = listOf("character", "dossier", "profile", "bio")
        ),
        ShortcutAction(
            id = "tmpl_scene_beat_sheet",
            label = "Scene Beat Sheet",
            kind = "insert",
            payload = "# Scene: [Working Title]\n\n- **POV:** \n- **Setting & Time:** \n- **Starting Emotional Valence:** \n- **Ending Emotional Valence:** \n\n### Beats\n- **Beat 1 (Entry):** \n- **Beat 2 (Obstacle):** \n- **Beat 3 (Escalation):** \n- **Beat 4 (The Reveal):** \n\n---\n\n",
            itemType = "template",
            description = "Turn-by-turn emotional and dramatic beat sheet",
            keywords = listOf("beat", "scene", "sheet", "drama")
        ),
        ShortcutAction(
            id = "tmpl_3act_structure",
            label = "Three-Act Architecture",
            kind = "insert",
            payload = "# Story Architecture: [Title]\n\n## Act I — Departure\n- **Ordinary World:** \n- **Inciting Incident:** \n- **Plot Point 1 (No Return):** \n\n## Act II — Confrontation\n- **Rising Stakes & Trials:** \n- **Midpoint Climax:** \n- **All Hope Is Lost:** \n\n## Act III — Resolution\n- **Climax / Final Choice:** \n- **Resolution & New World:** \n\n---\n\n",
            itemType = "template",
            description = "Macro three-act story architecture framework",
            keywords = listOf("act", "story", "novel", "architecture")
        ),
        ShortcutAction(
            id = "tmpl_world_lore",
            label = "World Lore Entry",
            kind = "insert",
            payload = "# Lore Entry: [Topic / Realm / Artifact]\n\n- **Classification:** Faction / Geographic / Historical / Magick\n- **Origins & Legends:** \n- **Rules & Limitations:** \n- **Current Relevance to Plot:** \n\n---\n\n",
            itemType = "template",
            description = "Worldbuilding entry for factions, lore, or magic systems",
            keywords = listOf("world", "lore", "codex", "setting")
        )
    )

    fun autoDetectIcon(kind: String, payload: String, closing: String?): String {
        return when {
            kind == "wrap" || kind == "pair" -> closing ?: payload
            payload.startsWith("# ") -> "H1"
            payload.startsWith("## ") -> "H2"
            payload.startsWith("### ") -> "H3"
            payload.contains("***") -> "***"
            payload.contains("---") -> "—"
            else -> payload.take(2).trim()
        }
    }
}
