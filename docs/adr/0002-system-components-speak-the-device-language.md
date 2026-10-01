# System components speak the device language, not English

Every string the app itself owns is English and lives in `values/strings.xml`, but the Material date picker opened by the Form's date button renders its own chrome — the "select date" headline, the month name, the weekday initials — in whatever language the phone is set to. We leave it that way, deviating from the spec's "English-language interface" for components the app does not author.

## Considered options

Filtering locales at build time (`androidResources { localeFilters += listOf("en") }`) would strip the library translations out of the APK and make the calendar English everywhere. Rejected because the app is personal and installed on one phone: a date picker that matches every other date picker on that phone is worth more than a uniform language, and the filter would also silently affect any library text added later.

## Consequences

- The spec's user story "an English-language interface" holds for strings the app writes, not for platform and library components it merely hosts.
- A reviewer seeing Russian in a screenshot of the calendar is looking at a decision, not a defect.
- The decision is cheap to reverse: it is one line in `app/build.gradle.kts`, with no data or API consequences.
