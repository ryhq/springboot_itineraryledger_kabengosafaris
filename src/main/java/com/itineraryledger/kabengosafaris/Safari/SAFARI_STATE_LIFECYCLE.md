# Safari State Lifecycle Guide

This document explains the Safari booking lifecycle with real-world examples of how safaris transition through different states.

## State Categories

| Category | States | Description |
|----------|--------|-------------|
| **Booking** | DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED | Initial booking process |
| **Payment** | PENDING_DEPOSIT, DEPOSIT_PAID, FULLY_PAID | Payment tracking |
| **Operational** | READY, IN_PROGRESS | Safari execution |
| **Completion** | COMPLETED, PENDING_REVIEW, CLOSED | Post-safari wrap-up |
| **Hold/Pause** | ON_HOLD, PENDING_DOCUMENTS, PENDING_AVAILABILITY | Temporary pauses |
| **Reschedule** | POSTPONED, RESCHEDULING | Date changes |
| **Cancellation** | CANCELLATION_REQUESTED, CANCELLED, CANCELLED_BY_CLIENT, CANCELLED_BY_OPERATOR, CANCELLED_FORCE_MAJEURE | Cancellation types |
| **Refund** | REFUND_PENDING, REFUND_PARTIAL, REFUND_COMPLETE | Refund processing |
| **Dispute** | DISPUTED, UNDER_INVESTIGATION | Complaint handling |

---

## Example 1: Happy Path - Successful Safari

**Scenario:** The Johnson family books a 7-day Serengeti safari. Everything goes smoothly.

```
Timeline:
Jan 5:  Safari created from "7-Day Serengeti Adventure" itinerary
        → DRAFT

Jan 5:  Agent completes all booking details, submits for approval
        → PENDING_APPROVAL

Jan 6:  Operations manager reviews and approves
        → APPROVED

Jan 7:  Client confirms dates, accommodations verified with lodges
        → CONFIRMED

Jan 8:  Deposit invoice sent to client
        → PENDING_DEPOSIT

Jan 12: Client pays 30% deposit ($2,400)
        → DEPOSIT_PAID

Feb 15: Client pays remaining balance ($5,600)
        → FULLY_PAID

Mar 1:  All preparations complete (guides assigned, permits obtained)
        → READY

Mar 5:  Safari begins! Clients picked up from Arusha
        → IN_PROGRESS

Mar 11: Safari ends, clients dropped at Kilimanjaro Airport
        → COMPLETED

Mar 15: Post-trip review collected, feedback positive
        → PENDING_REVIEW

Mar 20: All accounting reconciled, supplier payments done
        → CLOSED
```

**State Flow:**
```
DRAFT → PENDING_APPROVAL → APPROVED → CONFIRMED → PENDING_DEPOSIT
→ DEPOSIT_PAID → FULLY_PAID → READY → IN_PROGRESS → COMPLETED
→ PENDING_REVIEW → CLOSED
```

---

## Example 2: Rescheduling Due to Client Request

**Scenario:** The Martinez family needs to change their safari dates due to a family emergency.

```
Timeline:
Feb 1:  Safari confirmed for April 10-16
        State: CONFIRMED

Feb 5:  Client paid deposit
        → DEPOSIT_PAID

Mar 15: Client calls - family emergency, needs to postpone
        → RESCHEDULING
        Reason: "Family medical emergency - client requests date change"

Mar 18: New dates confirmed: June 5-11 (availability verified)
        → CONFIRMED
        Dates updated: Start June 5, End June 11

Mar 20: Full payment received
        → FULLY_PAID

(Safari proceeds normally in June)
```

**State Flow:**
```
CONFIRMED → DEPOSIT_PAID → RESCHEDULING → CONFIRMED → FULLY_PAID → ...
```

**API Call for Reschedule:**
```json
POST /api/safaris/{id}/state/reschedule/initiate
{
  "reason": "Family medical emergency - client requests date change"
}

POST /api/safaris/{id}/state/reschedule/complete
{
  "newStartDate": "2025-06-05",
  "reason": "Rescheduled per client request, all suppliers confirmed"
}
```

---

## Example 3: Cancellation by Client with Partial Refund

**Scenario:** Mr. Thompson cancels his safari 2 weeks before departure due to work commitments.

```
Timeline:
Jan 10: Safari booked for Feb 15-21
        State: CONFIRMED

Jan 15: Full payment received ($8,000)
        → FULLY_PAID

Feb 1:  Client calls to cancel - urgent work project
        → CANCELLATION_REQUESTED
        Reason: "Client unable to travel due to work commitment"

Feb 2:  Operations confirms cancellation, calculates refund
        → CANCELLED_BY_CLIENT
        Reason: "Cancellation confirmed - 14 days notice"

Feb 3:  Refund processing begins (per cancellation policy: 50% refund)
        → REFUND_PENDING

Feb 10: Partial refund of $4,000 issued to client
        → REFUND_PARTIAL
        Reason: "50% refund per cancellation policy (14 days notice)"

        Note: No further refund expected - case closed at REFUND_PARTIAL
```

**State Flow:**
```
FULLY_PAID → CANCELLATION_REQUESTED → CANCELLED_BY_CLIENT
→ REFUND_PENDING → REFUND_PARTIAL
```

**Cancellation Policy Example:**
- 30+ days notice: 90% refund
- 14-29 days notice: 50% refund
- 7-13 days notice: 25% refund
- Less than 7 days: No refund

---

## Example 4: Force Majeure Cancellation

**Scenario:** A volcanic eruption closes airspace, making safari impossible.

```
Timeline:
Mar 1:  Safari confirmed for Mar 10-16
        State: FULLY_PAID

Mar 8:  Volcanic activity forces airspace closure
        → CANCELLED_FORCE_MAJEURE
        Reason: "Volcanic eruption - airspace closed, travel impossible"

Mar 9:  Full refund initiated
        → REFUND_PENDING

Mar 15: Full refund of $12,000 issued
        → REFUND_COMPLETE
        Reason: "100% refund - force majeure event"
```

**State Flow:**
```
FULLY_PAID → CANCELLED_FORCE_MAJEURE → REFUND_PENDING → REFUND_COMPLETE
```

---

## Example 5: Pending Documents Hold

**Scenario:** Safari booking held while waiting for visa approval.

```
Timeline:
Jan 5:  Safari booked for Mar 1-7
        State: CONFIRMED

Jan 10: Deposit received
        → DEPOSIT_PAID

Jan 15: Client's visa application still pending
        → PENDING_DOCUMENTS
        Reason: "Awaiting Tanzania tourist visa approval"

Feb 5:  Visa approved!
        → DEPOSIT_PAID (returned to previous payment state)
        Reason: "Visa approved - document #TZ-2025-12345"

Feb 10: Full payment received
        → FULLY_PAID

(Safari proceeds normally)
```

**State Flow:**
```
CONFIRMED → DEPOSIT_PAID → PENDING_DOCUMENTS → DEPOSIT_PAID → FULLY_PAID → ...
```

---

## Example 6: Availability Hold

**Scenario:** Preferred lodge fully booked, waiting for alternative confirmation.

```
Timeline:
Jan 5:  Safari booking in progress
        State: APPROVED

Jan 6:  Serengeti Serena Lodge fully booked for requested dates
        → PENDING_AVAILABILITY
        Reason: "Serengeti Serena Lodge unavailable Mar 3-5, checking alternatives"

Jan 8:  Four Seasons Serengeti confirms availability
        → CONFIRMED
        Reason: "Four Seasons Serengeti confirmed - upgraded accommodation"

(Booking proceeds normally)
```

**State Flow:**
```
APPROVED → PENDING_AVAILABILITY → CONFIRMED → ...
```

---

## Example 7: General Hold for Issues

**Scenario:** Credit card payment flagged for fraud review.

```
Timeline:
Jan 10: Safari confirmed
        State: CONFIRMED

Jan 12: Deposit payment attempted
        → PENDING_DEPOSIT

Jan 13: Payment flagged by bank's fraud department
        → ON_HOLD
        Reason: "Credit card payment under fraud review by issuing bank"

Jan 15: Bank clears payment, deposit confirmed
        → DEPOSIT_PAID
        Reason: "Payment cleared by bank, fraud alert resolved"

(Booking proceeds normally)
```

**State Flow:**
```
CONFIRMED → PENDING_DEPOSIT → ON_HOLD → DEPOSIT_PAID → ...
```

---

## Example 8: Client Dispute After Safari

**Scenario:** Client unhappy with accommodation quality, files complaint.

```
Timeline:
Apr 1-7: Safari completed
         State: COMPLETED

Apr 10:  Client emails complaint about lodge conditions
         → DISPUTED
         Reason: "Client complaint: Room at Ngorongoro lodge had no hot water for 2 nights"

Apr 12:  Operations begins investigation
         → UNDER_INVESTIGATION
         Reason: "Investigating complaint with lodge management"

Apr 20:  Lodge confirms issue, offers compensation
         → CLOSED
         Reason: "Resolved: Lodge provided $200 credit, client satisfied"

         Note: targetState set to CLOSED with resolution details
```

**State Flow:**
```
COMPLETED → DISPUTED → UNDER_INVESTIGATION → CLOSED
```

**API Call for Resolution:**
```json
POST /api/safaris/{id}/state/dispute/resolve
{
  "reason": "Resolved: Lodge provided $200 credit for inconvenience, client satisfied",
  "targetState": "CLOSED"
}
```

---

## Example 9: Operator-Initiated Cancellation

**Scenario:** Tour operator cannot fulfill safari due to guide unavailability.

```
Timeline:
Feb 1:  Safari confirmed for Feb 20-26
        State: FULLY_PAID

Feb 15: All available guides booked, cannot staff this safari
        → CANCELLED_BY_OPERATOR
        Reason: "Unable to provide qualified guide for requested dates - staff shortage"

Feb 16: Full refund initiated
        → REFUND_PENDING

Feb 18: Full refund of $9,500 issued + $500 goodwill credit
        → REFUND_COMPLETE
        Reason: "Full refund plus $500 credit for future booking"
```

**State Flow:**
```
FULLY_PAID → CANCELLED_BY_OPERATOR → REFUND_PENDING → REFUND_COMPLETE
```

---

## Example 10: Booking Rejection

**Scenario:** Management rejects booking due to insufficient lead time.

```
Timeline:
Jan 15: Agent creates safari for Jan 25-31 (only 10 days away)
        State: DRAFT

Jan 15: Agent submits for approval
        → PENDING_APPROVAL

Jan 16: Manager reviews - insufficient time to arrange permits
        → DRAFT
        Reason: "Rejected: Insufficient lead time for park permits (minimum 14 days required)"

Jan 16: Agent discusses with client, new dates agreed
        (Agent updates dates to Feb 10-16)

Jan 17: Resubmitted for approval
        → PENDING_APPROVAL

Jan 17: Manager approves
        → APPROVED
```

**State Flow:**
```
DRAFT → PENDING_APPROVAL → DRAFT → PENDING_APPROVAL → APPROVED → ...
```

---

## State Diagram

```
                                    ┌─────────────────────────────────────────────────┐
                                    │                  BOOKING FLOW                    │
                                    └─────────────────────────────────────────────────┘

    ┌───────┐      ┌──────────────────┐      ┌──────────┐      ┌───────────┐
    │ DRAFT │─────▶│ PENDING_APPROVAL │─────▶│ APPROVED │─────▶│ CONFIRMED │
    └───────┘      └──────────────────┘      └──────────┘      └───────────┘
        ▲                   │                      │                  │
        │                   │                      │                  │
        └───────────────────┘                      │                  │
           (rejected)                              │                  ▼
                                                   │         ┌─────────────────┐
                                    ┌──────────────┘         │ PENDING_DEPOSIT │
                                    │                        └─────────────────┘
                                    ▼                                  │
                          ┌────────────────────┐                       ▼
                          │ PENDING_AVAILABILITY│              ┌──────────────┐
                          └────────────────────┘              │ DEPOSIT_PAID │
                                                              └──────────────┘
                                                                       │
                                    ┌─────────────────────────────────────────────────┐
                                    │                  PAYMENT FLOW                    │
                                    └─────────────────────────────────────────────────┘
                                                                       │
                                                                       ▼
                                                              ┌────────────┐
                                                              │ FULLY_PAID │
                                                              └────────────┘
                                                                       │
                                    ┌─────────────────────────────────────────────────┐
                                    │               OPERATIONAL FLOW                   │
                                    └─────────────────────────────────────────────────┘
                                                                       │
                                                                       ▼
                                                                 ┌───────┐
                                                                 │ READY │
                                                                 └───────┘
                                                                       │
                                                                       ▼
                                                              ┌─────────────┐
                                                              │ IN_PROGRESS │
                                                              └─────────────┘
                                                                       │
                                    ┌─────────────────────────────────────────────────┐
                                    │               COMPLETION FLOW                    │
                                    └─────────────────────────────────────────────────┘
                                                                       │
                                                                       ▼
                                                               ┌───────────┐
                                                               │ COMPLETED │
                                                               └───────────┘
                                                                       │
                                                    ┌──────────────────┼──────────────────┐
                                                    ▼                  ▼                  ▼
                                           ┌────────────────┐   ┌──────────┐        ┌────────┐
                                           │ PENDING_REVIEW │──▶│  CLOSED  │◀───────│DISPUTED│
                                           └────────────────┘   └──────────┘        └────────┘
                                                                                          │
                                                                                          ▼
                                                                              ┌─────────────────────┐
                                                                              │ UNDER_INVESTIGATION │
                                                                              └─────────────────────┘


    ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
    │                                    CANCELLATION FLOW                                             │
    │   (Can be triggered from: DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, PENDING_DEPOSIT,        │
    │    DEPOSIT_PAID, FULLY_PAID, READY, ON_HOLD, POSTPONED, PENDING_DOCUMENTS, PENDING_AVAILABILITY)│
    └─────────────────────────────────────────────────────────────────────────────────────────────────┘

                            ┌────────────────────────┐
                            │ CANCELLATION_REQUESTED │
                            └────────────────────────┘
                                         │
            ┌────────────────────────────┼────────────────────────────┐
            ▼                            ▼                            ▼
    ┌───────────┐            ┌─────────────────────┐      ┌────────────────────────┐
    │ CANCELLED │            │ CANCELLED_BY_CLIENT │      │ CANCELLED_BY_OPERATOR  │
    └───────────┘            └─────────────────────┘      └────────────────────────┘
            │                            │                            │
            └────────────────────────────┼────────────────────────────┘
                                         │
                                         ▼
                              ┌────────────────────────┐
                              │ CANCELLED_FORCE_MAJEURE│
                              └────────────────────────┘
                                         │
                                         ▼
                                ┌────────────────┐
                                │ REFUND_PENDING │
                                └────────────────┘
                                         │
                            ┌────────────┴────────────┐
                            ▼                         ▼
                    ┌────────────────┐       ┌─────────────────┐
                    │ REFUND_PARTIAL │──────▶│ REFUND_COMPLETE │
                    └────────────────┘       └─────────────────┘
```

---

## API Endpoints Reference

| Transition | Endpoint | Required Fields |
|------------|----------|-----------------|
| Submit for Approval | `POST /state/submit-for-approval` | - |
| Approve | `POST /state/approve` | - |
| Reject | `POST /state/reject` | `reason` (required) |
| Confirm | `POST /state/confirm` | - |
| Request Deposit | `POST /state/request-deposit` | - |
| Record Deposit | `POST /state/record-deposit` | - |
| Record Full Payment | `POST /state/record-full-payment` | - |
| Mark Ready | `POST /state/mark-ready` | - |
| Start Safari | `POST /state/start` | - |
| Complete Safari | `POST /state/complete` | - |
| Request Review | `POST /state/request-review` | - |
| Close Safari | `POST /state/close` | - |
| Hold | `POST /state/hold` | `reason` (required) |
| Release Hold | `POST /state/release-hold` | `targetState` (optional) |
| Pending Documents | `POST /state/pending-documents` | `reason` (required) |
| Documents Received | `POST /state/documents-received` | `targetState` (optional) |
| Pending Availability | `POST /state/pending-availability` | `reason` (required) |
| Availability Confirmed | `POST /state/availability-confirmed` | `targetState` (optional) |
| Postpone | `POST /state/postpone` | `reason` (required) |
| Initiate Reschedule | `POST /state/reschedule/initiate` | `reason` (required) |
| Complete Reschedule | `POST /state/reschedule/complete` | `newStartDate` (required) |
| Request Cancellation | `POST /state/request-cancellation` | `reason` (required) |
| Cancel | `POST /state/cancel` | `reason` (required) |
| Cancel by Client | `POST /state/cancel-by-client` | `reason` (required) |
| Cancel by Operator | `POST /state/cancel-by-operator` | `reason` (required) |
| Cancel Force Majeure | `POST /state/cancel-force-majeure` | `reason` (required) |
| Initiate Refund | `POST /state/initiate-refund` | - |
| Record Partial Refund | `POST /state/record-partial-refund` | - |
| Record Full Refund | `POST /state/record-full-refund` | - |
| Mark Disputed | `POST /state/disputed` | `reason` (required) |
| Investigate Dispute | `POST /state/investigate` | - |
| Resolve Dispute | `POST /state/resolve-dispute` | `reason`, `targetState` |

---

## Terminal States

These states indicate the safari lifecycle has ended:

- **CLOSED** - Safari completed successfully with all tasks finished
- **CANCELLED** - Generic cancellation
- **CANCELLED_BY_CLIENT** - Client-initiated cancellation
- **CANCELLED_BY_OPERATOR** - Operator-initiated cancellation
- **CANCELLED_FORCE_MAJEURE** - Cancellation due to unforeseen circumstances
- **REFUND_COMPLETE** - Full refund issued (terminal for refund flow)

---

## State Behavior Flags

| State | Editable | Cancellable | Terminal | Active | Requires Payment |
|-------|----------|-------------|----------|--------|------------------|
| DRAFT | Yes | Yes | No | No | No |
| PENDING_APPROVAL | Yes | Yes | No | No | No |
| APPROVED | Yes | Yes | No | No | No |
| CONFIRMED | Yes | Yes | No | Yes | No |
| PENDING_DEPOSIT | Yes | Yes | No | Yes | Yes |
| DEPOSIT_PAID | Yes | Yes | No | Yes | Yes |
| FULLY_PAID | No | Yes | No | Yes | No |
| READY | No | Yes | No | Yes | No |
| IN_PROGRESS | No | No | No | Yes | No |
| COMPLETED | No | No | No | No | No |
| CLOSED | No | No | Yes | No | No |
| CANCELLED_* | No | No | Yes | No | No |
| REFUND_COMPLETE | No | No | Yes | No | No |
