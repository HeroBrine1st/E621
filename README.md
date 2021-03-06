# E621

Android client for e926.net and e621.net

This project is a work in progress. It may contain bugs, slow code or even features. Use at your own
risk.

Get latest release [here](https://github.com/HeroBrine1st/E621/releases).
These releases support only e621.net and rebuild required to change it (see in app/build.gradle.kts
android.buildTypes)

# Features

- [x] Search posts
    * [x] Search by arbitrary tags
      * Note that tags you type are sent to server as is, so use underscores when needed. This also
        means that you can use ``~`` (OR), ``-`` (NOT) and metatags (for example, ``type:png``)
    * [x] Sorting
    * [x] Search by rating
    * [x] Search favourites
        * [x] Server-side
        * [ ] Local
    * [ ] Search by file type
- [ ] Autocomplete tags
- [x] Authorization
- [x] Blacklists
- [x] Favorites
    * [x] Add and remove
    * [x] View
- [x] Post screen
    * [x] Comments
    * [x] Tags
        * [x] Add to search
- [ ] Up/down score

## File types supported

- [x] JPG
- [x] PNG
- [x] GIF
- [x] WEBM
- [x] MP4
- [ ] SWF - flash won't be supported neither in near nor far future

# Build instructions

## Android Studio

1. Open project
2. Build (on toolbar)
3. Generate signed bundle / APK
4. Follow the instructions
