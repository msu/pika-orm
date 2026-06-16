---
layout: default
title: "Philosophy of PikaORM"
description: "The design principles behind PikaORM — Layered API Design, simplicity, and the vision of Carson Gross."
active_page: philosophy
permalink: /pages/philosophy/
---

# Philosophy of PikaORM

> PikaORM was brought to you by Carson Gross with affiliation from Big Sky Software, The HyperMedia Research Group, and The Open Source Group at Montana State University, with student assistance from Will Mitchell, Dylan Shaffer and Zachariah Craven. Created in Summer 2025, Carson had a vision to kill two birds with one stone: creating a sleek and small ORM to display for his 440 Database Systems class for Montana State University, as well as open source the software for anyone to use.

The philosophy around Pika has always been simple. The goal is to design an API that people can actually use, understand, and modify to their liking. In particular, the API design of the system follows a concept of [Layered API Design](https://www.youtube.com/watch?v=dTstnhS3moc&ab_channel=MontanaProgrammers) discussed by Carson at the 2025 Big Sky Dev Con. 

The idea is to create stepping stones of complexity for APIs. This allows a new user to worry little about customization or system knowledge, being able to use the API out of the box, while building additional onion layers of complexity as people start to understand the API more or want to change it in some way. 

PikaORM offers an incredibly intuitive basic design for people to interact with their data, using essentially all methods to invoke the querying actions. Beyond that, however, users can change their own mappings, metadata, and logging settings all on their own, simply implemented with just a few more methods. All of this general principle comes from [Carson's ultimate coding philosophy of complexity being bad](https://grugbrain.dev), which has his full thoughts on coding paradigms.
