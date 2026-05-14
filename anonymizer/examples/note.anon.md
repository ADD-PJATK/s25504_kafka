# Project Kick-off Meeting Notes
**Date:** 2024-03-15  
**Facilitator:** Piotr Wiśniewski  
**Note-taker:** Katarzyna Kowalska

---

## Attendees

| Name | Role | Contact |
|------|------|---------|
| PERSON_A | Project Lead | PERSON_A <EMAIL_A> · PHONE_A |
| Piotr Wiśniewski | Developer | piotr.wisniewski@firma.test |
| Katarzyna Kowalska | QA Engineer | katarzyna.kowalska@firma.test |
| Marek Lewandowski | DevOps | marek.lewandowski@firma.test |
| Agnieszka Wójcik | HR | agnieszka.wojcik@firma.test |
| Tomasz Kamiński | Analyst | tomasz.kaminski@firma.test |

---

## Agenda

1. Project overview and goals
2. Role assignments
3. Timeline and milestones
4. Risk register review
5. AOB

---

## 1. Project Overview

PERSON_A opened the meeting by presenting the high-level goals of the data
pipeline modernisation initiative. The primary contact for external stakeholders
is PERSON_A; all queries should be directed to EMAIL_A or by phone at
PHONE_A.

Piotr Wiśniewski outlined the technical architecture: a Kafka-based streaming
layer feeding a PostgreSQL warehouse, with nightly batch exports consumed by the
reporting tier.

---

## 2. Role Assignments

- **Project Lead:** PERSON_A — accountable for delivery, escalation point.
- **Lead Developer:** Piotr Wiśniewski — owns backend services.
- **QA Lead:** Katarzyna Kowalska — owns test strategy and automation.
- **DevOps:** Marek Lewandowski — owns CI/CD pipelines and infrastructure.
- **Business Analyst:** Tomasz Kamiński — owns requirements backlog.
- **HR Liaison:** Agnieszka Wójcik — handles onboarding and access requests.

---

## 3. Timeline and Milestones

| Milestone | Owner | Target Date |
|-----------|-------|-------------|
| Architecture sign-off | PERSON_A | 2024-03-22 |
| Dev environment ready | Marek Lewandowski | 2024-03-29 |
| First integration test | Piotr Wiśniewski | 2024-04-12 |
| UAT start | Katarzyna Kowalska | 2024-04-26 |
| Go-live | PERSON_A | 2024-05-10 |

PERSON_A confirmed the go-live date is fixed due to a contractual obligation.

---

## 4. Risk Register

| # | Risk | Owner | Mitigation |
|---|------|-------|------------|
| R1 | Kafka version incompatibility | Piotr Wiśniewski | Pin version; test upgrade path |
| R2 | Data volume underestimate | Tomasz Kamiński | Stress test with 2× projected load |
| R3 | Access provisioning delays | Agnieszka Wójcik | Submit requests by 2024-03-18 |
| R4 | Key person dependency on PERSON_A | PERSON_A | Document decisions; share access |

---

## 5. Action Items

- [ ] **PERSON_A** — send signed architecture document to piotr.wisniewski@firma.test by 2024-03-20.
- [ ] **Marek Lewandowski** — provision dev cluster and share credentials with PERSON_A.
- [ ] **Agnieszka Wójcik** — submit access requests for all attendees listed above.
- [ ] **Katarzyna Kowalska** — draft test strategy v1 and share with EMAIL_A.
- [ ] **Tomasz Kamiński** — baseline backlog ready for sprint-planning on 2024-03-25.

---

## 6. AOB

Marek Lewandowski raised a question about on-call rotation. PERSON_A
(EMAIL_A, PHONE_A) will coordinate the initial rota with HR.

Next meeting: **2024-03-22 10:00**, chaired by PERSON_A.
