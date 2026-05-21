# Checklist of stuff for the site

- [ ] Interactive sandbox with the actual pika file?
  - [ ] Using the Ace Editor, making sessions instatiated for course objectives to create a small learning path for users
- [ ] Full mini-course of the features that gets user comfortable (Using ACE as backing with session control?)
    - [ ] Migrations
    - [ ] different query styles
    - [ ] configuration
    - [ ] Connecting with standard error/logging interfaces
    - [ ] Connection pooling with HikariCP? 
    - [ ] mapping configurations
- [ ] Easier affordances for interacting (rust style web UI)?
- [ ] Fully update docs, explore full feature lists, with technical deep dives
  - [ ] Covers errors
  - [ ] Cover coercions
  - [ ] Cover CLI tooling (migrations etc.) 
- [ ] Maven hover tooltips for all important functions (Smaller extensions of the docs) 


# Theoretical Ideas / Todo on the project
- [ ] some system for confirmation and general type correction for our where strings. We have some pretty loose coupling right now, and even though we are an SQL exposed ORM, I think it is reasonable to have some basic checks on typing and structure to the where clause strings. (also makes errors much easier to catagorize and actually just make less work of the error/log system) and just focus errors less on runtime?
- [ ] make really easy integration with existing connection pool architecture like HikariCP (easily wrapping pooled connections) and makes pika pluggable to existing web server stuff that is popular right now
- [ ] updates / deletes that we played with a bunch are still singular, we could easily switch to the preparedstatement methodls of .addBatch and execute to make our blunk stuff easily more preformant