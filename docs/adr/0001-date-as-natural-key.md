# The Record Date is the natural key, and no time is stored

A Record holds a calendar date only, with no time of day, and that date is its unique key: at most one Record exists per day. Saving again against an occupied date replaces the existing Record, with a confirmation in the UI.

## Considered options

The alternative was to store a moment in time (date plus time) and allow several Records per day: that is how most weight trackers work, and a weigh-in genuinely happens at a moment rather than "on a day". Rejected because without a displayed time two Records on the same day are visually indistinguishable in the History and read as a defect, and the scenario itself — weighing in both morning and evening — is not needed for this app.

## Consequences

- The database schema and the whole UI assume date uniqueness; moving back to timestamps would require a data migration and a rework of both the History and the entry form.
- Editing a Record Date can collide with an existing Record, so the UI must ask for confirmation before replacing it.
- Future dates are not forbidden, so "the latest Record" and "the latest Record not after today" are different things: the form prefills from the second.
