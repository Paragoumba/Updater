# Updater
A snippet that tends to represent an application's updater.

The /repo dir represents the online dir which contains the update files.
The /local dir represents the installed update.

/repo/latest files contains the latest update version.

Each dir in /repo contains update's files and each dir contains an update file that contains all the hashes of the update's files.

This app doesn't need a custom server, just a web server to deliver files.
