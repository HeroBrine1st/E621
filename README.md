# E621

Read-only Android client for e621.net. e926 is also supported but requires rebuild.

Releases are available [here](https://github.com/HeroBrine1st/E621/releases).

This application is released under GNU GPLv3 (see [LICENSE](LICENSE) and [NOTICE](NOTICE)).

# Features

TL;DR you can browse, search and save ("Favorite" button) posts and view wiki. This client is mostly read-only.

- View posts
    - All formats except Adobe Flash  
      Parity with website is kept on best-effort basis, usually after someone encounters new format
    - Zoom images
    - View max resolution file
    - Post listing supports staggered grid better than one on website as unstable feature
- Search posts
    * Search by arbitrary tags: fancy UI and raw query support (toggleable in topbar while on search screen)
    * UI to sort search results (`order` meta-tag)
    * UI to filter by rating (safe-questionable-explicit)
    * UI to filter by post type
    * Search favourites
        * Server-side
        * [ ] Local
    * [ ] "Micro-search" (fast local search through current results)
    * Pools support
- Safe mode on (probably) every screen with images, enabled by default with disclaimer on disable.
- Autocomplete tags
- Authorization with API key (auth with login/password is not provided by API thus not planned)
- Blacklisting, with pretty basic query filtering (should be enough for most)  
  It fetches blacklist from website on first auth, but there's no synchronisation.
    - [ ] Fancy UI to configure blacklist
- [ ] Hiding posts at the click of button
- Favorites
    * Add and remove
    * View
- Post screen
    * Comments
        * Read
            * Basic formatting (bold, italic, quotes)
            * [ ] Advanced formatting (sub/sup, colours, inline post previews, links)
    * Tags
        * Add to/exclude from search
        * View wiki
            * Formatting (the same as in comments)
- Up/down score
- (Deprecated, unsupported and likely broken) Basic SOCKS5 proxy support. **WARNING**: it *will* fall back to direct
  connection if proxy is
  unreachable in any way
    - [ ] Preference to control fall back behaviour