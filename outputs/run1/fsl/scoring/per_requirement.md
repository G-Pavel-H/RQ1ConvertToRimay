# Per-requirement review — strategy `fsl`

## 72-Signal

**NL:** Request Number - 72 | Request Title - Message content preview in notification bar | Request Description - I'd like to get a preview of the messagge in notification bar. Could be also good a JB-style quick reply fuction. Thanks for your working and this awesome software!

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> the App must show a preview of the message in the notificaiton bar.

**Human conversions:**
- <MISSING_CONDITION> For all users, the system must provide a preview of the message in notification bar.
- For all users the App must show a "preview of the message" in the "notification bar"
- The App must display a "preview of the message" in the "notification bar".<MISSING_CONDITION>
- For all users, <MISSING_CONDITION> the app must show a preview of the message in the notificaiton bar.

**LLM Rimay:** For all users, the app must provide a preview of the message content in the notification bar.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.891 · jaccard=0.706

**Paska:** pass

---

## 36-Mastodon

**NL:** Request Number - 36 | Request Title - Feature request: Fediverse service label | Request Description - For example, Fedilab shows which service the profile is for, i.e. whether it is a Mastodon, Pixelfed, PeerTube, etc. account. Something similar for the official Mastodon app would be desirable, as it shows newcomers that theFediverse is more than just Mastodon.

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> Mastodon app must display the "service type" for each profile

**Human conversions:**
- <MISSING_CONDITION> For all users, the official Mastodon app must provide which service the profile is for.
- For all the users the Mastodon app must display the "service type" of each profile
- The App must show the service type of the profile.<MISSING_CONDITION>
- For all users, <MISSING_CONDITION> the app must show which service the profile is for.

**LLM Rimay:** For all users, the app must display a service label indicating which Fediverse service the profile is for.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | present | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.593 · jaccard=0.500

**Paska:** pass

---

## 152-Mastodon

**NL:** Request Number - 152 | Request Title - Display MD lists correctly | Request Description - I'm using the Mastodon app with my profile on mstdn.social which supports Markdown toots. Unfortunately, the Mastodon app isn't able to display MD lists correctly. Instead of putting each list item into a new line, it shrinks the complete list into a single line:  For comparison: Tusky is perfectly capable of at least inserting line breaks:

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> Mastodon app must dispay "MD" lists correctly

**Human conversions:**
- <MISSING_CONDITION> For all users, the Mastodon app must be able to display Markdown lists correctly.
- For all the users the Mastodon app must display "Markdown lists" correctly
- The App must display Markdown lists by rendering each list item on a separate line.<MISSING_CONDITION>
- For all users, <MISSING_CONDITION> the app must be able to display "MD" lists correctly.

**LLM Rimay:** For all users with profiles on mstdn.social, the Mastodon app must display Markdown lists with each list item on a new line.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | present | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.554 · jaccard=0.292

**Paska:** pass

---

## 5874-Signal

**NL:** Request Number - 5874 | Request Title - Allow saving multiple images in a conversation | Request Description - I have:   searched open and closed issues for duplicates  read https://github.com/WhisperSystems/Signal-Android/wiki/Submitting-useful-bug-reports  Feature Request. There was #5088 asking for multi-selecting images in "All images" and then saving them. It was closed without actually having multi-select & save implemented. I am asking for multi-select images in a conversation and then save them. Steps to reproduce.  go to a conversation long press a picture message tap a second/third/... picture message Actual result: No save button is shown. Expected result: A save button should be shown and allow to save the selected images.

**Gold canonical Rimay:** For all users, when the user long presses a "Picture message", the app must allow multi selection and saving of "picture messages"

**Human conversions:**
- When the user selects multiple pictures, the system must provide a save button to allow save selected images.
- When the user selects multiple "picture messages" in a conversation, then the App must show a "save button" that saves the selected "images"
- When the user selects multiple image messages in conversation, the App must display a "Save" button that saves the selected images.
- For all users, when the user long presses a picture message, the app must allow to multi select pictures and save them.

**LLM Rimay:** For all users, when the user selects multiple image messages in a conversation, then the app must display a save button to save the selected images.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | present | filled | ✓ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.532 · jaccard=0.312

**Paska:** pass

---

## 751-Signal

**NL:** Request Number - 751 | Request Title - Adapt theme to light conditions | Request Description - Under low light conditions the light design is too bright and puts a strain on the eyes. Thus, a nice thing to have would be an option to switch between light and dark theme automagically using the light sensor. Theme light theme ( ) dark theme ( ) depending on light condition (o)

**Gold canonical Rimay:** For all users, if the light sensor senses change, then the App must switch to "dark theme"

**Human conversions:**
- <MISSING_CONDITION> For all users the app must provide an option to switch between light and dark themes.
- For all the users if the ambient "light conditions" are low, then the App must switch to the "dark theme" using the "light sensor"
- The App must automatically switch between the "light" and "dark" themes, when the light condition change is being observed by the light sensor.
- For all users, when the light sensor senses change, the app must automatically switch between light and dark modes.

**LLM Rimay:** For all users, when light conditions are low, then the app must switch automatically between light and dark theme using the light sensor.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | present | filled | ✓ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.608 · jaccard=0.500

**Paska:** pass

---

## 3192-Signal

**NL:** Request Number - 3192 | Request Title - Image view in fullscreen | Request Description - Remove the action bar in the view where you can inspect a received picture to enlarge the image.

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> the App must not display the action bar where users can inspect a received picture to enlarge the image.

**Human conversions:**
- <MISSING_CONDITION> For all users, the App must remove the action bar in the view where you can inspect a received picture to enlarge the image.
- For all the users the App must remove the "action bar" in the "image-inspection view" where a received picture is enlarged
- When the user inspects a received picture, the App must remove the "action bar" to enlarge the image.
- For all users, <MISSING_CONDITION>, the app should not display the action in the view where user inspects a received picture to enlarge the image.

**LLM Rimay:** For all users, when viewing a received picture in fullscreen, the app must remove the action bar.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.509 · jaccard=0.458

**Paska:** FAIL
- Incomplete condition: Actor is missing in the condition.

---

## 778-Signal

**NL:** Request Number - 778 | Request Title - Feature Request: Only one notification for multiple messages received at once + Timestamps | Request Description - After being offline (flight mode / no internet connection) my device rings/vibrates multiple times, depending on how many messages have been sent by others in the meantime. Being in a quite active group my device vibrated for about 30 seconds this morning. I can see the point of being notified that not only one but multiple messages have been received but this should be reworked to a less annoying way (e.g., ring twice for multiple messages). Additionally, all messages that get then received are displayed with their receiving time. I would prefer having them displayed with the sending time, which can also be seen by looking in the message details. Maybe a menu entry to switch between both options would be a good idea because I can imagine people who like it the way it is handled now.

**Gold canonical Rimay:** For all users, when the user receives multiple messages in a small timeframe, the App must only ring for a limited time

**Human conversions:**
- For all the users when multiple "messages" are received after the device has been offline, then the App must notify the user a limited number of times instead of once per message
- When the App reconnects after being offline, the App must notify the user of multiple received messages using fewer notification sounds or vibrations.
When the App receives messages that were sent while the device was offline, the App must display the messages using their sending time.
- For all users, when the user receives multiple messages in a small timeframe, the app must only ring for a limited time instead of ringing for every message.
- After the device has been offline and multiple messages have been received, the App must notify the user in less annoying way.

**LLM Rimay:** For all users, when the device receives multiple messages at once after being offline, then the app must send a single notification instead of multiple notifications. <NON_ATOMIC>

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | present | filled | ✓ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.589 · jaccard=0.344

**Paska:** pass

---

## 1699-Signal

**NL:** Request Number - 1699 | Request Title - Add Android Wear support | Request Description - Hangouts has the possibility to answer with predefined answers or voice. Something similar would be awesome for TextSecure. Since all this works over notifications, this should be reasonably easy to intergrate.

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> TextSecure must provide possibility to answer messages with predefined answers or voice messages.

**Human conversions:**
- <MISSING_CONDITION> For all users, TextSecure must answer with predefined answers or voice.
- For all the users when a "message notification" is shown, then the App must allow the user to reply with "predefined answers"
- When the user receives a notification, the App must allow the user to reply with predefined answer.
When the user receives a notification, the App must allow the user to reply with voice.
- For all users, <MISSING_CONDITION> TextSecure must provide possibility to answer messages with predefined answers or voice messages.

**LLM Rimay:** For all users, the app must provide predefined answers and voice response options on Android Wear notifications. <NON_ATOMIC>

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | present | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.562 · jaccard=0.333

**Paska:** pass

---

## 2159-Signal

**NL:** Request Number - 2159 | Request Title - Share multiple photos (files) | Request Description - Hi, I love TextSecure but there's one thing that I find rather annoying. It often happens that I want to share some pics with someone (more than just one pic), and I have to select them one at a time instead of selecting them all and sending them all at once. It would be cool if I could pick them from the gallery and then click "share" and send them without going through the procedure n times. I don't think this should be hard to implement and it would save so much time and improve usability greatly. Thank you

**Gold canonical Rimay:** For all users, <MISSING_CONDITION> TextSecure must allow users to select multiple "pictures" from the "gallery" and send them all at once

**Human conversions:**
- When the user selects more than one picture from gallery to share, the App must provide functionality to share them all at once.
- For all the users the App must allow the user to select multiple "pictures" from the "gallery" and send them all at once
- When the user shares pictures from the "gallery", the App must allow the user to select multiple pictures and send them in a single operation.
- For all users, <MISSING_CONDITION> TextSecure must allow users to send multiple images from the gallery at once.

**LLM Rimay:** For all users, the app must provide the ability to select and send multiple photos at once from the gallery.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | missing | filled | ✗ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.569 · jaccard=0.636

**Paska:** pass

---

## 4049-Signal

**NL:** Request Number - 4049 | Request Title - delivery indicator in conversation list item | Request Description - the conversation summary should show the most recent message's delivery status if it's outgoing

**Gold canonical Rimay:** For all users, if the most recent "message" is outgoing, then the App must show its "delivery status" in the "conversation summary"

**Human conversions:**
- If the most recent message is outgoing, the App must show the delivery status in the conversation summary.
- If the most recent "message" is outgoing, then the App must show its "delivery status" in the "conversation summary"
- When the most recent message in a conversation is outgoing, the App must display its delivery status in the conversation summary.
- For all users, <MISSING_CONDITION> the app must show the most recent message's delivery status.

**LLM Rimay:** For all conversations, the app must display the delivery status of the most recent outgoing message in the conversation list item.

**Slot signals (gold ternary vs LLM binary):**

| Slot | Gold | LLM | Match |
|------|------|-----|-------|
| scope | implied | filled | ✓ |
| condition | present | filled | ✓ |
| actor | implied | filled | ✓ |
| modalVerb | implied | filled | ✓ |
| action | present | filled | ✓ |

**Overall verdict:** gold=complete · LLM=complete · match=✓

**Similarity to gold:** seq_ratio=0.521 · jaccard=0.520

**Paska:** pass

---
