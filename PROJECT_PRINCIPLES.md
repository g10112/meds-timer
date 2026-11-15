# Meds Timer — project principles

**This is a public-benefit software project.** The goal is to make basic reminder technology straightforward, private and accessible, without exploiting people who need it.

## Commitments for the official app
1. **Free to use:** no price, ads, subscriptions, in-app purchases or feature paywalls.
2. **Privacy:** no personal medical information collection, user profiling, behavioural advertising or sale of data.
3. **No forced account or internet connection.**
4. **Accessible by default:** readable text, meaningful controls, clear instructions and device support wherever feasible.
5. **Open and auditable:** public source, publicly documented changes, testable builds, release notes and issue reporting.
6. **No dark patterns:** no guilt prompts, misleading permissions, or tricks to make people pay.
7. **Safety before novelty:** test alarm delivery, restore behaviour and changes across Android versions.
8. **Respect user control:** allow the user to cancel reminders, configure sound, and understand exactly what is scheduled.

## How to keep it independent
- Keep source under **GPL-3.0-or-later** (strong reciprocal open-source licence).
- Publish official APKs and source from the upstream repository; later support independent build verification.
- Use community-maintained stores (for example F-Droid) in addition to conventional platforms, once eligible.
- Review contributions publicly; maintainers control what merges into the official app.
- Don't take advertising, tracking or exclusive distribution contracts for the upstream project.

### What this can and cannot protect
GPL requires that distributed modified versions provide corresponding source and remain under compatible GPL terms. **GPL does not ban selling software**; someone can legally charge for a copy if they follow the licence. Conversely, banning commercial use would generally mean the project is **not** open source and could exclude it from F-Droid. We can control our official releases, distribution accounts, and branding, and encourage trust through signed builds and clear provenance. Trademark protection, if desired, is a separate legal decision.

This document is the project's policy, not an additional restriction on the GPL licence.

## Health and reliability
Meds Timer is a reminder tool. It does **not** select medication schedules, verify doses or replace a clinician, carer or another reliable safety system. Phone settings and operating-system restrictions can affect delivery. Never promise an alert will always fire.
