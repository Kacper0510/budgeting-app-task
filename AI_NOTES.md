## Overview

I used AI in some ways while building the project, but I have mostly written the code myself (as the costs of using agentic AI are rising, see new GitHub pricing as an example). I mostly used it to autocomplete some code, to ask questions about how to achieve certain things and to generate repetitive/boilerplate code such as OpenAPI annotations, React styling or integration tests. AI also helped with debugging some issues, but often I had to figure out the root cause of the problem myself.

## Backend implementation

I mainly used AI to ask questions about Spring Boot, as I was learning it while building the project. Example prompts include:

> what packages should i choose in spring boot initializer if i need a simple rest api that stores data inside a postgres db

> how do i access the database, give me the simplest example with one model, one get endpoint and one post endpoint

> how do i create table relations then

> and how to store dates? java has many classes for those, which is recommended?

> what dto constraints can i use? how do i check for non null or explicitly allow nullability

## Integration testing

I used AI to generate most of the integration tests, after having written the backend code and some example tests.

In this part of the project, I have met a big issue with H2 database in tests - I tried to debug it with AI and it helped a bit, suggesting some possible solution, but ultimately I had to figure it out myself. The issue was that H2 generates invalid table constraints for Java enums while using the newest version of the library (and yet, they still haven't put out a fix after like half a year!), so the solution was to downgrade it - a really nasty bug that makes you doubt yourself while it isn't even your fault...

## Frontend implementation

As I know a bit about React, I wrote most of the logic myself while using AI to autocomplete some code and to generate Tailwind styling.
