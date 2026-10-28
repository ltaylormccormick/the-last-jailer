# Chapters 13 to 16 consequence ledger

Status: written on `chapters-6-9-expansion` (from 6f1d69c); uncommitted. This is a design record, not player text. Every row says what the decision
does in the story **today**, which scenes **actually read** the flag it sets (found in the compiled story graph, not a plan), and what a later chapter
is **planned** to do with it. "Planned" is a proposal. None of it is implemented, none of it needs an engine or save-format change, and Chapters 17
onward are not touched by this package.

## How the package holds state

| Family or set | Members | Cleared by |
|---|---|---|
| `C13_ASK_FAMILY` | `c13_asked_openly` | the delegation's first choices (chapter entry), and the ask itself |
| `C13_DOOR_FAMILY` (alternatives) | `c13_door_open`, `c13_door_barred` | the delegation's first choices, and the door answers |
| `HUB_MARKERS` | the shared `hub_a`, `hub_b`, `hub_c`, `hub_second` (no new marker) | the choice that leaves the hearing |
| `C13_TELLING_FLAGS` | the four shipped Chapter 13 answers about Voss, the three shipped stances on who holds a gate, the two new families and the hub markers | `the_orders_delegation_trusted` and `_guarded`, all four choices |
| `C14_TELLING_FLAGS` | the two shipped ways of meeting the prisoner's voice, the three ways of telling Voss, the defeat marker | the three Chapter 13 endings (the "Listen." choices) |
| `C15_TELLING_FLAGS` | the two shipped ways of reading the dispatch, the three answers about Ilsevet, the defeat marker | `chapter14_end`, both choices |
| `C16_TELLING_FLAGS` | the two ways of meeting the prisoner's warning, the three answers about the shards, the defeat marker | `chapter15_end` |

### Replay clears added to shipped choices outside Chapters 13 to 16

New readers made these necessary. They add `clearFlags` only; labels, stat changes, flags set, items and destinations are untouched, and nothing in the
story changes for a first telling.

| Where | Clears | Why |
|---|---|---|
| Chapter 3, `the_answer` (all three answers) | `C3_OFFICE_FAMILY`: `accepted_fully`, `accepted_reluctantly`, `refused_once` | Chapter 14 reads the day Kaelen answered Halvard. Before, a replay that answered differently held two of the three |
| Chapter 3, `the_first_true_knock` (both answers) | `C3_KNOCK_FAMILY`: `listened_to_the_prisoner`, `refused_the_prisoner` | Chapter 14 reads the first knock |
| Chapter 11, `what_thessaly_decides` (all three answers) | `C11_THESSALY_FAMILY` | Chapter 13 tells Fenmoor's word by it |
| Chapter 12, `the_call_to_go_eager` and `_thorough` (both calls) | `C12_ILSEVET_FAMILY`: the call and the three ways | Chapter 15 tells Ashfall again by it |
| Chapter 12, `what_kaelen_does_with_her` (the three ways) | `C12_WAY_FAMILY`: the other two ways | same |

## Chapter 13: What the Order Allows (29 new scenes)

| Flag | Set at | Local effect | Read by (implemented) | Journal | Planned later acknowledgement (not implemented) |
|---|---|---|---|---|---|
| `c13_asked_openly` | `c13_parting`, "Ask what Ilsevet was before she was the Marshal, where Voss can hear." | Courage +1. Selvane answers once; the file will come "to the jailer, and he may show it to whom he likes" | `chapter14_end` -> `c15_waiting_open` / `c15_waiting_private` (the private case is the absence of the flag) | hidden (tone only) | **Ch18**: whether Voss heard him ask |
| (asking low) | `c13_parting`, "...quietly, once Voss has turned away." | Honour +1; no flag | `c15_waiting_private` | n/a | none |
| `c13_door_open` | `c13_door`, "Keep the door open to them, night and day." | Honour +1. Edda: "There will be nights you wish you had said the other thing." | `what_was_taken` -> `c16_aftermath_open` | decisions, `stair_door` group | **Ch17** (what the Hold can spare), **Ch24**, the **Ch30** roll of the Hold |
| `c13_door_barred` | same, "Bar it at night, and tell them why." | Courage +1. Edda: "A reason I can repeat" | `what_was_taken` -> `c16_aftermath_barred` | decisions, `stair_door` | same |
| `hub_a`, `hub_b`, `hub_c`, `hub_second` | the hearing's table | which matters have been heard | the hearing's own continue choices | hidden (bookkeeping, as before) | none |

What the existing flags now do in Chapter 13 (all implemented; none changes a route, only which scene a reader meets):

| Flag | Read by | What the reader meets |
|---|---|---|
| the choice at `what_voss_answers` (it sets `kaelen_vouched_for_voss` or `trusted_voss_account`, which stay unread by name) | `what_voss_answers` -> `c13_hearing_vouched` / `c13_hearing_account` | Selvane turns to him, or speaks to Voss with him in earshot; two of up to three matters, the jailer's choice |
| `c7_voss_called_them_off` | hearing -> `c13_m_riders_called` / `c13_m_riders_not` | the Order's view of the loyalists at the black door: an order obeyed that had no right to be given, or people who were still hers |
| `c8_exit_tithe` / `c9_door_seal` | hearing -> `c13_m_seal_spent` / `c13_m_seal_door` | the signet shown to a collector who knew the face, or the mark the cell ward now remembers. The two cannot both be true: the door choice forbids a spent signet |
| `sealed_greymoor_personally` / `reported_greymoor_honestly` / `left_greymoor_as_is` | hearing -> `c13_m_greymoor_*` (left is read by the absence of the other two) | a stopgap that holds and masons in the spring; the plain account that reached the Order through a market-town factor; a gate left as found and "a habit" |
| `c7_rung_*` | `c13_parting_*` -> `c13_night_wary` / `working` / `trusted`; the honest answer moves the rung one step (the top has none to move) | Voss that night, three ways |
| `c8_chapel_story` | `c13_night_trusted` -> `c13_night_chapel` (top rung only) | Voss hoped the chapel would still be standing, and it was; nothing more |
| `c10_cover_stair` / `c10_cover_forge` / the absence of both | night -> `c13_eve_stair` / `c13_eve_forge` / `c13_eve_voss` | who comes to him before the rites, and what was promised at the last evening paid |
| `c10_rites_name` / `_deeds` / `_plain` (none: straight to the rites) | eve -> `c13_book_name` / `_deeds` / `_plain` | the smiths answer the line Kaelen wrote, beneath it in the gate-book |
| `invited_thessaly_to_stonebeard` / `pledged_support_to_fenmoor` / the absence of both | the rubbing -> `c13_thessaly_attended` / `_pledged` / `_silent` | Thessaly at the carving and gone before the question; her answer by rider; or no word |
| `c7_hold_terms` / `_refused` / `_hidden` (Tam is always with them: see below) | Thessaly's scene -> `c13_door` | Tam's lamp at the new mark, Edda's question about the door |

Tam is always met before a promise to Edda is made (proved in `Chapter1316FamilyProofTest`), so wherever Edda's people are, Tam is. There is no
separate Tam test.

## Chapter 14: Furniture (12 new scenes)

| Flag | Read by (implemented) | What the reader meets |
|---|---|---|
| the answer at `who_stands_with_stonebeard` (it sets `began_training_new_wardens`, `stood_watch_rather_than_delegate` or `deferred_the_question`, which stay unread by name) | the three Chapter 13 endings each lead to `c14_gate_training` / `_watch` / `_deferred` | the nights at the gate as the prisoner finds them: a household to listen to, company that makes it go quiet, or one man alone |
| `listened_to_the_prisoner` / (absent) | `the_offered_glimpse` -> `c14_knock_listened` / `c14_knock_refused` | the stone shows the first knock before the vision: "You listened", or "You left before I finished" |
| `accepted_fully` / `accepted_reluctantly` / (absent both) | knock -> `c14_office_full` / `_reluctant` / `_refusedonce` | the day he answered Halvard, as he answered it |
| `c7_rung_*`, on the choice that tells her or writes it (`told_voss_everything` and `recorded_the_vision_first` stay unread by name) | `what_kaelen_remembers` -> `c14_voss_heard_wary` / `working` / `trusted` | Voss has what he saw, at her own rung (the text reads the same whether he told her or wrote it) |
| the choice that keeps it (`kept_the_vision_to_himself` stays unread by name) | `what_kaelen_remembers` -> `c14_voss_withheld` | she does not ask |

No flag is added. The rung does not move in this chapter. Nothing says what the prisoner is: the promise in "The Names It Won't Give" is not paid.

## Chapter 15: Old Debts (9 new scenes)

| Flag | Read by (implemented) | What the reader meets |
|---|---|---|
| `c13_asked_openly` / (absent) | `chapter14_end` -> `c15_waiting_open` / `c15_waiting_private` | Voss on how he asked Selvane for the file |
| (none) | both "Keep reading." choices -> `c15_the_reply` | Ilsevet's own request, written before the ward failed, and the form that noted it |
| `c7_rung_*` | `c15_the_reply` -> `c15_voss_reads_wary` / `working` / `trusted` | Voss reading the pages without a word; she recognises how the Order closes a file, nothing about herself or Ashwell |
| `warned_ilsevet_directly` or `appealed_to_ilsevets_conscience` / `left_ilsevet_to_her_wreckage` / (none of the three) | `what_it_made_of_her` -> `c15_echo_reached` / `_left` / `_refused` | Ashfall coloured by how he met her there |

No flag is added. The choice at `old_debts` is still the player's and unchanged; Ilsevet's conflict is not resolved.

## Chapter 16: What Was Already Taken (6 new scenes)

| Flag | Read by (implemented) | What the reader meets |
|---|---|---|
| `c7_rung_*`, on the choice that splits the shards (`dispersed_the_shards` stays unread by name) | `what_to_protect` -> `c16_handing_wary` / `working` / `trusted`; the free answer moves the rung one step (the top has none to move) | a piece put in Voss's hand |
| `c13_door_open` / `c13_door_barred` / (neither) | `what_was_taken` -> `c16_aftermath_open` / `_barred` / `_none` | how the thief came in and who paid: no one else is hurt in any telling |

Four shipped phrases were corrected by the author's decision, because a player holds two shards or three (the Seventh Door shard comes only from helping Ilsevet): "Four fragments" in `what_to_protect` now reads "Several fragments"; "between three keepers" in `the_reliquary_thief_dispersed` reads "between keepers"; and in both `the_prisoners_warning_pressed` and `_patient` "the frame at Ashfall" reads "whatever came away from the frame at Ashfall", so the sentence names where a shard may have come from without saying Kaelen took one there. They are the only changes to shipped prose in Chapters 13 to 16, each held as one narrow exception by `Chapter1316ShippedPinTest`. No new scene gives a number of shards, riders or years; the aftermath says only that the pieces the thief carried off are "more than one", which Chapter 17's two fitted fragments need.

## Obligations carried over from the Chapters 10 to 12 ledger

| Flag or family | Assigned | Outcome |
|---|---|---|
| `c10_cover_voss` / `_stair` / `_forge` | Ch13 rites | **reader written**: `c13_eve_*`. Ch24 and the Ch30 roll remain planned |
| `c10_rites_name` / `_deeds` / `_plain` | Ch13 proper rites | **reader written**: `c13_book_*` |
| `c7_voss_called_them_off` | Ch13 Selvane | **reader written**: `c13_m_riders_*`. Ch18 remains planned |
| `c8_chapel_story` | Ch13 | **reader written**: `c13_night_chapel`, minimal by decision |
| `c8_exit_tithe`, `c9_door_seal` | Ch13 | **readers written**: `c13_m_seal_*` |
| Greymoor verdict (`sealed_greymoor_personally` and the other two) | Ch13, then 24 and 30 | **reader written for Ch13**: `c13_m_greymoor_*`. Ch24 and Ch30 remain planned |
| Tam's lamp | Ch13 rites | **written**: `c13_door` |
| `pledged_support_to_fenmoor`, `invited_thessaly_to_stonebeard`, `left_fenmoor_uncertain` | none assigned | **assigned to Ch13**: `c13_thessaly_*` |
| `chose_to_help_ilsevet`, `refused_to_help_ilsevet`, `warned_ilsevet_directly`, `appealed_to_ilsevets_conscience`, `left_ilsevet_to_her_wreckage` | none assigned | **assigned to Ch15**: `c15_echo_*` |
| `listened_to_the_prisoner`, `refused_the_prisoner`, `accepted_fully`, `accepted_reluctantly`, `refused_once` | none assigned | **assigned to Ch14**: the memories |
| `c9_door_nails` | Ch24 | **stays planned**: no Ch13 to 16 reader |
| `c7_asked_about_riders` | none | **stays a hidden record, no reader** |
| `c7_rung_*` | Ch13, Ch18 | **readers written** in Chapters 13 to 16; Chapter 18 remains planned |
| `accepted_dark_aid`, `refused_dark_aid_again` | none | **not read in this package** (the optional memory was left out by decision); Chapters 5 and 30 read them as before |

## Unread flags, from the compiled graph (`generated/flag_readers.txt` in the review pack)

"Read by name" means a choice requires or forbids the flag. A scene routed by which choice was taken, or by the absence of other flags, does not read the flag it
leaves behind; those flags stay unread and are listed so they are not mistaken for read.

* **Chapters 7 to 9 flags the earlier ledger tracked:** `c7_voss_called_them_off` (now read by `c13_hearing_*`), `c8_chapel_story` (`c13_night_trusted`), `c8_exit_tithe` and
  `c9_door_seal` (`c13_hearing_*`) are **read**. Two remain unread: `c7_asked_about_riders` (a hidden tone record, by decision) and `c9_door_nails` (planned for Ch24).
  Of the four the Chapters 10 to 12 ledger left unread, two are now read.
* **Chapters 11 and 12:** read by name now: `pledged_support_to_fenmoor` and `invited_thessaly_to_stonebeard` (the rubbing), `warned_ilsevet_directly`,
  `appealed_to_ilsevets_conscience`, `left_ilsevet_to_her_wreckage` (`what_it_made_of_her`). Read only by the absence of the others: `left_fenmoor_uncertain`,
  `refused_to_help_ilsevet`, `chose_to_help_ilsevet`.
* **Chapters 3 and 4:** `listened_to_the_prisoner` is read by `the_offered_glimpse`, `accepted_fully` and `accepted_reluctantly` by `c14_knock_*`; `refused_the_prisoner`
  and `refused_once` are read by the absence of the others; `bound_to_the_gate` and `refused_dark_aid_again` are not read; `accepted_dark_aid` is read as before (Chapters 5 and 30).
* **Chapters 13 to 16's own flags:** `c13_asked_openly` (`chapter14_end`'s two copies), `c13_door_open` and `c13_door_barred` (`what_was_taken`'s copies), `stood_with_voss` and
  `let_voss_go_alone` (`what_voss_answers`) are read. The other shipped flags of the four chapters (the Voss verdict, the three stances on who holds a gate, the two ways
  of meeting the prisoner, how the vision was shared, how the dispatch was read, the three answers about Ilsevet, how the prisoner was pressed, the three answers about the
  shards) are set and still unread by name; their consequences are carried by the scenes the choice leads to. The graph counts 68 flags set in Chapters 7 to 16 that
  no choice requires or forbids, most of them shipped bookkeeping and defeat markers.

## Planned for later chapters, not for this package (tracked, still unimplemented)

`c13_door_open` / `c13_door_barred` (Ch17 "what the Hold can spare", Ch24, the Ch30 roll of the Hold); `c13_asked_openly` (Ch18, whether Voss heard);
`c9_door_nails` (Ch24); the Greymoor verdict (Ch24, Ch30); `c10_cover_*` and `c10_rites_*` (Ch24, the Ch30 roll); the rung (Ch18);
`began_training_new_wardens`, `stood_watch_rather_than_delegate`, `deferred_the_question` (Ch17); `committed_to_proving_her_wrong` and the other Chapter 15
answers (Ch18, "Ashwell already took enough from you").

## Items and trophies

- No new item. `rubbing_of_halvards_mark`, `record_of_ashwell` and `shard_of_the_first_ward` are granted by the same shipped choices, once, as before.
- No new trophy. `TrophyBackTest` still counts 23 trophy sites.

## Canon held

- Voss's reason for leaving the Order is not written, hinted at or promised. The chapel is the only piece of her history touched: she hoped it would still be
  standing, it was, and she will give no more tonight. She recognises how the Order closes a file, not a place or a person.
- Chapter 14 does not say what the prisoner is, who cut the six wards apart, or what they were; its promise in "The Names It Won't Give" is unpaid.
- Ilsevet's conflict is not resolved. Her request was written before Ashwell failed and answered with a form; nothing says she was right.
- The verdict is unchanged: "content to let you be the ones standing between six and seven".
- Halvard's name is carved, the rubbing is taken once, and the shipped rites scene is untouched.
- Combat, routing out of Chapter 12 into Chapter 13 (on `trusted_voss_fully`), and the six endings are unchanged.

## Inventions to review (new, not in canon before)

The gate-book's second hand (the smiths answering a keeper's line: a long list under a short line, a stroke for each smith who was there, or the word the
Hold had before Kaelen); the factor in a market town through whom Kaelen's Greymoor account reached the Order; the cadence the Order teaches, in which Voss
stood the loyalists down; Selvane's masons in the spring; the Order's form with its ticked box ("noted") and Ilsevet's request about the hill wards, the stone
that sweated in a dry month; Edda's question about the door; Tam's lamp at the new mark; the thief's way in, by the door or by the old tunnels. None
contradicts a shipped scene.
