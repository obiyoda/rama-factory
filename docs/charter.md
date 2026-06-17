# Rama Factory Charter

## Mission

Build a Clojure software factory for Rama-backed applications that gives teams
Laravel-level developer experience while using agent skills to make distributed
systems work repeatable, reviewable, and economically useful.

## Product Thesis

Laravel's moat is not only framework code. It is coherent defaults, a memorable
CLI, a practical ecosystem, excellent documentation, and packages that solve
boring application problems.

Rama Factory should apply that model to Clojure and Rama:

- Clojure is the implementation language and extension language.
- Rama is the durable state, streaming, materialized view, and query engine.
- Devenv is the reproducible project entrypoint.
- Zodiac is the default web runtime.
- Agent skills encode how factory roles build, review, validate, and extend
  applications.
- Extensions package reusable application capabilities with code, Rama
  blueprints, generators, validation rules, tests, docs, and skills.

## Users

- Developers building Rama-backed applications who need clear conventions and
  faster project setup.
- Teams that want AI agents to contribute to real application work without
  losing ownership, review boundaries, or validation discipline.
- Organizations that need auditable generation, testing, and refactoring of
  stateful distributed systems.

## Principles

- Prefer explicit distributed-system design over magic.
- Assume any successful app may eventually need coordination, history, audit,
  replay, and scale; start on a stack that can grow without a rewrite.
- Keep Rama concerns visible: depots, PStates, partitioning, query routing,
  topology behavior, idempotency, fault tolerance, and read-path cost.
- Make the local developer experience boring and reliable through devenv tasks,
  processes, services, and tests.
- Treat agents as role-bound collaborators with skills, worktrees, durable
  handoffs, and verification gates.
- Give agents named personas so humans can understand ownership, style, skills,
  and event attribution.
- Build the docs site in the framework as the first dogfooded application.
- Let extensions solve common application needs without hiding the underlying
  state model.

## Non-Goals

- Do not copy Laravel's ORM/database mental model.
- Do not put all web behavior inside Rama modules.
- Do not make agents bypass tests, commits, role ownership, or handoff state.
- Do not require a hosted service for the local factory to work.

## Open-Source Strategy

The project should be useful as an open-source starter and seed factory before
any hosted or commercial surface exists.

The core value is:

- starter repositories for new Rama-backed apps
- copy-owned extension seeds that developers can inspect and modify
- a factory-floor dashboard that shows agent work flowing through Rama
- named agent personas that make factory activity legible to humans
- agent skills and role workflows that help build app ideas safely
- real Rama module examples, tests, and validation gates
- strong docs that explain the generated architecture

The project should feel more like shadcn-style seeds than a closed framework
runtime: pull in a point-in-time version, own the copied source, and evolve it
for the business idea being built.
