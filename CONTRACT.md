# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes or no.

Yes.

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

Java picks an overload at compile time partly by argument count. Both
`FrontDesk` calls (lines 27 and 33) pass 4 arguments, so they still bind to
the 4-arg `createBooking` and can't match the 5-arg one, even the `null` call.
The consumer only calls `BookingApi` and doesn't implement it, so the new
abstract method only has to be implemented in `InMemoryBookingService`, which
we own.

### What happened

**The result.** What the build printed for each module.

```
lab06-api:      Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
lab06-consumer: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**If your prediction was wrong,** say what you missed.

It was right. The consumer didn't notice: its 4-arg calls compiled against the
same method, and their behavior didn't change because the 4-arg version just
delegates with `notes = null`.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

No. Adding an abstract method to `BookingApi` breaks any outside class that
implements it, since that class no longer compiles until it adds the method.
A `default` method would avoid that. An overload can also cause ambiguity:
adding `createBooking(String, long, long, Integer)` would make
`createBooking(room, s, e, null)` in `FrontDesk` fail to compile.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No. `lab06-consumer` goes red at compile time (`compile`, before any of its
tests run), because the positional `createBooking` it calls no longer exists.

**Where.** Name the call sites you expect to be affected, if any.

`FrontDesk.java` line 27 (`bookWalkIn`) and line 33 (`joinWaitlist`), both
calling `api.createBooking(roomId, start, end, key)`. `listBookings` and
`cancelBooking` are unchanged, so those calls are fine.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

They should pass 5/5, since the behavior is the same and only the call shape
changed. That says nothing about the consumer: we rewrote our own tests to
the new signature, but we can't rewrite theirs.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

```
lab06-api:      Tests run: 5, Failures: 0, Errors: 0, Skipped: 0   -> SUCCESS
lab06-consumer: COMPILATION ERROR                                  -> FAILURE
FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
  required: edu.cmu.cs214.booking.BookingRequest
  found:    java.lang.String,long,long,<nulltype>
FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
  required: edu.cmu.cs214.booking.BookingRequest
  found:    java.lang.String,long,long,java.lang.String
BUILD FAILURE
```

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

Only the api's 5 tests ran, and they were green. The consumer failed at
`compile`, so none of its 7 tests ran. Our own suite can't detect a contract
break, because we update it along with the change. Only the caller's code,
which we don't control, catches it.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

`@Deprecated createBooking(String, long, long, String)` and
`@Deprecated createBooking(String, long, long, String, String)`, both as
`default` methods on `BookingApi`. Each one wraps its arguments in a
`new BookingRequest(...)` (notes `null` for the 4-arg version) and calls
`createBooking(BookingRequest)`.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

```
[WARNING] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
```

There's a matching warning for line 33. The build went from FAILURE to
SUCCESS: api 5/5, consumer 7/7.

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The front desk team can build and pass again without changing anything. We
moved to the new API immediately, and they can migrate whenever they choose,
until we actually remove the old overloads.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

The warning appears in the consumer team's own build output, at their exact
file and line, every time they compile. They don't have to go looking for it
the way they'd have to find a note in our README. The `@deprecated` javadoc
also shows up in their IDE and names the replacement.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The boolean flag on `cancelBooking(long bookingId, boolean notifyWaitlist)`.
`true` and `false` don't say what they mean, and both are valid, so passing
the wrong one still compiles.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

`FrontDesk.java:48`: `api.cancelBooking(bookingId, true);`
`FrontDesk.java:53`: `api.cancelBooking(bookingId, false);`

Without the javadoc or the method names around them, you can't tell which
call promotes a waitlisted guest.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

Silent wrong behavior. Flip the flag in `cancelQuietly` and a desk typo
correction gives the room to a waitlisted guest. Flip it the other way and
the waitlisted guest never gets the room. Neither case throws an error.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

```java
public enum WaitlistAction { PROMOTE_NEXT, LEAVE_WAITLISTED }

boolean cancelBooking(long bookingId, WaitlistAction action);

// FrontDesk
api.cancelBooking(bookingId, WaitlistAction.PROMOTE_NEXT);
api.cancelBooking(bookingId, WaitlistAction.LEAVE_WAITLISTED);
```

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The compiler's type check. A bare `true` or `false` no longer compiles, so
every call has to name what it does, and a wrong choice is visible when
reading the line. Adding a third action later (for example "notify all") is
also caught by an exhaustive `switch` in the implementation. This doesn't
stop someone from picking the wrong constant on purpose, but it makes the
mistake visible.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

It's another breaking change, so it needs a second deprecation cycle:
`cancelBooking(long, boolean)` stays as a `@Deprecated` method that maps to
the enum, and the consumer gets more warnings. Callers that compute the flag
(say `cancelBooking(id, guestAsked)`) now need a ternary to convert it to the
enum.

**When the price is worth paying.** A condition under which it is.

When the mistake has real consequences that nobody would notice. Here, giving
away a room nobody released is a real-world harm that no test would catch.
It's also cheaper to do while there's only one consumer, before more callers
are relying on the boolean.
