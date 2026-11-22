# 6. Challenges and lessons

[← Quality](05-quality.md) · [Wiki home](README.md)

The running record is [`case-study-log.md`](../case-study-log.md).

## What each step of testing caught

- **Fast renders** found layout and text-size problems: hidden actions, and labels that broke mid-word at 200% text.
- **The emulator** found inset, keyboard and accessibility problems, like action bars hiding behind the keyboard and an unlabelled drag handle.
- **My real phone** found navigation-mode problems. It uses 3-button navigation, a 48 dp bar where the emulator had a thin gesture handle, so content sat under the system buttons.
- **Android 17** found a test-tooling problem, not an app problem. The Espresso version pulled in by Compose's test library called a method Android 17 removed, so the device test crashed before its first tap. Pinning Espresso 3.7.0 fixed it.

**Lesson:** no single level of testing is enough. Each step up costs more and finds things the cheaper ones can't.

## The update problem

Testing over a cable proves the app works. It doesn't make it safe to put real students in it. Every CI build had its own signing key, so the first update would have meant uninstalling, and uninstalling deletes everything.

The fix was a permanent personal key kept as a secret, with CI verifying the certificate before publishing ([decision 6](04-decisions.md#6-ship-updates-without-risking-real-data-adr-013)).

**Lesson:** "it runs" and "it's safe to depend on" are different milestones.

## How I worked with an AI coding agent

Kaiju Coach was built with an AI coding agent (Claude), and the commit history says so: 42 of its first 47 commits are authored by the agent. The way the work was split is the part I'd want to talk about:

- **I own the product.** The brief, the scope of each sprint, the brand and design direction, and the sign-off on every sprint. ADR-012 records my Sprint 01 sign-off: "Yes to all." ADR-015 records a change made at my request: copying imported videos into a private folder instead of referencing them.
- **Decisions are written down.** Fifteen ADRs, each with its cost, so I can explain and defend every choice.
- **I verify on real hardware.** I ran the full class flow on my own phone, over USB and over Wi-Fi, and those runs found problems the emulator never showed.
- **Trust stays with a person.** The agent can't write repository secrets or branch-protection rules. Adding the signing key and protecting `main` were done by me, from written steps. That's the right limit for an automated agent.

<!-- EARL: confirm this matches how you worked, and add anything you wrote or changed by hand. Interviewers will ask. -->

## What's next

- **Teach a real class with it** and write down what got in the way. This is the test that matters most, and it hasn't happened yet.
- Restore from backup, so an export can come back into the app.
- The desktop companion (React and TypeScript) and local Wi-Fi sync, already designed in [`06-sync-architecture.md`](../06-sync-architecture.md).
- Move UI strings into resources.
- Decide on a higher-contrast mode after real use in a bright gym.
