# Search & Calc

**Search & Calc** is a client-side Fabric mod for Minecraft (1.21 / 26.1.2) that completely overhauls how you interact with inventories. It seamlessly docks a powerful, multi-functional text bar to the bottom of any container screen (chests, shulker boxes, player inventory, etc.), acting as both an advanced item filter and an in-game calculator.

## What it does

### 🔍 Advanced Inventory Searching
By default, the bar acts as a search filter. Typing any text will instantly highlight items in the open inventory that match your query, while dimming everything else with a dark overlay. 

- **Smart Matching**: It searches through the item's display name, its underlying registry ID, and all custom lore/descriptions attached to it.
- **Logical AND (`&&`)**: You can chain multiple search terms together. For example, typing `diamond && sword` will only highlight items that contain both words.
- **Total Counts**: While searching, the bar displays a live, running total of how many matching items exist in the container.

### 🧮 Built-in Calculator Mode
Typing an equals sign (`=`) at the start of the bar instantly transforms it into a live math calculator.

- **Live Evaluation**: Type equations like `=5 * (12 + 4)` and the result will instantly preview as ghost text (`→ 80`) next to your cursor.
- **Shorthand Support**: It natively understands `k` (thousands) and `m` (millions). Typing `=2.5m / 5k` evaluates exactly as you'd expect.
- **Quick Submit**: Pressing `ENTER` will execute the calculation and replace the text in the box with the final result.

### ⚙️ HUD Editor & Customization
- **Drag-to-Move**: Hold `ALT`, click, and drag the search box to move it anywhere on your screen.
- **Drag-to-Resize**: Hold `ALT` + `CTRL`, click, and drag left or right to dynamically resize the width of the box to fit long equations.
- **Visual Clarity**: Matched items are highlighted with a sleek green border rather than just being left alone, making them pop out instantly.

### 🏝️ Hypixel Skyblock Compatibility
When playing on heavily customized servers like Hypixel Skyblock, items often use vanilla placeholders (like a diamond sword for an Aspect of the End).
- Use the command `/sac togglevanilla` in chat to toggle Vanilla ID matching on or off. 
- When disabled, the mod completely ignores the meaningless vanilla item ID and perfectly filters based *only* on the custom Skyblock display name and lore.
