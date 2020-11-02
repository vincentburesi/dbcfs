# DBCFS

Discord Bot Controlled Factorio Server is an all integrated, docker-embedded system that aims at setting up a server managable by people without any technical knowledge through discord messages and some simple http pages.
This repository is only a POC (proof of concept) and will serve as a base to prove the whole system viability.

## How to start the server

- Make sure Docker is installed on the machine
- Write the configuration file: `src/main/resources/application.yml` (you can use the example file in the same folder as base)
- You can start the server using the `run.sh` file

## About the project

As I now work without time constraint I will focus on refactoring and improving the code.

I am currently doing a large refactor to fix many issues and prepare the projet for public release.
The project will be broken down and rebuilt from scratch during this period and therefore won't be usable until first public release.

I will try to keep a regular update schedule (about once a week) and will keep this section updated with changes.

## Planned tasks

- [X] Notifier rework:
    - [X] Notifier flush system (how the hell did I miss this?)
    - [X] Group Notifier and log system
    - [X] Use synchronous update in coroutine to avoid stacking the updates
    - [X] Separate status update from messages (to allow reuse of functions)
- [ ] Moving from Mustache to Pebble to benefit from template inheritance and other cool stuff
- [ ] Security overhaul
    - [ ] Pass on code and check for possible attacks
    - [ ] HTTPS support (Let's encrypt/Certbot integration ?)
- [ ] Global refactoring
    - [ ] Isolate all string literals in a separate module (and lay the groundwork for multiple languages support)
    - [ ] Generic download function
    - [ ] Systematic use of exceptions and coroutines
- [ ] Reorganize dependencies (headless execs and access rights linked to profile, client package put in commons)
    - [ ] Multiple server processes running in parallel
- [ ] Run locally (dynamic DNS integration and router configuration tutorial ? tunneling ? other ?)
- [ ] Proper version tags
- [ ] Reduce mod index to simple ID/URL and retrieve other infos from WS on the fly

This list will be updated and expanded over time.
