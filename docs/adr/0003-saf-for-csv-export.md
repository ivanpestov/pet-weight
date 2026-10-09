# CSV export writes through the Storage Access Framework, not a share intent

Export History saves a CSV file via `ACTION_CREATE_DOCUMENT` (the system "save as" picker): the user names the file and chooses its location directly, and the app keeps no `FileProvider` and asks for no storage permission.

## Considered options

The alternative was `ACTION_SEND` through a `FileProvider`-backed temporary file, handing the CSV to a share-sheet chooser (Drive, email, messaging) — the more common pattern for "export" actions on Android. Rejected because it adds a manifest-level `FileProvider` and a cache file whose lifetime must be managed, for a personal app whose actual need is a durable file the user puts somewhere specific, not a one-off share.

## Consequences

- There is no `FileProvider` in the manifest; adding "share the export directly to an app" later means introducing one.
- The exported file's path is whatever the user picked in the system dialog — the app has no record of it afterward.
