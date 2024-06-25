[![Snapshot build](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml/badge.svg)](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml)

# E621

Android client for e621.net

This project is a work in progress. It may contain bugs, slow code or even features. Use at your own
risk, but it is harmless as this client is mostly read-only :-)

e926 is also supported, but there's no switch in settings, i.e. rebuild required, although you can
decompile, change url and assemble back. It is complicated though.

Releases are available [here](https://github.com/HeroBrine1st/E621/releases).

Get latest **development** snapshot [here](https://github.com/HeroBrine1st/E621/actions/workflows/snapshot-build.yml) (select
last passed workflow and see artifacts). Keep in mind that snapshot build is a different application (has different package name), so you can have both snapshot and release build on the same device.

# Features

TL;DR you can browse, search and save ("Favorite" button) posts and view wiki. This client is mostly
read-only and has some harmless bugs.

- [x] Search posts
    * [x] Search by arbitrary tags
        * You can use ``~`` (OR), ``-`` (NOT) and meta-tags (for example, ``type:png``), as tags
          sent to the API almost as is. Underscores are hidden and instead replaced with spaces in
          UI.
    * [x] UI to sort search results (`order` meta-tag)
    * [x] UI to filter by rating (safe-questionable-explicit)
    * [x] UI to filter by file type
    * [x] Search favourites
        * [x] Server-side
        * [ ] Local
    * [ ] "Micro-search" (fast local search through current results)
  * [x] Pools support
- [x] Safe mode on (probably) every screen with images, enabled by default with disclaimer on disable.
- [x] Autocomplete tags
- [x] Authorization by API key
  - ~~Authorization by regular username and password~~ failed to circumvent CSRF despite having no browser sandboxing :-/
    For that reason, this feature is no longer planned.
- [x] Blacklisting
    - [ ] Fancy UI to configure blacklist
    - [x] Support for extended syntax (still incubating)
- [ ] Hiding posts at the click of button
- [x] Favorites
    * [x] Add and remove
    * [x] View
- [x] Post screen
    * [x] Comments
        * [x] Read
            * [x] Basic formatting (bold, italic, quotes)
            * [ ] Advanced formatting (sub/sup, colors, inline post previews, links)
    * [x] Tags
        * [x] Add to/exclude from search
        * [x] View wiki
            * [x] Formatting (the same as in comments)
- [x] Up/down score
- [x] Basic SOCKS5 proxy support. **WARNING**: it *will* fall back to direct connection if proxy is
  unreachable in any way
    - [ ] Preference to control fall back behavior

## File types supported

- [x] JPG
- [x] PNG
- [x] GIF
- [x] WEBM
- [x] MP4
- SWF - can't be supported

# License

This application is released under GNU GPLv3 (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). The used
libraries are released under different licenses, mostly Apache 2.0 and some under BSD 3-clause and
MIT License.

If you found any violation of your copyright, contact HeroBrine1st Erquilenne. See details below.

# Contact

To directly contact me, HeroBrine1st Erquilenne:

1. Make an issue - optional, but it will speed up my answer and so highly recommended, as I don't
   check email inbox every day.
2. Write to project-e621-android@herobrine1st.ru - be advised that I will respond you from other
   email address as this address is a proxy address (I'm just too lazy to set up a proper e-mail
   server).

Note that this email address is not for bugs, feature requests and anything that can be discussed
publicly. Use Github Issues instead.

P.s. (If you're curious) I use email on own domain primarily to protect from spammers: that is, I
can easily block spam and know which site leaked my email, potentially improving my security by
detecting leaks before they get into public, for example, when only addresses are got to spammers
and remaining data is still being processed.
