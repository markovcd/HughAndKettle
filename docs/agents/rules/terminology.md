# Terminology

## One word per thing

`docs/glossary.md` gives each thing in the domain one word, the name the code uses for it where that differs, and the words it must not be called. Use the glossary's word in everything written to the repo: code, UI text, docs, test names, comments and commit messages. When a new concept arrives, add its row in the same commit that introduces it.

## Correct the user's word on the spot

When the user writes a word the glossary lists under **Not** for the thing they mean, say so at the top of the reply, in one line, before anything else: "*node* → **module** (the glossary keeps *node* out of prose)." Then carry on with the task using the right word.

**Why:** a glossary is only worth keeping if the words hold in conversation too. A wrong word said to a session ends up in its comments, test names and commit messages.

**How to apply:**

- One line per word, once per session per word, and only where the meaning is clear. If the word could be right, say nothing.
- Code names are not wrong. Quoting a class that uses the old word is fine.
- Never adopt the user's word in anything written to the repo.
- No glossary yet: skip this rule until one exists.
