# Chapters 10 to 12 consequence ledger

Status: written on `chapters-6-9-expansion` (from 8be2d98); uncommitted. This is a design record, not player text. Every row says what
the decision does in the story **today**, which scenes **actually read** the flag it sets (found in the compiled story graph, not a plan),
and what a later chapter is **planned** to do with it. "Planned" is a proposal. None of it is implemented, none of it needs an engine or
save-format change, and Chapters 13 onward are not touched by this package.

## How the package holds state

| Family or set | Members | Cleared by |
|---|---|---|
| `C10_COVER_FAMILY` (alternatives) | `c10_cover_voss`, `c10_cover_stair`, `c10_cover_forge` | the first preparation choice (chapter entry), and the cover choice itself |
| `C10_RITES_FAMILY` (alternatives) | `c10_rites_name`, `c10_rites_deeds`, `c10_rites_plain` | the first preparation choice, and the page itself |
| `HUB_MARKERS_FOUR` | the shared `hub_a`, `hub_b`, `hub_c`, `hub_second`, plus new `hub_d` (the fourth place) and `hub_third` (the third visit) | the choice that leaves the night |
| `C10_TELLING_FLAGS` | the two preparation flags, the three answers to Ilsevet, the defeat marker, and the three sets above | `the_last_two`, both choices |

Not cleared, deliberately: `halvard_died` and `took_the_warden_chain`. Nothing in Chapters 10 to 12 reads them, no new scene sets or clears
them, and a test holds that. The Chapter 11 and 12 flags that shipped (`trusted_the_vision` and the rest) were never cleared on a replay
and still are not; nothing reads them either, so no path changes, and the Journal's alternative groups already give one neutral line
when a replay leaves two of them. Chapters 11 and 12 add no flag.

## Chapter 10: What Ilsevet Came to See

| Flag | Set at | Local effect | Read by (implemented) | Journal | Planned later acknowledgement (not implemented) |
|---|---|---|---|---|---|
| `hub_a`, `hub_b`, `hub_c`, `hub_d`, `hub_second`, `hub_third` | the night's hub | which of the four places were seen and how many | the hub's own continue choices | hidden (bookkeeping) | none |
| `c10_cover_voss` | `c10_line_cover`, "Stand with Voss at the gate." | Courage +1. Voss and Kaelen carry Halvard; at the foot of the stair she says "You could have stood anywhere", and "I noticed" | `c10_carry_bridge` -> `c10_carry_voss` (also the scene a save with no answer gets) | decisions, `siege_stand` group | **Ch13** rites: she keeps the watch and is the one who speaks beside him; **Ch18** (what Selvane's verdict has already cost her); **Ch30** roll, Voss's ladder |
| `c10_cover_stair` | same, "Go to the stair, to the people on it." | Honour +1. The people carry Halvard. Edda (where she is in the story) says the door is Kaelen's now to keep or shut, and that they will remember where he stood; elsewhere the oldest woman on the stair says it | `c10_carry_bridge` -> `c10_carry_stair_edda` (any of the three Chapter 7 promise outcomes) or `c10_carry_stair` | decisions, `siege_stand` | **Ch13** rites: the girl with the lamp (Tam, where she was met) lights it; **Ch24**: Tam recognises the nails or the grave; **Ch30** roll, Tam and the refugees, and the Hold's state |
| `c10_cover_forge` | same, "Stand at the forge door with the smiths." | Courage +1. The smiths carry Halvard; the old smith tells Kaelen the forge will be lit at first light and that someone must keep working what Halvard left | `c10_carry_bridge` -> `c10_carry_forge` | decisions, `siege_stand` | **Ch13** rites: the smiths carve or keep the name; later chapters: the Hold's forge at work |
| `c10_rites_name`, `c10_rites_deeds`, `c10_rites_plain` | `c10_page` (the smiths' gate-book) | none yet; the page decides the words said at the hasty earth | `c10_carry_*` -> `c10_earth_name`, `c10_earth_deeds`, `c10_earth_plain` (none written: `c10_earth_bare`) | hidden (tone only, to keep the Journal curated) | **Ch13** proper rites quote the page |
| trophy "The Last Evening" | `c10_forge_gap`, the continue after Kaelen offers to hold the north gap and Halvard refuses | secret; paid once; three sites (see below) | the trophy list | n/a | none |

What the existing flags now do in Chapter 10 (all implemented; none of these changes a route, only which scene a reader meets):

| Flag | Read by | What the reader meets |
|---|---|---|
| `c7_traded` or `c7_page` | hub -> `c10_forge_open` | Halvard on Kestrel's words, which Kaelen did not keep back |
| `c7_comfort` or `c7_quiet` | hub -> `c10_forge_kind` | Halvard on how decent Kaelen was after the letter |
| `c7_shut`, or none of the five | hub -> `c10_forge_shut` | Halvard on the weight Kaelen has not set down |
| `c7_halvard_trusts_voss` / otherwise | `c10_forge_talk` -> `c10_forge_voss_trusts` / `c10_forge_voss_doubts` | what Halvard makes of Voss, in his own voice |
| `c9_stair_rules` | `c10_forge_talk` -> `c10_forge_side` | Halvard thanks Kaelen for standing behind the rules |
| `c7_rung_wary` / `working` / `trusted` | hub -> `c10_wall_*`; `c10_wall_stay` (moves the rung +1); `toTheWeight` | Voss on the wall, three ways |
| `c7_voss_hurt_deeper` | `c10_wall_*` -> `c10_wall_hurt` | "still healing" is true, and she says she will say if she must leave the wall |
| `c7_hold_terms` / `_refused` / `_hidden` | hub -> `c10_people_inside` / `_outside` / `_unspent`; `c10_line_hold_*`; `carryChoices` | Edda's people on the stair, on the ridge, or in the ravine with the promise unspent. With no promise there are no people to look in on and the hub shows three places |
| `c6_met_tam` | `c10_people_*` -> `c10_tam` | Tam, fifteen, keeping a lamp lit by her feet, and what she saw in the wall |
| `c9_stair_people` | `c10_people_inside` -> `c10_carter` | the broad woman in the carter's coat (inside only: the north stair was the only place she was met) |
| `c7_ground_yard` / `_stair` / `_road` | `c10_before_light` -> `c10_line_ground_*` | no whetstones; the column knows the stair; Ilsevet already knows who stands beside the jailer |
| `refused_envoy`, `heard_envoy_offer`, `confronted_envoy_with_prisoners_words` | `kaelens_answer` (three new gated answers) | Kaelen answers Ilsevet knowing what her envoy asked; each reaches a shipped scene and sets the flag that scene's shipped answer sets |

## Chapter 11: The Sixth Name

| Flag | Read by (implemented) | What the reader meets |
|---|---|---|
| `c7_rung_*` | `chapter10_end` -> `c11_weight_wary` / `working` / `trusted`; the honest answer moves the rung one step (the top rung has none to move) | the first night at the north gap: the chain against Kaelen's throat, a sentry's "Warden", and Voss's answer by where they stand |
| `c8_tithe_seen` | `the_road_to_fenmoor` -> `c11_tithe_route` -> `c11_cut_them_off` (Courage +1) | the thumbprint of ash on a post, and the causeway where a heavy cart must slow |

No flag is added. The marsh detour has no flag because nothing would read it.

## Chapter 12: Four Settings

| Flag | Read by (implemented) | What the reader meets |
|---|---|---|
| `c7_rung_*` | `back_to_the_ash` -> `c12_voss_wary` / `working` / `trusted` | Voss at the last dry stone, three ways. The rung does not move in this chapter |
| `c9_way_wall`, `c9_way_cart`, and `c9_way_stair` (top rung only) | `c12_voss_*` -> `c12_way_*` | the way Kaelen went into the Sanctum in Chapter 9, met again, ruined |
| `trusted_voss_fully` | both shipped endings, unchanged | still alone decides which delegation scene follows. Routing on the ladder was considered and refused: Chapter 13's delegation text names the Chapter 7 decision |

## Obligations carried over from the Chapters 7 to 9 ledger

| Flag or family | Assigned | Outcome |
|---|---|---|
| `c7_traded`, `c7_shut`, `c7_page`, `c7_comfort`, `c7_quiet` | Ch10 | **reader written**: the three forge conversations |
| `c7_voss_hurt_deeper` | Ch10 | **reader written**: `c10_wall_hurt` |
| `c9_stair_rules` | Ch10 | **reader written**: `c10_forge_side` |
| `c9_stair_people` | Ch10 | **reader written**: `c10_carter` (inside only: she was met on the north stair) |
| `c7_hold_*` | Ch10 | **reader written**: the people, the Line and the carrying |
| `c7_ground_*` | Ch10 | **reader written**: `c10_line_ground_*` |
| `c8_tithe_seen` | Ch11 | **reader written**: `c11_tithe_route` |
| `c7_rung_*` | Ch12, then 13 and 18 | **readers written** in Chapters 10, 11 and 12; Chapters 13 and 18 remain planned |
| `c7_halvard_trusts_voss`, `c7_halvard_doubts_voss` | Ch10 (Chapter 7 table) | **reader written**: `c10_forge_voss_trusts`, `c10_forge_voss_doubts` |
| `c9_way_stair`, `c9_way_wall`, `c9_way_cart` | none assigned | **assigned to Ch12**: `c12_way_*` |
| `c7_asked_about_riders` | none assigned | **kept as a hidden record, no reader**: a tone beat; recorded here so it is not forgotten |
| `c8_exit_tithe` | none assigned (and Ch13 in the later list) | **stays planned for Chapter 13**: no Ch10 to 12 reader |

Fourteen flags had no reader at 8be2d98. Ten now have one: nine are read by name (`c7_traded`, `c7_page`, `c7_comfort`, `c7_quiet`,
`c7_voss_hurt_deeper`, `c9_stair_rules`, and the three `c9_way_*`) and `c7_shut` is read by the absence of the other four, as `c7_rung_wary` is by
the absence of the other two rungs: the third forge conversation is also the one a save with none of the five gets, and writing a separate
choice for that save would leave a choice no first playthrough can reach, which the reachability check refuses. **Four remain unread**: `c7_asked_about_riders`,
`c8_exit_tithe`, `c9_door_seal`, `c9_door_nails` (five, if reading by absence is not counted). The count comes from the compiled graph
(`flag_readers.txt` in the review pack), not from this table. Of the eight flags this package adds, `c10_cover_stair` and
`c10_cover_forge` are read by name in Chapter 10 and `c10_cover_voss` by the absence of the other two (as `c7_rung_wary` is), which is also the carrying
scene a save with no answer gets; the hub markers are bookkeeping; the three `c10_rites_*` are read at the hasty earth. None is unread.

## Planned for later chapters, not for this package (tracked, still unimplemented)

`c7_voss_called_them_off` (Ch13 Selvane, Ch18); `c8_chapel_story` (Ch13); `c8_exit_tithe` and `c9_door_seal` (Ch13's delegation scene);
`c9_door_nails` (Ch24); `c10_cover_*` and `c10_rites_*` (Ch13 rites, Ch24, the Ch30 roll of the watch, as in the table above); the rung
(Ch13, Ch18).

## Items and trophies

- No new item. `halvards_warden_chain` is granted by the one shipped choice, once, as before.
- One new trophy, "The Last Evening" (secret): earned by offering to hold the north gap and hearing Halvard refuse. `TrophyBackTest` counted 20
  trophy sites; it counts 23, because the same continue choice is written once for each of the hub's three exits (first, second and third visit),
  exactly one of which is ever visible.

## Canon held

- **Halvard's death is mandatory** and nothing before the fall speaks of it: no new scene before `halvard_falls` contains a word of death or burial, and
  Halvard never says he will die. He says the wall leans, that only he knows how to lean the other way, that it stands if he is on it and falls if he is
  not, and that he would have it otherwise if it could be. The fall, the last words and the fight that leads to them are exactly as shipped.
- Nobody else dies. The cover choice changes who carries him and what is said to Kaelen, not who lives.
- Voss's reason for leaving the Order is not written. The borrowed blade is never said to be a loan that will not be returned: Halvard "wants it
  back" in Voss's words on the wall, she is wearing it at the earth, and nobody mentions it.
- The shipped `chapter10_end` is the grave and ends "She just stays"; no new scene before it uses the word.
- The routing from Chapter 12 into Chapter 13 is unchanged and rests on `trusted_voss_fully` alone.
- Chapter 5: the blueprint's envoy-fate and Hold-reserve reads are not built. No `c5_` flag exists in the story data.

## Inventions to review (new, not in canon before)

Kestrel's rule that a warden's door is never left dark (Tam's lamp); the smiths' gate-book (a line for each keeper, written by someone who knows
them); the smiths' hummed tune and the forge relit at first light; the back galleries and hill road by which Edda's people can leave; Halvard's
chalk on the old tunnel mouth; the sentry's "Warden"; the dwarves who set new iron in the join. None contradicts a shipped scene.
