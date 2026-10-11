# One type per file

## Split files that hold several types

A file declares one top-level type, and the file is named for it: `PluginInstalls` lives in `PluginInstalls.cs` (or `.ts`, `.java`, `.kt`, `.py` module). When a change touches a file that declares more than one type, split it in the same change, one file per type, each named for its type, in the same folder and namespace or package.

**Why:** a type is found by its file name, and a second type tucked into another's file is not.

**How to apply:** the split covers every top-level type: classes, records, structs, enums, interfaces, type aliases and delegates. A nested type stays inside its owner. Move each type verbatim with its own imports, then build; do not tidy it in the same commit. Leave files this session did not touch alone, however many types they hold.

Where a language's convention says otherwise (a Go package, a Rust module of small private types, a Python module of related functions), follow the language and say so in this file.
