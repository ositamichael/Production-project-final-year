# Sprint 18 — Mobile shell and private illustrations

## Purpose

This increment corrects the tablet and phone navigation behaviour and gives the no-cost Image Studio a useful visual output without falsely presenting a template engine as a large generative model.

## Mobile navigation

- Phones keep the five primary destinations in a fixed bottom navigation bar.
- **More** opens a rounded, scrollable app sheet above the bottom navigation.
- The sheet exposes every UniCircle workspace in a two-column menu.
- A close button, outside backdrop and Escape-key control provide predictable dismissal.
- Background scrolling is locked while the menu is open.
- Tablets receive a scrollable side drawer rather than an oversized fixed desktop menu.

## No-cost Image Studio

Image Studio now creates an original SVG illustration from the user's description. The colour palette and scene vary using the prompt, with dedicated compositions for university life, study and technology themes. The result appears in the conversation and can be downloaded as an editable SVG.

The generator runs locally in the application. It does not send the prompt to an image provider, incur model charges or claim to create photorealistic images. When a configured advanced provider is available, the existing gateway can still return model-generated PNG images.

## Verification

- Server tests verify a complete SVG data URL and the private-generation notice.
- Front-end structure tests verify the drawer controls and downloadable SVG handling.
- Manual browser checks cover desktop and a 390-pixel phone viewport.
