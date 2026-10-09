# An Import collapses a day to its earliest Record and confirms before writing

A CSV Import reads one Record per line. Where several lines fall on the same calendar date, the line with the earliest time of day wins and the rest are dropped; a line carrying no time of day counts as the end of its day, so a timed line always beats an untimed one. Nothing is written until the user confirms a count of how many Records the Import will add, how many it will replace, and how many lines it skipped.

## Considered options

For the collapse, the alternatives were keeping the day's latest line, averaging the day's lines, and storing several Records per day. The last is ruled out by ADR-0001. Averaging invents a Weight that was never measured, and makes a second Import of the same file produce a different History. Latest-of-day is the genuine rival — it loses to earliest only because a morning weigh-in before eating is the figure comparable across days, which is the whole point of the History.

For the write, the alternatives were replacing occupied dates silently (what `save` already does), asking per date, and skipping occupied dates. Silence would destroy Records the user cannot get back without noticing. Per-date questions are unusable on a file of hundreds of lines. Skipping occupied dates would make re-importing a corrected file pointless.

## Consequences

- The time of day is read from the file, used to pick a winner, and then thrown away: it never enters the Record store, and the Import cannot be reversed back into the file it came from.
- The counts in the confirmation require parsing the whole file before writing anything, so the file is held in memory. Reading is capped at 1 MB and a larger file is refused rather than read.
- The write is one transaction: an Import either lands whole or not at all. There is no undo — the confirmation stands in for it.
- Re-importing a corrected file is the supported way to fix Weights in bulk, because occupied dates are replaced.
- Lines the Import cannot read — an unparsable date or Weight, a Weight outside 1-500 kg — are counted and skipped, never fatal: one bad line in a foreign export must not block the rest.
