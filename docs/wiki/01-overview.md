# 1. Overview

[← Wiki home](README.md) · Next: [Design →](02-design.md)

## The problem

Teaching a BJJ class means juggling a lesson plan in one place, a round timer in another app, attendance on paper or in memory, and technique notes somewhere else. All of that happens while standing on a mat, often with one free hand and students waiting. Every switch costs class time, and it's hard to see what was taught last week.

## The user

Me. I teach kids', adults' and private classes from an Android phone. I plan on my computer, teach from the phone, and wanted a tool that feels like my brand instead of a generic fitness app. The brief lists the real situations the design has to answer ([product brief](../01-product-brief.md)):

| Moment | What I need |
|---|---|
| Walking onto the mat | One tap to start the class |
| Mid-class, standing | Current segment, time left and what's next, readable at arm's length |
| Live rounds | A timer everyone can read and hear; start and pause without looking twice |
| Students arriving | One tap per student, with instant undo |
| After class | Record what worked |

## What's built

| Area | Status |
|---|---|
| Today dashboard, day/week planner, class editor | Built (Sprint 01) |
| Lesson builder with reusable templates | Built (Sprint 01) |
| Class Mode with segment countdown and post-class notes | Built (Sprint 01) |
| Interval timer with presets and cues | Built (Sprint 01) |
| Students, programs, attendance with undo | Built (Sprint 01) |
| Technique library, settings, JSON backup export | Built (Sprint 01) |
| Timer that keeps running and sounding with the phone locked | Built (Sprint 02) |
| Private video library: import, labels, clips, a player with loop, slow motion and mirror | Built (Sprint 03) |
| Signed personal builds that install over the last one from a fixed link | Built |
| Restore from backup | Not built |
| Desktop companion and local Wi-Fi sync | Designed ([sync architecture](../06-sync-architecture.md)), not built |

## Privacy

The app stores only what serves teaching: name, belt, an under-18 flag instead of a birth date, programs, attendance and one teaching note. There are no contacts, photos or medical details. Android cloud backup is off; backups are exports I choose to make. No real student data is in this repository; sample data is fictional.
