# Sprint 19 — Focused mobile workspaces

## Purpose

This increment makes the phone experience behave like a focused application instead of a desktop dashboard squeezed into a narrow screen. It also changes Ask UniCircle from one temporary answer panel into a conversation workspace with stored chat history.

## Mobile experience

- My Campus remains the main dashboard and keeps its greeting and global search.
- Feature pages use a compact application header so their own content begins near the top of the screen.
- Messages uses the full available phone height. Opening a person hides the thread list and presents a dedicated conversation with a fixed header, scrolling messages and bottom composer.
- Page headings, toolbars, cards and safety content use consistent spacing at phone widths.
- The large staging warning was removed from the main shell. Honest staging and fictional-data information remains in the footer, sign-in journey and Pilot Test Centre.

## Ask UniCircle history

- A user can create a separate chat with **New chat**.
- The chat list shows the title, latest answer preview and update date.
- Questions, answers, source links, notices and generated-image data are stored in PostgreSQL or the local H2 database.
- Signed-in history is private to the account. Guest history is private to the browser session and is not treated as an authenticated personal record.
- Opening a saved chat restores its workspace mode and complete conversation.

## Verification

- Controller tests cover conversation creation, persisted turns, listing and owner isolation.
- Front-end structure tests cover the history list, New chat control and conversation API wiring.
- Mobile browser checks cover Messages, Ask UniCircle and representative campus feature pages.
