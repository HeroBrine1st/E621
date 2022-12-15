[![Snapshot build](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml/badge.svg)](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml)

# E621

Android client for e926.net and e621.net

This project is a work in progress. It may contain bugs, slow code or even features. Use at your own
risk.

Development is focused on internal code changes, but new features are rarely seen too. Get latest
snapshot [here](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml) (select
last passed workflow and see artifacts). Keep in mind that snapshot builds aren't compatible with
release builds (they use different signing keys), but you can reinstall if you want.

# Features

- [x] Search posts
    * [x] Search by arbitrary tags
        * Note that tags you type are sent to server as is, so use underscores when needed. This
          also means that you can use ``~`` (OR), ``-`` (NOT) and metatags (for
          example, ``type:png``)
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
- [ ] SWF - flash won't be supported neither in near nor far future (but PRs are welcome if they're
  safe in terms of vulnerabilities that Adobe Flash had)

# License

This application is released under GNU GPLv3 (see [LICENSE](LICENSE)).
Some of the used libraries are released under different licenses.