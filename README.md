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
          also means that you can use ``~`` (OR), ``-`` (NOT) and meta-tags (for
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
  safe in terms of vulnerabilities which Adobe Flash had)

# License

This application is released under GNU GPLv3 (see [LICENSE](LICENSE) and [NOTICE](NOTICE)). The used
libraries are released under different licenses.

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
